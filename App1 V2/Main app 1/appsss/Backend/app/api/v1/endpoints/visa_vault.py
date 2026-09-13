from fastapi import APIRouter
from pydantic import BaseModel
from typing import List
from datetime import datetime

router = APIRouter()

class VisaDocument(BaseModel):
    id: str
    document_type: str
    country: str
    issue_date: datetime
    expiry_date: datetime
    status: str
    document_url: str

@router.get("/", response_model=List[VisaDocument])
async def get_visa_documents():
    return [
        VisaDocument(
            id="doc_123",
            document_type="Tourist e-Visa",
            country="India",
            issue_date=datetime(2025, 1, 1),
            expiry_date=datetime(2026, 1, 1),
            status="Active",
            document_url="https://s3.mock/vault/visa_123.pdf"
        )
    ]
