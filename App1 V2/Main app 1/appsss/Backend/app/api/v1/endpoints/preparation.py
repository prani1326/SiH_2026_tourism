from fastapi import APIRouter
from app.repositories.destination_repo import destination_repo
from app.services.cultural_engine import cultural_engine

router = APIRouter()

@router.get("/{destination_name}")
def get_preparation_checklist(destination_name: str):
    """
    Preparation Engine with Firestore (Section 26).
    Generates customized weather packing list, cultural dress requirements,
    medical travel kit, and international electrical checklists.
    """
    dest = destination_repo.get_by_id(destination_name)
    if not dest:
        results = destination_repo.search(query=destination_name, limit=1)
        dest = results[0] if results else None

    dest_title = dest.name if dest else destination_name.title()
    weather_desc = getattr(dest, "weather_condition", "Sunny") if dest else "Sunny"

    return cultural_engine.generate_preparation_checklist(
        destination_name=dest_title,
        weather_summary=weather_desc
    )
