import logging
import smtplib
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart
from typing import Dict, Any, Optional, List
from app.core.config import settings
from app.core.redis import redis_client

logger = logging.getLogger("tourist_app.communications")

class CommunicationService:
    """
    Transactional communications engine:
    - Email delivery via SMTP / SendGrid
    - SMS alerts via Twilio / SMS Gateway
    - Background task queueing via Redis
    """

    def send_email(self, to_email: str, subject: str, body_html: str) -> bool:
        """Sends an HTML transactional email."""
        if not to_email:
            return False

        if settings.SMTP_HOST and settings.SMTP_USER and settings.SMTP_PASSWORD:
            try:
                msg = MIMEMultipart("alternative")
                msg["Subject"] = subject
                msg["From"] = settings.SMTP_FROM_EMAIL
                msg["To"] = to_email
                msg.attach(MIMEText(body_html, "html"))

                with smtplib.SMTP(settings.SMTP_HOST, settings.SMTP_PORT) as server:
                    server.starttls()
                    server.login(settings.SMTP_USER, settings.SMTP_PASSWORD)
                    server.sendmail(settings.SMTP_FROM_EMAIL, to_email, msg.as_string())
                logger.info(f"Email sent successfully to {to_email}")
                return True
            except Exception as e:
                logger.error(f"Failed to send email to {to_email}: {e}")
                return False

        logger.info(f"[SIMULATED EMAIL] To: {to_email} | Subject: '{subject}'")
        return True

    def send_sms(self, to_phone: str, message_text: str) -> bool:
        """Sends an SMS alert to traveler or emergency contact."""
        if not to_phone:
            return False

        if settings.TWILIO_ACCOUNT_SID and settings.TWILIO_AUTH_TOKEN and settings.TWILIO_FROM_PHONE:
            try:
                import httpx
                resp = httpx.post(
                    f"https://api.twilio.com/2010-04-01/Accounts/{settings.TWILIO_ACCOUNT_SID}/Messages.json",
                    auth=(settings.TWILIO_ACCOUNT_SID, settings.TWILIO_AUTH_TOKEN),
                    data={
                        "From": settings.TWILIO_FROM_PHONE,
                        "To": to_phone,
                        "Body": message_text
                    }
                )
                if resp.status_code in [200, 201]:
                    logger.info(f"SMS dispatched successfully to {to_phone}")
                    return True
                logger.error(f"Twilio SMS failed with status {resp.status_code}: {resp.text}")
            except Exception as e:
                logger.error(f"Failed to send SMS to {to_phone}: {e}")
                return False

        logger.info(f"[SIMULATED SMS] To: {to_phone} | Message: '{message_text}'")
        return True

    def enqueue_sos_alert(self, alert_id: str, contacts: List[Dict[str, str]], location: str, live_url: str):
        """Dispatches high-priority emergency notifications to queue."""
        redis_client.enqueue_task("emergency_alerts", {
            "alert_id": alert_id,
            "contacts": contacts,
            "location": location,
            "live_url": live_url
        })

communication_service = CommunicationService()
