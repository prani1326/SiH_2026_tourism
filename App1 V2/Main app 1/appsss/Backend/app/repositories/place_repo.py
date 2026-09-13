from typing import Any, Dict, List, Optional
from app.models.firestore_models import PlaceAttraction
from app.repositories.base_repo import BaseFirestoreRepository
from app.repositories.destination_repo import destination_repo

class PlaceRepository(BaseFirestoreRepository[PlaceAttraction]):
    collection_name = "places"
    model_class = PlaceAttraction

    def get_by_id(self, doc_id: str) -> Optional[PlaceAttraction]:
        # First try direct places collection
        place = super().get_by_id(doc_id)
        if place:
            return place

        # If not found directly, check across destinations embedded places
        all_dests = destination_repo.get_all(limit=100)
        for dest in all_dests:
            for p in getattr(dest, "places", []):
                p_dict = p if isinstance(p, dict) else (p.to_dict() if hasattr(p, "to_dict") else vars(p))
                if str(p_dict.get("id")) == str(doc_id):
                    return PlaceAttraction(**p_dict)
        return None

    def search_places(
        self,
        destination_id: Optional[str] = None,
        query: Optional[str] = None,
        category: Optional[str] = None,
        min_rating: Optional[float] = None,
        limit: int = 50,
        offset: int = 0
    ) -> List[PlaceAttraction]:
        # Collect from direct places collection
        results = self.get_all(limit=200)

        # Also collect from destinations embedded places
        if destination_id:
            dest = destination_repo.get_by_id(destination_id)
            dests = [dest] if dest else []
        else:
            dests = destination_repo.get_all(limit=100)

        for dest in dests:
            for p in getattr(dest, "places", []):
                p_dict = p if isinstance(p, dict) else (p.to_dict() if hasattr(p, "to_dict") else vars(p))
                # Avoid duplicate id
                if not any(str(r.id) == str(p_dict.get("id")) for r in results):
                    results.append(PlaceAttraction(**p_dict))

        # Filter
        filtered = results
        if destination_id:
            filtered = [p for p in filtered if str(getattr(p, "destination_id", "")) == str(destination_id)]
        if category:
            cat_lower = category.lower().strip()
            filtered = [
                p for p in filtered 
                if cat_lower in (getattr(p, "category", "") or "").lower() or 
                   (getattr(p, "subcategory", None) and cat_lower in p.subcategory.lower())
            ]
        if min_rating:
            filtered = [p for p in filtered if getattr(p, "rating", 0) >= min_rating]
        if query:
            q = query.lower().strip()
            filtered = [
                p for p in filtered
                if q in p.name.lower() or q in (getattr(p, "description", "") or "").lower() or q in (getattr(p, "category", "") or "").lower()
            ]

        filtered.sort(key=lambda p: getattr(p, "rating", 0), reverse=True)
        return filtered[offset : offset + limit]

place_repo = PlaceRepository()
