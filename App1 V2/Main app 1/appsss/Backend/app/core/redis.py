import json
import logging
import time
from typing import Any, Dict, Optional, Tuple

try:
    import redis
except ImportError:
    redis = None

from app.core.config import settings

logger = logging.getLogger("tourist_app.redis")

class EnterpriseRedisManager:
    """
    Enterprise Redis client providing:
    1. Durable token revocation blacklist
    2. User session revocation tracking
    3. Sliding-window rate limiting
    4. Fast OTP storage & validation
    5. Cache operations & Background task queueing
    Includes resilient in-memory fallback for isolated unit testing.
    """

    def __init__(self):
        self._client: Optional[Any] = None
        self._is_connected: bool = False
        self._memory_blacklist: Dict[str, float] = {}  # jti -> expiry timestamp
        self._memory_user_revocations: Dict[str, float] = {}  # user_id -> cutoff timestamp
        self._memory_otps: Dict[str, Tuple[str, float]] = {}  # key -> (otp, expiry)
        self._memory_rate_limits: Dict[str, list] = {}  # key -> list of request timestamps
        self._memory_cache: Dict[str, Tuple[str, float]] = {}  # key -> (value, expiry)
        self._memory_queues: Dict[str, list] = {}  # queue_name -> list of tasks
        self._init_connection()

    def _init_connection(self):
        if not settings.REDIS_ENABLED or redis is None:
            logger.info("Redis is disabled or library not available. Using fallback in-memory store.")
            return

        try:
            self._client = redis.Redis.from_url(
                settings.REDIS_URL,
                decode_responses=True,
                socket_connect_timeout=settings.REDIS_CONNECT_TIMEOUT,
                socket_timeout=settings.REDIS_CONNECT_TIMEOUT,
                retry_on_timeout=True
            )
            self._client.ping()
            self._is_connected = True
            logger.info(f"Connected to Redis at {settings.REDIS_URL}")
        except Exception as exc:
            self._is_connected = False
            self._client = None
            logger.warning(f"Unable to connect to Redis ({exc}). Operating with in-memory fallback.")

    @property
    def is_connected(self) -> bool:
        if self._client is not None:
            try:
                self._client.ping()
                return True
            except Exception:
                self._is_connected = False
                return False
        return False

    # --------------------------------------------------------------------------
    # 1. JWT Token Revocation Blacklist
    # --------------------------------------------------------------------------
    def revoke_token(self, jti: str, ttl_seconds: int = 86400) -> bool:
        """Add JWT JTI to revocation blacklist with TTL."""
        if not jti:
            return False

        if self.is_connected and self._client:
            try:
                self._client.setex(f"blacklist:jti:{jti}", max(1, ttl_seconds), "revoked")
                return True
            except Exception as e:
                logger.error(f"Redis revoke_token error: {e}")

        # Fallback
        self._memory_blacklist[jti] = time.time() + ttl_seconds
        return True

    def is_token_revoked(self, jti: str) -> bool:
        """Check if JWT JTI has been revoked."""
        if not jti:
            return True

        if self.is_connected and self._client:
            try:
                return bool(self._client.exists(f"blacklist:jti:{jti}"))
            except Exception as e:
                logger.error(f"Redis is_token_revoked error: {e}")

        # Fallback check
        expiry = self._memory_blacklist.get(jti)
        if expiry is not None:
            if time.time() <= expiry:
                return True
            del self._memory_blacklist[jti]
        return False

    # --------------------------------------------------------------------------
    # 2. User-Level Global Revocation (Logout All Devices / Lost Phone)
    # --------------------------------------------------------------------------
    def invalidate_user_sessions(self, user_id: str) -> bool:
        """Mark all tokens issued prior to this timestamp as invalid."""
        now = time.time()
        if self.is_connected and self._client:
            try:
                # Store cutoff timestamp with 30 days retention
                self._client.setex(f"user:revocation:{user_id}", 86400 * 30, str(now))
                return True
            except Exception as e:
                logger.error(f"Redis invalidate_user_sessions error: {e}")

        self._memory_user_revocations[user_id] = now
        return True

    def is_user_session_revoked(self, user_id: str, issued_at: float) -> bool:
        """Returns True if token was issued prior to user's revocation timestamp."""
        cutoff: Optional[float] = None
        if self.is_connected and self._client:
            try:
                val = self._client.get(f"user:revocation:{user_id}")
                if val:
                    cutoff = float(val)
            except Exception as e:
                logger.error(f"Redis is_user_session_revoked error: {e}")

        if cutoff is None:
            cutoff = self._memory_user_revocations.get(user_id)

        if cutoff is not None:
            return issued_at < cutoff
        return False

    # --------------------------------------------------------------------------
    # 3. Sliding-Window Rate Limiter
    # --------------------------------------------------------------------------
    def check_rate_limit(
        self,
        key: str,
        limit: int = 120,
        window_seconds: int = 60
    ) -> Tuple[bool, int, int]:
        """
        Sliding-window rate limiter.
        Returns: (allowed: bool, remaining_requests: int, retry_after_seconds: int)
        """
        now = time.time()
        window_start = now - window_seconds
        redis_key = f"ratelimit:{key}"

        if self.is_connected and self._client:
            try:
                pipe = self._client.pipeline()
                pipe.zremrangebyscore(redis_key, 0, window_start)
                pipe.zcard(redis_key)
                pipe.zadd(redis_key, {str(now): now})
                pipe.expire(redis_key, window_seconds + 5)
                results = pipe.execute()

                current_count = results[1]
                if current_count >= limit:
                    # Over limit
                    retry_after = int(window_seconds - (now - window_start))
                    return False, 0, max(1, retry_after)
                
                remaining = max(0, limit - current_count - 1)
                return True, remaining, 0
            except Exception as e:
                logger.error(f"Redis check_rate_limit error: {e}")

        # In-memory sliding window
        history = self._memory_rate_limits.setdefault(key, [])
        # Evict old timestamps
        history = [ts for ts in history if ts > window_start]
        self._memory_rate_limits[key] = history

        if len(history) >= limit:
            retry_after = int(window_seconds - (now - history[0])) if history else window_seconds
            return False, 0, max(1, retry_after)

        history.append(now)
        remaining = limit - len(history)
        return True, remaining, 0

    # --------------------------------------------------------------------------
    # 4. OTP Store & Verification
    # --------------------------------------------------------------------------
    def store_otp(self, identifier: str, otp: str, ttl_seconds: int = 300) -> bool:
        """Store OTP code for an email or phone number with TTL (default 5 min)."""
        key = f"otp:{identifier.strip().lower()}"
        if self.is_connected and self._client:
            try:
                self._client.setex(key, ttl_seconds, otp)
                return True
            except Exception as e:
                logger.error(f"Redis store_otp error: {e}")

        self._memory_otps[key] = (otp, time.time() + ttl_seconds)
        return True

    def verify_otp(self, identifier: str, otp: str, consume: bool = True) -> bool:
        """Verify OTP code. If consume=True, deletes OTP on success to prevent reuse."""
        key = f"otp:{identifier.strip().lower()}"
        if self.is_connected and self._client:
            try:
                stored = self._client.get(key)
                if stored and stored == otp:
                    if consume:
                        self._client.delete(key)
                    return True
                return False
            except Exception as e:
                logger.error(f"Redis verify_otp error: {e}")

        entry = self._memory_otps.get(key)
        if entry:
            val, expiry = entry
            if time.time() <= expiry and val == otp:
                if consume:
                    del self._memory_otps[key]
                return True
            if time.time() > expiry:
                del self._memory_otps[key]
        return False

    # --------------------------------------------------------------------------
    # 5. Generic Cache
    # --------------------------------------------------------------------------
    def get_cache(self, key: str) -> Optional[str]:
        if self.is_connected and self._client:
            try:
                return self._client.get(f"cache:{key}")
            except Exception as e:
                logger.error(f"Redis get_cache error: {e}")

        entry = self._memory_cache.get(key)
        if entry:
            val, expiry = entry
            if time.time() <= expiry:
                return val
            del self._memory_cache[key]
        return None

    def set_cache(self, key: str, value: str, ttl_seconds: int = 3600) -> bool:
        if self.is_connected and self._client:
            try:
                self._client.setex(f"cache:{key}", ttl_seconds, value)
                return True
            except Exception as e:
                logger.error(f"Redis set_cache error: {e}")

        self._memory_cache[key] = (value, time.time() + ttl_seconds)
        return True

    def delete_cache(self, key: str) -> bool:
        if self.is_connected and self._client:
            try:
                self._client.delete(f"cache:{key}")
                return True
            except Exception as e:
                logger.error(f"Redis delete_cache error: {e}")

        if key in self._memory_cache:
            del self._memory_cache[key]
        return True

    # --------------------------------------------------------------------------
    # 6. Background Task Queue
    # --------------------------------------------------------------------------
    def enqueue_task(self, queue_name: str, task: Dict[str, Any]) -> bool:
        payload = json.dumps(task)
        if self.is_connected and self._client:
            try:
                self._client.rpush(f"queue:{queue_name}", payload)
                return True
            except Exception as e:
                logger.error(f"Redis enqueue_task error: {e}")

        self._memory_queues.setdefault(queue_name, []).append(payload)
        return True

    def dequeue_task(self, queue_name: str, timeout: int = 1) -> Optional[Dict[str, Any]]:
        if self.is_connected and self._client:
            try:
                result = self._client.blpop(f"queue:{queue_name}", timeout=timeout)
                if result:
                    _, payload = result
                    return json.loads(payload)
                return None
            except Exception as e:
                logger.error(f"Redis dequeue_task error: {e}")

        q = self._memory_queues.get(queue_name, [])
        if q:
            return json.loads(q.pop(0))
        return None

# Global Redis manager singleton
redis_client = EnterpriseRedisManager()
