from fastapi import APIRouter
from app.schemas.trip import TrueTripCostResponse
from app.services.cost_calculator import cost_calculator

router = APIRouter()

@router.get("/{trip_id}/true-cost", response_model=TrueTripCostResponse)
def get_true_trip_cost(trip_id: str):
    """
    True Trip Cost Calculator with Firestore (Section 12).
    Calculates itemized breakdown (Flights, Hotel, Transport, Food, Visa, Insurance,
    eSIM, Taxes, Tips) and monitors for budget drift alerts.
    """
    return cost_calculator.calculate_true_cost(None, trip_id)

@router.get("/{trip_id}/drift-alerts")
def get_budget_drift_alert(trip_id: str):
    """Budget Drift Alert & Cost-Saving Suggestions from Firestore (Section 12)."""
    cost_data = cost_calculator.calculate_true_cost(None, trip_id)
    return {
        "trip_id": trip_id,
        "planned_budget": cost_data["planned_budget"],
        "total_estimated_cost": cost_data["total_estimated_cost"],
        "drift_percent": cost_data["drift_percent"],
        "drift_status": cost_data["drift_status"],
        "alert_message": cost_data["budget_drift_alert"],
        "cost_saving_suggestions": cost_data["cost_saving_suggestions"]
    }
