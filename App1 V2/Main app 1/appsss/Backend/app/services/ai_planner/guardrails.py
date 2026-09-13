import re
from typing import Tuple

INJECTION_PATTERNS = [
    r"ignore\s+(previous|above|all)\s+instructions",
    r"system\s+prompt",
    r"as\s+an\s+unfiltered\s+ai",
    r"reveal\s+secret",
    r"drop\s+table",
    r"<script.*?>.*?</script>",
]

def sanitize_and_guard_prompt(text: str, max_length: int = 500) -> Tuple[str, bool]:
    """
    Sanitizes user prompt inputs:
    - Strips prompt injection vectors
    - Limits character length
    - Flags security risks
    """
    if not text:
        return "", False

    cleaned = text[:max_length].strip()
    flagged = False

    for pattern in INJECTION_PATTERNS:
        if re.search(pattern, cleaned, re.IGNORECASE):
            flagged = True
            cleaned = re.sub(pattern, "[redacted]", cleaned, flags=re.IGNORECASE)

    # Redact potential credit card or Aadhaar numbers (12-16 digits)
    cleaned = re.sub(r"\b\d{4}[ -]?\d{4}[ -]?\d{4}[ -]?\d{0,4}\b", "[REDACTED_NUM]", cleaned)

    return cleaned, flagged
