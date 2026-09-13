from fastapi import APIRouter
from pydantic import BaseModel
from typing import List

router = APIRouter()

class SimPlan(BaseModel):
    id: str
    provider: str
    data_allowance: str
    validity_days: int
    price_inr: float
    is_esim: bool

@router.get("/plans", response_model=List[SimPlan])
async def get_sim_plans():
    return [
        SimPlan(id="sim_1", provider="Jio", data_allowance="2GB/day", validity_days=28, price_inr=349, is_esim=True),
        SimPlan(id="sim_2", provider="Airtel", data_allowance="1.5GB/day", validity_days=28, price_inr=299, is_esim=True)
    ]

@router.post("/purchase/{plan_id}")
async def purchase_sim_plan(plan_id: str):
    return {"status": "success", "message": f"Successfully purchased plan {plan_id}", "activation_code": "ACT-9921-ABCD"}
