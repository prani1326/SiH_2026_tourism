import uuid
from typing import Any, Dict, List, Optional, Tuple
from app.models.firestore_models import Trip, TripDay, TripActivity, DisruptionLog, PackingItem
from app.repositories.base_repo import BaseFirestoreRepository

class TripRepository(BaseFirestoreRepository[Trip]):
    collection_name = "trips"
    model_class = Trip

    def get_user_trips(self, user_id: str, status: Optional[str] = None, limit: int = 100) -> List[Trip]:
        if not user_id:
            return []
        all_trips = self.find_many("user_id", "==", str(user_id), limit=limit)
        if status:
            return [t for t in all_trips if t.status.lower() == status.lower()]
        return all_trips

    def get_by_share_token(self, token: str) -> Optional[Trip]:
        if not token:
            return None
        return self.find_one("share_token", "==", token)

    def add_day(self, trip_id: str, day_data: Dict[str, Any]) -> Optional[TripDay]:
        trip = self.get_by_id(trip_id)
        if not trip:
            return None
        day = TripDay(trip_id=trip_id, **day_data)
        days = [d.to_dict() if isinstance(d, TripDay) else d for d in trip.days]
        days.append(day.to_dict())
        self.update(trip_id, {"days": days})
        return day

    def find_activity(self, activity_id: str) -> Tuple[Optional[Trip], Optional[Dict[str, Any]], Optional[Dict[str, Any]]]:
        """Finds (trip, day_dict, activity_dict) by activity_id across all trips in Firestore."""
        all_trips = self.get_all(limit=10000)
        for trip in all_trips:
            for day in trip.days:
                d_dict = day.to_dict() if isinstance(day, TripDay) else day
                for act in d_dict.get("activities", []):
                    a_dict = act.to_dict() if isinstance(act, TripActivity) else act
                    if str(a_dict.get("id")) == str(activity_id):
                        return trip, d_dict, a_dict
        return None, None, None

    def find_day(self, day_id: str) -> Tuple[Optional[Trip], Optional[Dict[str, Any]]]:
        all_trips = self.get_all(limit=10000)
        for trip in all_trips:
            for idx, day in enumerate(trip.days, start=1):
                d_dict = day.to_dict() if isinstance(day, TripDay) else day
                cur_id = str(d_dict.get("id", ""))
                day_num = d_dict.get("day_number", idx)
                if cur_id == str(day_id) or str(day_id) in [f"day-{trip.id}-{idx}", f"day-{trip.id}-{day_num}", f"{trip.id}-day-{idx}"]:
                    return trip, d_dict
        return None, None

    def add_activity(self, day_id: str, act_data: Dict[str, Any], trip_id: Optional[str] = None) -> Optional[TripActivity]:
        if not trip_id:
            trip, _ = self.find_day(day_id)
            if not trip:
                return None
            trip_id = trip.id
        else:
            trip = self.get_by_id(trip_id)
            if not trip:
                return None

        act_id = act_data.get("id") or str(uuid.uuid4())
        act_data_clean = dict(act_data)
        act_data_clean["id"] = act_id
        act_data_clean["trip_day_id"] = day_id
        activity = TripActivity(**act_data_clean)
        days_payload = []
        target_found = False
        for idx, d in enumerate(trip.days, start=1):
            d_dict = d.to_dict() if isinstance(d, TripDay) else d
            cur_id = str(d_dict.get("id", ""))
            day_num = d_dict.get("day_number", idx)
            if cur_id == str(day_id) or str(day_id) in [f"day-{trip.id}-{idx}", f"day-{trip.id}-{day_num}", f"{trip.id}-day-{idx}"]:
                acts = d_dict.get("activities", [])
                acts.append(activity.to_dict())
                d_dict["activities"] = acts
                target_found = True
            days_payload.append(d_dict)

        if target_found:
            self.update(trip_id, {"days": days_payload})
            return activity
        return None

    def update_activity(self, activity_id: str, updates: Dict[str, Any]) -> bool:
        trip, _, _ = self.find_activity(activity_id)
        if not trip:
            return False

        days_payload = []
        for d in trip.days:
            d_dict = d.to_dict() if isinstance(d, TripDay) else d
            updated_acts = []
            for a in d_dict.get("activities", []):
                a_dict = a.to_dict() if isinstance(a, TripActivity) else a
                if str(a_dict.get("id")) == str(activity_id):
                    a_dict.update(updates)
                updated_acts.append(a_dict)
            d_dict["activities"] = updated_acts
            days_payload.append(d_dict)

        self.update(trip.id, {"days": days_payload})
        return True

    def delete_activity(self, activity_id: str) -> bool:
        trip, _, _ = self.find_activity(activity_id)
        if not trip:
            return False

        days_payload = []
        for d in trip.days:
            d_dict = d.to_dict() if isinstance(d, TripDay) else d
            d_dict["activities"] = [
                (a.to_dict() if isinstance(a, TripActivity) else a)
                for a in d_dict.get("activities", [])
                if str((a.to_dict() if isinstance(a, TripActivity) else a).get("id")) != str(activity_id)
            ]
            days_payload.append(d_dict)

        self.update(trip.id, {"days": days_payload})
        return True

    def reorder_activities(self, day_id: str, ordered_ids: List[str]) -> bool:
        trip, _ = self.find_day(day_id)
        if not trip:
            return False

        days_payload = []
        for d in trip.days:
            d_dict = d.to_dict() if isinstance(d, TripDay) else d
            if str(d_dict.get("id")) == str(day_id):
                act_map = {
                    str((a.to_dict() if isinstance(a, TripActivity) else a).get("id")): (a.to_dict() if isinstance(a, TripActivity) else a)
                    for a in d_dict.get("activities", [])
                }
                reordered = []
                for idx, a_id in enumerate(ordered_ids, start=1):
                    if a_id in act_map:
                        item = act_map[a_id]
                        item["sequence_order"] = idx
                        reordered.append(item)
                d_dict["activities"] = reordered
            days_payload.append(d_dict)

        self.update(trip.id, {"days": days_payload})
        return True

    def add_disruption(self, trip_id: str, disruption_data: Dict[str, Any]) -> Optional[DisruptionLog]:
        trip = self.get_by_id(trip_id)
        if not trip:
            return None
        disruption = DisruptionLog(trip_id=trip_id, **disruption_data)
        disruptions = [dl.to_dict() if isinstance(dl, DisruptionLog) else dl for dl in trip.disruptions]
        disruptions.append(disruption.to_dict())
        self.update(trip_id, {"disruptions": disruptions})
        return disruption

    def update_packing_item(self, trip_id: str, item_id: str, is_packed: bool) -> bool:
        trip = self.get_by_id(trip_id)
        if not trip:
            return False
        items = []
        found = False
        for p in trip.packing_items:
            p_dict = p.to_dict() if isinstance(p, PackingItem) else p
            if str(p_dict.get("id")) == str(item_id):
                p_dict["is_packed"] = is_packed
                found = True
            items.append(p_dict)
        if found:
            self.update(trip_id, {"packing_items": items})
            return True
        return False

trip_repo = TripRepository()
