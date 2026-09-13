import pytest
from fastapi.testclient import TestClient
from app.main import app
from app.models.firestore_models import User, UserProfile, UserAddress, KycData
from app.repositories.user_repo import user_repo
from app.services.profile_kyc_service import profile_kyc_service

client = TestClient(app)

def test_profile_completion_and_kyc_flow():
    # 1. Create a mock incomplete user
    test_uid = "test-user-kyc-101"
    user_repo.delete(test_uid)

    user = User(
        id=test_uid,
        email="traveler@test.com",
        phone="+919876543210",
        full_name="Akash Traveler",
        profile_status="PROFILE_INCOMPLETE",
        profile_completion=30.0,
        kyc_status="NOT_STARTED",
        kyc_verified=False,
        onboarding_step=1
    )
    user_repo.create(user)

    # 2. Check initial eligibility -> should be NOT ELIGIBLE
    eligibility = profile_kyc_service.check_trip_eligibility(test_uid)
    assert eligibility["eligible"] is False
    assert eligibility["kyc_verified"] is False
    assert "KYC_VERIFICATION" in eligibility["missing_requirements"]

    # 3. Save Step 1: Personal Details
    step1_res = profile_kyc_service.save_onboarding_step(
        test_uid,
        step=1,
        payload={
            "full_name": "Akash Traveler",
            "first_name": "Akash",
            "last_name": "Traveler",
            "date_of_birth": "1998-05-15",
            "gender": "Male",
            "nationality": "Indian",
            "language": "English"
        }
    )
    assert step1_res["success"] is True

    # 4. Save Step 2: Contact & Address
    step2_res = profile_kyc_service.save_onboarding_step(
        test_uid,
        step=2,
        payload={
            "phone": "+919876543210",
            "email": "traveler@test.com",
            "address_line1": "Flat 402, Lotus Towers",
            "address_line2": "MG Road",
            "city": "Bengaluru",
            "state": "Karnataka",
            "country": "India",
            "postal_code": "560001"
        }
    )
    assert step2_res["success"] is True
    assert step2_res["profile_completion"] == 100.0
    assert step2_res["profile_status"] == "PROFILE_COMPLETE"

    # 5. Skip KYC -> Profile complete (100%), but KYC skipped -> Trip Planning STILL LOCKED
    skip_res = profile_kyc_service.skip_kyc(test_uid)
    assert skip_res["success"] is True
    assert skip_res["kyc_status"] == "SKIPPED"
    assert skip_res["kyc_verified"] is False

    eligibility_skipped = profile_kyc_service.check_trip_eligibility(test_uid)
    assert eligibility_skipped["eligible"] is False
    assert eligibility_skipped["profile_complete"] is True
    assert eligibility_skipped["kyc_verified"] is False
    assert "KYC_VERIFICATION" in eligibility_skipped["missing_requirements"]

    # 6. Verify KYC -> Profile complete + KYC verified -> Trip Planning UNLOCKED
    verify_res = profile_kyc_service.verify_kyc(test_uid, document_type="Passport", document_number="J1234567")
    assert verify_res["success"] is True
    assert verify_res["kyc_status"] == "VERIFIED"
    assert verify_res["kyc_verified"] is True

    eligibility_verified = profile_kyc_service.check_trip_eligibility(test_uid)
    assert eligibility_verified["eligible"] is True
    assert eligibility_verified["profile_complete"] is True
    assert eligibility_verified["kyc_verified"] is True
    assert len(eligibility_verified["missing_requirements"]) == 0

    # Cleanup
    user_repo.delete(test_uid)
