import json
import logging
import signal
import sys
import time
from app.core.config import settings
from app.core.redis import redis_client
from app.core.firebase import firebase_service
from app.services.external.communication_service import communication_service

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s"
)
logger = logging.getLogger("tourist_app.worker")

class BackgroundWorker:
    """
    Enterprise Background Task Worker.
    Pulls asynchronous jobs from Redis queues:
    - emergency_alerts (SOS notification dispatch to emergency contacts & police)
    - push_notifications (FCM mobile pushes)
    - payment_reconciliation (Asynchronous ledger & webhook fallback)
    """

    def __init__(self):
        self._running = True
        signal.signal(signal.SIGINT, self._handle_shutdown)
        signal.signal(signal.SIGTERM, self._handle_shutdown)

    def _handle_shutdown(self, signum, frame):
        logger.info("Shutdown signal received. Finishing active tasks...")
        self._running = False

    def process_emergency_alerts(self):
        task = redis_client.dequeue_task("emergency_alerts", timeout_seconds=1)
        if not task:
            return

        alert_id = task.get("alert_id")
        contacts = task.get("contacts", [])
        location = task.get("location", "Coordinates unavailable")
        live_url = task.get("live_url", "")

        logger.warning(f"[CRITICAL SOS WORKER] Dispatching SOS #{alert_id} at {location}")
        for c in contacts:
            phone = c.get("phone")
            name = c.get("name")
            if phone:
                msg = f"EMERGENCY SOS: {name}, your traveler contact has triggered an emergency alert near {location}. Track live: {live_url}"
                communication_service.send_sms(phone, msg)

    def process_push_notifications(self):
        task = redis_client.dequeue_task("push_notifications", timeout_seconds=1)
        if not task:
            return

        token = task.get("fcm_token")
        title = task.get("title", "Tourist Update")
        body = task.get("body", "")
        data = task.get("data", {})

        if token:
            firebase_service.send_push_notification(token, title, body, data)

    def start(self):
        logger.info(f"Background worker started on environment '{settings.ENVIRONMENT}'. Listening on Redis queues...")
        while self._running:
            try:
                self.process_emergency_alerts()
                self.process_push_notifications()
                time.sleep(0.5)
            except Exception as e:
                logger.error(f"Worker task processing exception: {e}")
                time.sleep(1.0)
        logger.info("Background worker stopped gracefully.")

if __name__ == "__main__":
    worker = BackgroundWorker()
    worker.start()
