import json
import uuid
from datetime import datetime, timezone
from typing import Any, Dict, List, Optional, Union

def _now_iso() -> str:
    return datetime.now(timezone.utc).isoformat()

def _ensure_list(val: Any) -> List[Any]:
    if isinstance(val, list):
        return val
    if isinstance(val, str):
        try:
            parsed = json.loads(val)
            if isinstance(parsed, list):
                return parsed
        except Exception:
            pass
    return []

def _ensure_dict(val: Any) -> Dict[str, Any]:
    if isinstance(val, dict):
        return val
    if isinstance(val, str):
        try:
            parsed = json.loads(val)
            if isinstance(parsed, dict):
                return parsed
        except Exception:
            pass
    return {}


class FirestoreEntity:
    """Base Firestore entity model providing dictionary mapping and attribute access."""
    def __init__(self, **kwargs):
        self.id = kwargs.get("id") or str(uuid.uuid4())
        self.created_at = kwargs.get("created_at") or _now_iso()
        self.updated_at = kwargs.get("updated_at") or _now_iso()
        for k, v in kwargs.items():
            try:
                setattr(self, k, v)
            except AttributeError:
                pass

    def to_dict(self) -> Dict[str, Any]:
        res = {}
        for k, v in self.__dict__.items():
            if k.startswith("_"):
                continue
            if isinstance(v, FirestoreEntity):
                res[k] = v.to_dict()
            elif isinstance(v, list):
                res[k] = [item.to_dict() if isinstance(item, FirestoreEntity) else item for item in v]
            else:
                res[k] = v
        return res

    @classmethod
    def from_dict(cls, data: Dict[str, Any], doc_id: Optional[str] = None):
        if not data:
            return None
        payload = data.copy()
        if doc_id:
            payload["id"] = doc_id
        return cls(**payload)


class UserAddress(FirestoreEntity):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.address_line1 = kwargs.get("address_line1", "")
        self.address_line2 = kwargs.get("address_line2", "")
        self.city = kwargs.get("city", "")
        self.state = kwargs.get("state", "")
        self.country = kwargs.get("country", "India")
        self.postal_code = kwargs.get("postal_code", "")

class KycData(FirestoreEntity):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.status = kwargs.get("status", "NOT_STARTED")  # NOT_STARTED, SKIPPED, PENDING, VERIFIED, FAILED
        self.document_type = kwargs.get("document_type", "Passport")
        self.document_number = kwargs.get("document_number", "")
        self.verification_provider = kwargs.get("verification_provider", "Government Identity Services")
        self.verification_reference = kwargs.get("verification_reference", "")
        self.submitted_at = kwargs.get("submitted_at")
        self.verified_at = kwargs.get("verified_at")

class UserProfile(FirestoreEntity):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.avatar_url = kwargs.get("avatar_url")
        self.bio = kwargs.get("bio")
        self.first_name = kwargs.get("first_name", "")
        self.last_name = kwargs.get("last_name", "")
        self.date_of_birth = kwargs.get("date_of_birth", "")
        self.gender = kwargs.get("gender", "Prefer not to say")
        self.nationality = kwargs.get("nationality", "Indian")
        self.language = kwargs.get("language", "English")
        self.preferred_language = kwargs.get("preferred_language", self.language)
        self.currency = kwargs.get("currency", "INR")
        self.walking_tolerance = kwargs.get("walking_tolerance", "Moderate")
        self.pace = kwargs.get("pace", "Moderate")


class TravelPreferences(FirestoreEntity):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.preferred_destinations = _ensure_list(kwargs.get("preferred_destinations", []))
        self.budget_range = kwargs.get("budget_range", "Moderate")
        self.travel_styles = _ensure_list(kwargs.get("travel_styles", ["Solo"]))
        self.interests = _ensure_list(kwargs.get("interests", ["Heritage", "Food"]))
        self.dietary_preferences = _ensure_list(kwargs.get("dietary_preferences", ["Vegetarian"]))


