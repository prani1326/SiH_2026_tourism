from fastapi import APIRouter
from app.api.v1.endpoints import (
    auth, users, destinations, places, search, explore, trips,
    trip_os, budget, bookings, payments, refunds, trip_card,
    safety_sos, lost_phone, separated_mode, transport_brain,
    food_culture, preparation, reviews, community, groups,
    collections, bundles_events, support, notifications,
    translation, visa_vault, sim_esim, insurance, guide,
    safety_intelligence, crowd_intelligence, guardian, emergency_sync,
    heritage, scam_shield, sustainability, local_experiences, authority_intelligence
)

api_router = APIRouter()

api_router.include_router(auth.router, prefix="/auth", tags=["1. Authentication & Onboarding"])
api_router.include_router(users.router, prefix="/users", tags=["2. User Profile & Preferences"])
api_router.include_router(destinations.router, prefix="/destinations", tags=["3. Destinations & Guides"])
api_router.include_router(places.router, prefix="/places", tags=["4. Places & Attractions"])
api_router.include_router(search.router, prefix="/search", tags=["5. Global Search & Geolocation"])
api_router.include_router(explore.router, prefix="/explore", tags=["6. Explore Categories & Maps"])
api_router.include_router(trips.router, prefix="/trips", tags=["7. AI Trip Planner & Itinerary Builder"])
api_router.include_router(trip_os.router, prefix="/trip-os", tags=["8. Trip OS & Auto Re-planning"])
api_router.include_router(budget.router, prefix="/budget", tags=["9. True Cost & Budget Drift Alerts"])
api_router.include_router(bookings.router, prefix="/bookings", tags=["10. Request-to-Book & Vouchers"])
api_router.include_router(payments.router, prefix="/payments", tags=["11. Payments & Invoices"])
api_router.include_router(refunds.router, prefix="/refunds", tags=["12. Cancellations & Refunds"])
api_router.include_router(trip_card.router, prefix="/trip-card", tags=["13. Digital Trip Card & Offline Sync"])
api_router.include_router(safety_sos.router, prefix="/safety", tags=["14. Safety, SOS & Intelligence"])
api_router.include_router(safety_intelligence.router, prefix="/safety-intel", tags=["14B. AI Tourist Safety Intelligence"])
api_router.include_router(crowd_intelligence.router, prefix="/crowd", tags=["14C. Tourist Crowd Intelligence"])
api_router.include_router(guardian.router, prefix="/guardian", tags=["14D. Tourist Guardian & Family Safety"])
api_router.include_router(emergency_sync.router, prefix="/emergency", tags=["14E. Offline SOS Mesh & Emergency Mode"])
api_router.include_router(heritage.router, prefix="/heritage", tags=["14F. AI Heritage Lens"])
api_router.include_router(scam_shield.router, prefix="/scams", tags=["14G. Tourist Scam Shield"])
api_router.include_router(sustainability.router, prefix="/sustainability", tags=["14H. Sustainable Trip Score"])
api_router.include_router(local_experiences.router, prefix="/local-experiences", tags=["14I. Local Experiences"])
api_router.include_router(authority_intelligence.router, prefix="/intelligence", tags=["14J. Tourism Authority Intelligence"])
api_router.include_router(lost_phone.router, prefix="/lost-phone", tags=["15. Lost Phone Mode Recovery"])
api_router.include_router(separated_mode.router, prefix="/separated-mode", tags=["16. Lost Person & Separated Mode"])
api_router.include_router(transport_brain.router, prefix="/transport-brain", tags=["17. Local Transport Brain"])
api_router.include_router(food_culture.router, prefix="/food-culture", tags=["18. Food & Cultural Engine"])
api_router.include_router(preparation.router, prefix="/preparation", tags=["19. Preparation Engine"])
api_router.include_router(reviews.router, prefix="/reviews", tags=["20. Verified Reviews & Confidence"])
api_router.include_router(community.router, prefix="/community", tags=["21. Community & Creator Marketplace"])
api_router.include_router(groups.router, prefix="/groups", tags=["22. Group Planning & Voting"])
api_router.include_router(collections.router, prefix="/collections", tags=["23. Saved Collections"])
api_router.include_router(bundles_events.router, prefix="/bundles-events", tags=["24. Day Passes & Events"])
api_router.include_router(support.router, prefix="/support", tags=["25. 24/7 Support Concierge"])
api_router.include_router(notifications.router, prefix="/notifications", tags=["26. Notification Center"])
api_router.include_router(translation.router, prefix="/translation", tags=["27. Translation & Voice"])
api_router.include_router(visa_vault.router, prefix="/visa-vault", tags=["28. Visa & Document Vault"])
api_router.include_router(sim_esim.router, prefix="/sim-esim", tags=["29. SIM & eSIM"])
api_router.include_router(insurance.router, prefix="/insurance", tags=["30. Travel Insurance"])
api_router.include_router(guide.router, prefix="/guide", tags=["31. AI Travel Guide & Vision/Voice"])


