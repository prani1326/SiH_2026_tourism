from typing import Any, Dict, List, Optional
from app.models.firestore_models import Destination, PlaceAttraction, DayPassBundle
from app.repositories.base_repo import BaseFirestoreRepository

class DestinationRepository(BaseFirestoreRepository[Destination]):
    collection_name = "destinations"
    model_class = Destination

    def search(
        self,
        query: Optional[str] = None,
        state: Optional[str] = None,
        tag: Optional[str] = None,
        is_featured: Optional[bool] = None,
        limit: int = 50,
        offset: int = 0
    ) -> List[Destination]:
        all_dests = self.list_all(limit=500, offset=0)
        filtered = all_dests

        if state:
            filtered = [d for d in filtered if d.state and d.state.lower() == state.lower()]
        if is_featured is not None:
            filtered = [d for d in filtered if d.is_featured == is_featured]
        if tag:
            filtered = [d for d in filtered if tag.lower() in [t.lower() for t in d.tags]]
        if query:
            q = query.lower().strip()
            filtered = [
                d for d in filtered
                if q in d.name.lower() or q in (d.state or "").lower() or q in (d.description or "").lower()
            ]

        # Sort by rating descending
        filtered.sort(key=lambda d: d.rating, reverse=True)
        return filtered[offset : offset + limit]

    def get_places(
        self,
        destination_id: Optional[str] = None,
        category: Optional[str] = None,
        dietary_filter: Optional[str] = None
    ) -> List[PlaceAttraction]:
        from app.repositories.place_repo import place_repo
        return place_repo.search_places(
            destination_id=destination_id,
            category=category,
            limit=100
        )

    def get_place_by_id(self, place_id: str) -> Optional[PlaceAttraction]:
        from app.repositories.place_repo import place_repo
        return place_repo.get_by_id(place_id)

    def get_place(self, destination_id: str, place_id: str) -> Optional[PlaceAttraction]:
        dest = self.get_by_id(destination_id)
        if not dest:
            return None
        for p in dest.places:
            if str(p.id) == str(place_id):
                return p
        return None

    def add_place(self, destination_id: str, place_data: Dict[str, Any]) -> Optional[PlaceAttraction]:
        dest = self.get_by_id(destination_id)
        if not dest:
            return None
        place = PlaceAttraction(destination_id=destination_id, **place_data)
        places = [p.to_dict() if isinstance(p, PlaceAttraction) else p for p in dest.places]
        places.append(place.to_dict())
        self.update(destination_id, {"places": places})
        return place

    def add_day_pass(self, destination_id: str, pass_data: Dict[str, Any]) -> Optional[DayPassBundle]:
        dest = self.get_by_id(destination_id)
        if not dest:
            return None
        bundle = DayPassBundle(destination_id=destination_id, **pass_data)
        passes = [b.to_dict() if isinstance(b, DayPassBundle) else b for b in dest.day_passes]
        passes.append(bundle.to_dict())
        self.update(destination_id, {"day_passes": passes})
        return bundle

destination_repo = DestinationRepository()
