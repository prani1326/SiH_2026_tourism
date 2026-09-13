from fastapi import APIRouter
from pydantic import BaseModel
from typing import List

router = APIRouter()

class InsurancePolicy(BaseModel):
    id: str
    provider: str
    coverage_details: str
    premium_inr: float
    coverage_amount_inr: float

@router.get("/policies", response_model=List[InsurancePolicy])
async def get_insurance_policies():
    return [
        InsurancePolicy(id="ins_1", provider="Bajaj Allianz", coverage_details="Medical, Trip Cancellation, Baggage Delay", premium_inr=599, coverage_amount_inr=500000),
        InsurancePolicy(id="ins_2", provider="HDFC ERGO", coverage_details="Medical, Accident, Loss of Passport", premium_inr=799, coverage_amount_inr=1000000)
    ]

@router.post("/purchase/{policy_id}")
async def purchase_policy(policy_id: str):
    return {"status": "success", "message": f"Successfully purchased policy {policy_id}", "policy_number": "POL-IND-8842"}