class EmergencyContact(FirestoreEntity):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.user_id = kwargs.get("user_id")
        self.contact_name = kwargs.get("contact_name", "")
        self.relationship_type = kwargs.get("relationship_type", "Family")
        self.phone_number = kwargs.get("phone_number", "")
        self.email = kwargs.get("email")
        self.is_primary = kwargs.get("is_primary", False)


class User(FirestoreEntity):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.email = kwargs.get("email", "")
        self.phone = kwargs.get("phone", "")
        self.password_hash = kwargs.get("password_hash", "")
        self.full_name = kwargs.get("full_name", "Traveler")
        self.role = kwargs.get("role", "traveler")
        self.is_verified = kwargs.get("is_verified", False)
        self.email_verified = kwargs.get("email_verified", True)
        self.phone_verified = kwargs.get("phone_verified", True)
        self.fcm_token = kwargs.get("fcm_token")
        self.device_id = kwargs.get("device_id")
        self.device_name = kwargs.get("device_name")
        self.firebase_uid = kwargs.get("firebase_uid")

        # Profile & KYC Lifecycle status
        self.profile_status = kwargs.get("profile_status", "PROFILE_INCOMPLETE")
        self.profile_completion = float(kwargs.get("profile_completion", 40.0))
        self.kyc_status = kwargs.get("kyc_status", "NOT_STARTED")  # NOT_STARTED, SKIPPED, PENDING, VERIFIED, FAILED
        self.kyc_verified = bool(kwargs.get("kyc_verified", False))
        self.onboarding_step = int(kwargs.get("onboarding_step", 1))

        # Nested address
        addr_data = kwargs.get("address")
        if isinstance(addr_data, dict):
            self.address = UserAddress(**addr_data)
        elif isinstance(addr_data, UserAddress):
            self.address = addr_data
        else:
            self.address = UserAddress()

        # Nested KYC
        kyc_data = kwargs.get("kyc")
        if isinstance(kyc_data, dict):
            self.kyc = KycData(**kyc_data)
        elif isinstance(kyc_data, KycData):
            self.kyc = kyc_data
        else:
            self.kyc = KycData(status=self.kyc_status)

        # Nested profile and preferences
        profile_data = kwargs.get("profile")
        if isinstance(profile_data, dict):
            self.profile = UserProfile(**profile_data)
        elif isinstance(profile_data, UserProfile):
            self.profile = profile_data
        else:
            self.profile = UserProfile()

        prefs_data = kwargs.get("preferences")
        if isinstance(prefs_data, dict):
            self.preferences = TravelPreferences(**prefs_data)
        elif isinstance(prefs_data, TravelPreferences):
            self.preferences = prefs_data
        else:
            self.preferences = TravelPreferences()

        # Relations
        self.emergency_contacts: List[EmergencyContact] = [
            EmergencyContact(**c) if isinstance(c, dict) else c
            for c in kwargs.get("emergency_contacts", [])
        ]
        self.trips = kwargs.get("trips", [])
        self.bookings = kwargs.get("bookings", [])
        self.support_tickets = kwargs.get("support_tickets", [])
        self.notifications = kwargs.get("notifications", [])


class PlaceAttraction(FirestoreEntity):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.destination_id = kwargs.get("destination_id")
        self.name = kwargs.get("name", "")
        self.category = kwargs.get("category", "Monument")
        self.latitude = float(kwargs.get("latitude", 0.0))
        self.longitude = float(kwargs.get("longitude", 0.0))
        self.image_url = kwargs.get("image_url", "")
        self.description = kwargs.get("description", "")
        self.rating = float(kwargs.get("rating", 4.5))
        self.price_tier = kwargs.get("price_tier", "Free")
        self.entry_fee = float(kwargs.get("entry_fee", 0.0))
        self.estimated_duration_hours = float(kwargs.get("estimated_duration_hours", 2.0))
        self.is_must_visit = kwargs.get("is_must_visit", False)
        self.is_wheelchair_accessible = kwargs.get("is_wheelchair_accessible", True)


