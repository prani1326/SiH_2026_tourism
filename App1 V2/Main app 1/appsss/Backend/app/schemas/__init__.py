from app.schemas.common import BaseResponse, StatusResponse, PaginatedResponse
from app.schemas.auth import (
    SignupRequest, LoginRequest, PhoneOtpSendRequest, PhoneOtpVerifyRequest,
    GoogleLoginRequest, RefreshTokenRequest, PasswordRecoveryRequest,
    PasswordRecoveryConfirm, TokenResponse, SplashInitResponse
)
from app.schemas.user import (
    UserProfileUpdate, TravelPreferencesUpdate, EmergencyContactCreate,
    EmergencyContactOut, SavedItemCreate, SavedItemOut, SavedCollectionCreate,
    SavedCollectionOut, UserOut
)
from app.schemas.destination import (
    DestinationOut, DestinationDetailOut, PlaceAttractionOut, DayPassBundleOut,
    ReviewCreate, ReviewOut, GlobalSearchQuery, MenuScanRequest, MenuScanResponse
)
from app.schemas.trip import (
    AITripPlanRequest, TripActivityCreate, TripActivityUpdate, ReorderActivitiesRequest,
    DisruptionReplanRequest, TrueTripCostResponse, TripOut, TripDayOut, TripActivityOut,
    DigitalTripCardOut
)
from app.schemas.booking import (
    RequestToBookRequest, BookingOut, PaymentProcessRequest, PaymentReceiptOut,
    CancellationRequest, RefundStatusOut
)
from app.schemas.safety import (
    SOSAlertRequest, SOSAlertResponse, LostPhoneRecoverRequest, LostPhoneRecoverResponse,
    SeparatedGroupCreate, GroupMemberCheckin, GroupStatusResponse
)
from app.schemas.community import (
    CommunityPostCreate, CommunityCommentCreate, CommunityPostOut, CommunityForumOut,
    CreatorItineraryOut, GroupVoteRequest, GroupExpenseSplitRequest
)
from app.schemas.support import (
    SupportTicketCreate, TicketMessageCreate, TicketMessageOut, SupportTicketOut, NotificationOut
)
