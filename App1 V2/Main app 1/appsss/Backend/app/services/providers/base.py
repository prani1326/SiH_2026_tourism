from abc import ABC, abstractmethod
from typing import Dict, Any, Optional

class BookingProviderBase(ABC):
    """
    Abstract interface for enterprise external booking providers.
    Decouples core platform from specific suppliers (Hotels, Activities, Transport, Passes).
    """

    @property
    @abstractmethod
    def provider_name(self) -> str:
        pass

    @abstractmethod
    def check_availability(
        self,
        item_id: str,
        check_in_date: str,
        check_out_date: Optional[str] = None,
        guest_count: int = 1
    ) -> Dict[str, Any]:
        """Verify real-time availability and dynamic pricing."""
        pass

    @abstractmethod
    def create_reservation(
        self,
        item_id: str,
        booking_reference: str,
        guest_name: str,
        guest_contact: str,
        check_in_date: str,
        check_out_date: Optional[str] = None,
        guest_count: int = 1,
        total_amount: float = 0.0,
        special_requests: Optional[str] = None
    ) -> Dict[str, Any]:
        """Dispatch reservation to provider inventory system."""
        pass

    @abstractmethod
    def cancel_reservation(
        self,
        vendor_booking_ref: str,
        cancellation_reason: str
    ) -> Dict[str, Any]:
        """Notify vendor of cancellation and request release of inventory."""
        pass