class DayPassBundle(FirestoreEntity):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.destination_id = kwargs.get("destination_id")
        self.title = kwargs.get("title", "")
        self.description = kwargs.get("description", "")
        self.price = float(kwargs.get("price", 999.0))
        self.validity_days = int(kwargs.get("validity_days", 1))
        self.included_attractions = _ensure_list(kwargs.get("included_attractions", []))
        self.terms_and_conditions = kwargs.get("terms_and_conditions", "Valid for specified duration only.")
        self.is_active = kwargs.get("is_active", True)


class Destination(FirestoreEntity):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.name = kwargs.get("name", "")
        self.state = kwargs.get("state", "India")
        self.country = kwargs.get("country", "India")
        self.latitude = float(kwargs.get("latitude", 20.5937))
        self.longitude = float(kwargs.get("longitude", 78.9629))
        self.hero_image_url = kwargs.get("hero_image_url", "")
        self.description = kwargs.get("description", "")
        self.rating = float(kwargs.get("rating", 4.8))
        self.best_time_to_visit = kwargs.get("best_time_to_visit", "October to March")
        self.estimated_budget_tier = kwargs.get("estimated_budget_tier", "₹2,000–₹5,000/day")
        self.weather_temperature = kwargs.get("weather_temperature", "26°C")
        self.weather_condition = kwargs.get("weather_condition", "Clear Sky")
        self.safety_score = float(kwargs.get("safety_score", 9.2))
        self.is_featured = kwargs.get("is_featured", True)
        self.is_popular = kwargs.get("is_popular", True)

        self.known_for = kwargs.get("known_for", "")
        self.ideal_stay = kwargs.get("ideal_stay", "")
        self.budget_per_day = kwargs.get("budget_per_day", "")
        self.tags = _ensure_list(kwargs.get("tags", []))
        self.top_attractions = _ensure_list(kwargs.get("top_attractions", []))
        self.activities = _ensure_list(kwargs.get("activities", []))
        self.famous_food = _ensure_list(kwargs.get("famous_food", []))
        self.local_transport = _ensure_list(kwargs.get("local_transport", []))
        self.nearby_places = _ensure_list(kwargs.get("nearby_places", []))
        self.scam_alerts = _ensure_list(kwargs.get("scam_alerts", []))
        self.local_etiquette = _ensure_list(kwargs.get("local_etiquette", []))
        self.emergency_info = _ensure_dict(kwargs.get("emergency_info", {}))
        
        self.places = [
            PlaceAttraction(**p) if isinstance(p, dict) else p
            for p in kwargs.get("places", [])
        ]
        self.day_passes = [
            DayPassBundle(**b) if isinstance(b, dict) else b
            for b in kwargs.get("day_passes", [])
        ]


class TripActivity(FirestoreEntity):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.trip_day_id = kwargs.get("trip_day_id")
        self.place_id = kwargs.get("place_id")
        self.time_slot = kwargs.get("time_slot", "Morning")
        self.sequence_order = int(kwargs.get("sequence_order", 1))
        self.title = kwargs.get("title", "")
        self.description = kwargs.get("description", "")
        self.start_time = kwargs.get("start_time", "09:00 AM")
        self.end_time = kwargs.get("end_time", "11:30 AM")
        self.estimated_cost = float(kwargs.get("estimated_cost", 0.0))
        self.travel_time_minutes = int(kwargs.get("travel_time_minutes", 20))
        self.is_outdoor = kwargs.get("is_outdoor", True)
        self.is_completed = kwargs.get("is_completed", False)
        self.transport_mode = kwargs.get("transport_mode", "Taxi / Auto")
        self.notes = kwargs.get("notes", "")


