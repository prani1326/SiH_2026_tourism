import time
import uuid
from typing import Dict, List
from starlette.middleware.base import BaseHTTPMiddleware
from starlette.requests import Request
from starlette.responses import Response, JSONResponse
from app.core.redis import redis_client

class SecurityHeadersMiddleware(BaseHTTPMiddleware):
    """
    Enterprise Security Headers Middleware.
    Protects against XSS, clickjacking, MIME-sniffing, and enforces HTTPS transport standards.
    """
    async def dispatch(self, request: Request, call_next):
        response: Response = await call_next(request)
        response.headers["X-Content-Type-Options"] = "nosniff"
        response.headers["X-Frame-Options"] = "DENY"
        response.headers["X-XSS-Protection"] = "1; mode=block"
        response.headers["Referrer-Policy"] = "strict-origin-when-cross-origin"
        response.headers["Strict-Transport-Security"] = "max-age=31536000; includeSubDomains"
        response.headers["Permissions-Policy"] = "geolocation=(self), camera=(self), microphone=()"
        response.headers["Content-Security-Policy"] = "default-src 'self'; frame-ancestors 'none';"
        return response


class RateLimiterMiddleware(BaseHTTPMiddleware):
    """
    Enterprise Redis-backed Sliding Window Rate Limiter Middleware.
    Throttles brute-force attacks on authentication, OTP generation, and Lost Phone Mode.
    Falls back gracefully to in-memory tracking when Redis is disconnected.
    """
    def __init__(self, app):
        super().__init__(app)
        self.sensitive_routes: Dict[str, int] = {
            "/api/v1/auth/login": 20,
            "/api/v1/auth/otp/send": 10,
            "/api/v1/lost-phone/recover": 10,
            "/api/v1/trips/ai-plan": 30,
        }
        self.default_rate_limit: int = 300

    async def dispatch(self, request: Request, call_next):
        client_ip = request.client.host if request.client else "127.0.0.1"
        path = request.url.path

        # Determine limit for path
        limit = self.default_rate_limit
        for route_prefix, route_limit in self.sensitive_routes.items():
            if path.startswith(route_prefix):
                limit = route_limit
                break

        key = f"{client_ip}:{path}" if limit < self.default_rate_limit else client_ip

        allowed, remaining, retry_after = redis_client.check_rate_limit(
            key=key,
            limit=limit,
            window_seconds=60
        )

        if not allowed:
            return JSONResponse(
                status_code=429,
                content={
                    "success": False,
                    "error": "Too Many Requests",
                    "detail": f"Rate limit exceeded. Please wait {retry_after} seconds before retrying.",
                    "retry_after_seconds": retry_after
                },
                headers={
                    "Retry-After": str(retry_after),
                    "X-RateLimit-Limit": str(limit),
                    "X-RateLimit-Remaining": "0"
                }
            )

        response: Response = await call_next(request)
        response.headers["X-RateLimit-Limit"] = str(limit)
        response.headers["X-RateLimit-Remaining"] = str(remaining)
        return response


class RequestTracingMiddleware(BaseHTTPMiddleware):
    """
    Request Tracing and Audit Logger Middleware.
    Injects unique X-Request-ID and measures server processing latency.
    """
    async def dispatch(self, request: Request, call_next):
        request_id = request.headers.get("X-Request-ID") or str(uuid.uuid4())
        request.state.request_id = request_id
        start_time = time.time()

        response: Response = await call_next(request)
        
        process_time = (time.time() - start_time) * 1000.0  # ms
        response.headers["X-Request-ID"] = request_id
        response.headers["X-Process-Time-Ms"] = f"{process_time:.2f}"
        return response
