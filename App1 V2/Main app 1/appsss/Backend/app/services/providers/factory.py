from typing import Optional
from app.services.providers.base import BookingProviderBase
from app.services.providers.hotel_provider import VerifiedHotelProviderAdapter
from app.services.providers.activity_provider import VerifiedActivityProviderAdapter
from app.services.providers.transport_provider import VerifiedTransportProviderAdapter

_hotel_provider = VerifiedHotelProviderAdapter()
_activity_provider = VerifiedActivityProviderAdapter()
_transport_provider = VerifiedTransportProviderAdapter()

def get_booking_provider(item_type: str) -> BookingProviderBase:
    """
    Factory resolving provider adapter by booking item type.
    """
    t = (item_type or "").strip().lower()
    if "hotel" in t or "stay" in t or "resort" in t:
        return _hotel_provider
    elif "transport" in t or "flight" in t or "cab" in t:
        return _transport_provider
    else:
        # Default to activities/experiences/passes
        return _activity_provider