class TripDay(FirestoreEntity):
    def __init__(self, **kwargs):
        trip_id = kwargs.get("trip_id", "")
        day_num = kwargs.get("day_number", 1)
        if not kwargs.get("id"):
            kwargs["id"] = f"day-{trip_id}-{day_num}" if trip_id else str(uuid.uuid4())
        super().__init__(**kwargs)
        self.trip_id = trip_id
        self.day_number = int(day_num)
        self.date = kwargs.get("date", "")
        self.weather_summary = kwargs.get("weather_summary", "Sunny, 28°C")
        self.notes = kwargs.get("notes", "")
        self.activities: List[TripActivity] = [
            TripActivity(**a) if isinstance(a, dict) else a
            for a in kwargs.get("activities", [])
        ]


class DisruptionLog(FirestoreEntity):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.trip_id = kwargs.get("trip_id")
        self.type = kwargs.get("type") or kwargs.get("disruption_type", "Weather")
        self.disruption_type = self.type
        self.severity = kwargs.get("severity", "Medium")
        self.affected_day = kwargs.get("affected_day", 1)
        self.affected_activity_id = kwargs.get("affected_activity_id")
        self.recommended_action = kwargs.get("recommended_action", "")
        self.recommended_replan = kwargs.get("recommended_replan", {})
        self.is_resolved = kwargs.get("is_resolved", False)
        self.resolved_at = kwargs.get("resolved_at")


class PackingItem(FirestoreEntity):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.trip_id = kwargs.get("trip_id")
        self.category = kwargs.get("category", "Clothing")
        self.item_name = kwargs.get("item_name", "")
        self.is_packed = kwargs.get("is_packed", False)


class Trip(FirestoreEntity):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.user_id = kwargs.get("user_id", "")
        self.destination_id = kwargs.get("destination_id", "")
        self.title = kwargs.get("title", "Trip Itinerary")
        self.start_date = kwargs.get("start_date", "")
        self.end_date = kwargs.get("end_date", "")
        self.traveler_count = int(kwargs.get("traveler_count", 1))
        self.travel_style = kwargs.get("travel_style", "Solo")
        self.total_budget = float(kwargs.get("total_budget", 40000.0))
        self.total_estimated_cost = float(kwargs.get("total_estimated_cost", 36500.0))
        self.actual_spend = float(kwargs.get("actual_spend", 0.0))
        self.budget_drift_percent = float(kwargs.get("budget_drift_percent", 0.0))
        self.status = kwargs.get("status", "planning")
        self.share_token = kwargs.get("share_token")
        self.is_offline_cached = kwargs.get("is_offline_cached", False)
        self.active_step = kwargs.get("active_step", "Overview")

        trip_id = self.id
        self.days: List[TripDay] = []
        for idx, d in enumerate(kwargs.get("days", []), start=1):
            if isinstance(d, dict):
                d_dict = dict(d)
                if not d_dict.get("trip_id"):
                    d_dict["trip_id"] = trip_id
                if not d_dict.get("day_number"):
                    d_dict["day_number"] = idx
                if not d_dict.get("id"):
                    d_dict["id"] = f"day-{trip_id}-{d_dict['day_number']}"
                self.days.append(TripDay(**d_dict))
            elif isinstance(d, TripDay):
                if not d.trip_id:
                    d.trip_id = trip_id
                self.days.append(d)

        self.disruptions: List[DisruptionLog] = [
            DisruptionLog(**dl) if isinstance(dl, dict) else dl
            for dl in kwargs.get("disruptions", [])
        ]
        self.packing_items: List[PackingItem] = [
            PackingItem(**pi) if isinstance(pi, dict) else pi
            for pi in kwargs.get("packing_items", [])
        ]
        self.bookings = kwargs.get("bookings", [])


class PaymentTransaction(FirestoreEntity):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.booking_id = kwargs.get("booking_id", "")
        self.user_id = kwargs.get("user_id", "")
        self.transaction_ref = kwargs.get("transaction_ref", "")
        self.payment_method = kwargs.get("payment_method", "UPI")
        self.amount = float(kwargs.get("amount", 0.0))
        self.currency = kwargs.get("currency", "INR")
        self.status = kwargs.get("status", "Pending")
        self.gateway_response_json = kwargs.get("gateway_response_json", "{}")
        self.invoice_number = kwargs.get("invoice_number")
        self.razorpay_order_id = kwargs.get("razorpay_order_id")
        self.razorpay_payment_id = kwargs.get("razorpay_payment_id")
        self.razorpay_signature = kwargs.get("razorpay_signature")
        self.idempotency_key = kwargs.get("idempotency_key")
        self.is_simulated = kwargs.get("is_simulated", False)


