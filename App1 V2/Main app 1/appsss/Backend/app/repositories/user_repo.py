from typing import Any, Dict, List, Optional
from app.models.firestore_models import User, EmergencyContact
from app.repositories.base_repo import BaseFirestoreRepository

class UserRepository(BaseFirestoreRepository[User]):
    collection_name = "users"
    model_class = User

    def get_by_email(self, email: str) -> Optional[User]:
        if not email:
            return None
        return self.find_one("email", "==", email.lower().strip())

    def get_by_phone(self, phone: str) -> Optional[User]:
        if not phone:
            return None
        return self.find_one("phone", "==", phone.strip())

    def get_by_firebase_uid(self, uid: str) -> Optional[User]:
        if not uid:
            return None
        return self.find_one("firebase_uid", "==", uid)

    def update_profile(self, user_id: str, profile_data: Dict[str, Any]) -> Optional[User]:
        user = self.get_by_id(user_id)
        if not user:
            return None
        curr_profile = user.profile.to_dict()
        curr_profile.update(profile_data)
        return self.update(user_id, {"profile": curr_profile})

    def update_preferences(self, user_id: str, prefs_data: Dict[str, Any]) -> Optional[User]:
        user = self.get_by_id(user_id)
        if not user:
            return None
        curr_prefs = user.preferences.to_dict()
        curr_prefs.update(prefs_data)
        return self.update(user_id, {"preferences": curr_prefs})

    def add_emergency_contact(self, user_id: str, contact_data: Dict[str, Any]) -> Optional[EmergencyContact]:
        user = self.get_by_id(user_id)
        if not user:
            return None
        contact = EmergencyContact(user_id=user_id, **contact_data)
        contacts = [c.to_dict() if isinstance(c, EmergencyContact) else c for c in user.emergency_contacts]
        contacts.append(contact.to_dict())
        self.update(user_id, {"emergency_contacts": contacts})
        return contact

    def list_emergency_contacts(self, user_id: str) -> List[EmergencyContact]:
        user = self.get_by_id(user_id)
        if not user:
            return []
        return user.emergency_contacts

    def delete_emergency_contact(self, user_id: str, contact_id: str) -> bool:
        user = self.get_by_id(user_id)
        if not user:
            return False
        updated = [c for c in user.emergency_contacts if str(c.id) != str(contact_id)]
        self.update(user_id, {"emergency_contacts": [c.to_dict() for c in updated]})
        return True

user_repo = UserRepository()