class Refund(FirestoreEntity):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.booking_id = kwargs.get("booking_id", "")
        self.transaction_id = kwargs.get("transaction_id")
        self.refund_ref = kwargs.get("refund_ref", "")
        self.amount = float(kwargs.get("amount", 0.0))
        self.currency = kwargs.get("currency", "INR")
        self.cancellation_reason = kwargs.get("cancellation_reason", "")
        self.status = kwargs.get("status", "Initiated")
        if "milestones" in kwargs:
            m = kwargs.get("milestones")
            self.milestones_json = m if isinstance(m, str) else json.dumps(m)
        else:
            self.milestones_json = kwargs.get("milestones_json", "[]")
        self.razorpay_refund_id = kwargs.get("razorpay_refund_id")
        self.is_simulated = kwargs.get("is_simulated", False)

    @property
    def milestones(self) -> List[Dict[str, Any]]:
        return _ensure_list(self.milestones_json)

    @milestones.setter
    def milestones(self, value):
        if isinstance(value, str):
            self.milestones_json = value
        elif isinstance(value, list):
            self.milestones_json = json.dumps(value)


class Booking(FirestoreEntity):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.user_id = kwargs.get("user_id", "")
        self.trip_id = kwargs.get("trip_id")
        self.item_type = kwargs.get("item_type", "Hotel")
        self.item_id = kwargs.get("item_id", "")
        self.item_title = kwargs.get("item_title", "")
        self.booking_reference = kwargs.get("booking_reference", "")
        self.check_in_date = kwargs.get("check_in_date", "")
        self.check_out_date = kwargs.get("check_out_date")
        self.guest_count = int(kwargs.get("guest_count", 1))
        self.total_amount = float(kwargs.get("total_amount", 0.0))
        self.currency = kwargs.get("currency", "INR")
        self.status = kwargs.get("status", "Requested")
        self.payment_status = kwargs.get("payment_status", "Pending")
        self.voucher_qr_data = kwargs.get("voucher_qr_data")
        self.cancellation_policy = kwargs.get("cancellation_policy", "Free cancellation up to 24h before check-in")
        self.special_requests = kwargs.get("special_requests")
        self.vendor_name = kwargs.get("vendor_name")
        self.vendor_booking_ref = kwargs.get("vendor_booking_ref")
        self.is_simulated = kwargs.get("is_simulated", False)

        self.payments: List[PaymentTransaction] = [
            PaymentTransaction(**p) if isinstance(p, dict) else p
            for p in kwargs.get("payments", [])
        ]
        self.refunds: List[Refund] = [
            Refund(**r) if isinstance(r, dict) else r
            for r in kwargs.get("refunds", [])
        ]


class SOSAlert(FirestoreEntity):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.user_id = kwargs.get("user_id", "")
        self.trip_id = kwargs.get("trip_id")
        self.latitude = float(kwargs.get("latitude", 0.0))
        self.longitude = float(kwargs.get("longitude", 0.0))
        self.address = kwargs.get("address", "")
        self.status = kwargs.get("status", "Active")
        self.notified_contacts_json = kwargs.get("notified_contacts_json", "[]")
        self.emergency_services_json = kwargs.get("emergency_services_json", "{}")
        self.live_location_active = kwargs.get("live_location_active", True)
        self.resolved_at = kwargs.get("resolved_at")
        self.resolution_notes = kwargs.get("resolution_notes")

    @property
    def notified_contacts(self):
        return _ensure_list(self.notified_contacts_json)

    @property
    def emergency_services(self):
        return _ensure_dict(self.emergency_services_json)


class LostPhoneSession(FirestoreEntity):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.user_id = kwargs.get("user_id", "")
        self.recovery_token = kwargs.get("recovery_token", "")
        self.recovery_code = kwargs.get("recovery_code", "")
        self.access_email = kwargs.get("access_email", "")
        self.is_card_frozen = kwargs.get("is_card_frozen", False)
        self.expires_at = kwargs.get("expires_at", "")
        self.last_accessed_at = kwargs.get("last_accessed_at", _now_iso())


class SeparatedGroup(FirestoreEntity):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.trip_id = kwargs.get("trip_id")
        self.group_name = kwargs.get("group_name", "")
        self.admin_user_id = kwargs.get("admin_user_id", "")
        self.rendezvous_meeting_point = kwargs.get("rendezvous_meeting_point", "Main Entrance Gate 2")
        self.geofence_lat = float(kwargs.get("geofence_lat", 0.0)) if kwargs.get("geofence_lat") else None
        self.geofence_lon = float(kwargs.get("geofence_lon", 0.0)) if kwargs.get("geofence_lon") else None
        self.geofence_radius_meters = float(kwargs.get("geofence_radius_meters", 500.0))
        self.checkin_interval_minutes = int(kwargs.get("checkin_interval_minutes", 60))
        self.is_active = kwargs.get("is_active", True)
        self.members = kwargs.get("members", [])


class GroupMember(FirestoreEntity):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.group_id = kwargs.get("group_id", "")
        self.user_id = kwargs.get("user_id", "")
        self.member_name = kwargs.get("member_name", "")
        self.phone = kwargs.get("phone")
        self.status = kwargs.get("status", "Safe")
        self.last_checkin_at = kwargs.get("last_checkin_at", _now_iso())
        self.last_latitude = float(kwargs.get("last_latitude", 0.0)) if kwargs.get("last_latitude") else None
        self.last_longitude = float(kwargs.get("last_longitude", 0.0)) if kwargs.get("last_longitude") else None


class CommunityForum(FirestoreEntity):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.title = kwargs.get("title", "")
        self.slug = kwargs.get("slug", "")
        self.description = kwargs.get("description", "")
        self.cover_image = kwargs.get("cover_image", "")
        self.category = kwargs.get("category", "Regional Travelers")
        self.member_count = int(kwargs.get("member_count", 1240))
        self.is_active = kwargs.get("is_active", True)
        self.posts = kwargs.get("posts", [])


class CommunityPost(FirestoreEntity):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.forum_id = kwargs.get("forum_id", "")
        self.user_id = kwargs.get("user_id", "")
        self.author_name = kwargs.get("author_name", "Travel Enthusiast")
        self.author_avatar = kwargs.get("author_avatar", "")
        self.title = kwargs.get("title", "")
        self.content = kwargs.get("content", "")
        self.post_type = kwargs.get("post_type", "Tip")
        self.likes_count = int(kwargs.get("likes_count", 14))
        self.comments_count = int(kwargs.get("comments_count", 3))
        self.is_verified_traveler = kwargs.get("is_verified_traveler", True)
        self.comments = kwargs.get("comments", [])


class CommunityComment(FirestoreEntity):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.post_id = kwargs.get("post_id", "")
        self.user_id = kwargs.get("user_id", "")
        self.author_name = kwargs.get("author_name", "Traveler")
        self.author_avatar = kwargs.get("author_avatar", "")
        self.content = kwargs.get("content", "")
        self.likes_count = int(kwargs.get("likes_count", 0))


class CreatorItinerary(FirestoreEntity):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.creator_id = kwargs.get("creator_id", "")
        self.creator_name = kwargs.get("creator_name", "")
        self.creator_avatar = kwargs.get("creator_avatar", "")
        self.title = kwargs.get("title", "")
        self.destination_name = kwargs.get("destination_name", "")
        self.duration_days = int(kwargs.get("duration_days", 5))
        self.total_estimated_cost = float(kwargs.get("total_estimated_cost", 35000.0))
        self.price = float(kwargs.get("price", 0.0))
        self.is_paid = kwargs.get("is_paid", False)
        self.purchases_count = int(kwargs.get("purchases_count", 89))
        self.copy_count = int(kwargs.get("copy_count", 342))
        self.rating = float(kwargs.get("rating", 4.9))
        self.itinerary_json = kwargs.get("itinerary_json", "{}")

    @property
    def itinerary_data(self):
        return _ensure_dict(self.itinerary_json)


class SupportTicket(FirestoreEntity):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.user_id = kwargs.get("user_id", "")
        self.category = kwargs.get("category", "General")
        self.subject = kwargs.get("subject", "")
        self.status = kwargs.get("status", "Open")
        self.priority = kwargs.get("priority", "Medium")
        self.booking_id = kwargs.get("booking_id")
        self.messages = kwargs.get("messages", [])


class TicketMessage(FirestoreEntity):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.ticket_id = kwargs.get("ticket_id", "")
        self.sender_type = kwargs.get("sender_type", "user")
        self.sender_name = kwargs.get("sender_name", "Traveler")
        self.message = kwargs.get("message", "")
        self.attachments_json = kwargs.get("attachments_json", "[]")

    @property
    def attachments(self):
        return _ensure_list(self.attachments_json)


class Notification(FirestoreEntity):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.user_id = kwargs.get("user_id", "")
        self.title = kwargs.get("title", "")
        self.message = kwargs.get("message", "")
        self.category = kwargs.get("category", "Booking")
        self.is_read = kwargs.get("is_read", False)
        self.deep_link = kwargs.get("deep_link")


class Review(FirestoreEntity):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.user_id = kwargs.get("user_id", "")
        self.user_name = kwargs.get("user_name", "Traveler")
        self.user_avatar = kwargs.get("user_avatar", "")
        self.target_type = kwargs.get("target_type", "destination")
        self.target_id = kwargs.get("target_id", "")
        self.rating = float(kwargs.get("rating", 5.0))
        self.comment = kwargs.get("comment", "")
        self.helpful_votes = int(kwargs.get("helpful_votes", 0))
        self.is_verified = kwargs.get("is_verified", True)


class AuditLog(FirestoreEntity):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.user_id = kwargs.get("user_id")
        self.action = kwargs.get("action", "")
        self.entity_type = kwargs.get("entity_type")
        self.entity_id = kwargs.get("entity_id")
        self.ip_address = kwargs.get("ip_address")
        self.user_agent = kwargs.get("user_agent")
        self.details = kwargs.get("details", "{}")


class IdempotencyRecord(FirestoreEntity):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.key = kwargs.get("key", "")
        self.user_id = kwargs.get("user_id")
        self.request_path = kwargs.get("request_path", "")
        self.request_hash = kwargs.get("request_hash")
        self.response_code = kwargs.get("response_code")
        self.response_body = kwargs.get("response_body")
        self.status = kwargs.get("status", "PROCESSING")
        self.expires_at = kwargs.get("expires_at", "")


class AIGenerationLog(FirestoreEntity):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.user_id = kwargs.get("user_id")
        self.trip_id = kwargs.get("trip_id")
        self.destination = kwargs.get("destination", "")
        self.model_name = kwargs.get("model_name", "gemini-2.0-flash")
        self.prompt_tokens = int(kwargs.get("prompt_tokens", 0))
        self.completion_tokens = int(kwargs.get("completion_tokens", 0))
        self.latency_ms = int(kwargs.get("latency_ms", 0))
        self.status = kwargs.get("status", "SUCCESS")
        self.raw_prompt = kwargs.get("raw_prompt")
        self.raw_response = kwargs.get("raw_response")
        self.validation_notes = kwargs.get("validation_notes")
        self.feedback = kwargs.get("feedback", [])


class AIFeedback(FirestoreEntity):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.log_id = kwargs.get("log_id", "")
        self.user_id = kwargs.get("user_id", "")
        self.rating = int(kwargs.get("rating", 5))
        self.feedback_text = kwargs.get("feedback_text")
        self.customizations_count = int(kwargs.get("customizations_count", 0))
