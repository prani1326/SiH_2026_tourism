from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
from app.core.config import settings
from app.core.firestore_db import get_firestore
from scripts.seed_firestore import seed_firestore
from app.api.v1.api import api_router
from fastapi.responses import JSONResponse
from fastapi.exceptions import RequestValidationError
from starlette.exceptions import HTTPException as StarletteHTTPException

@asynccontextmanager
async def lifespan(app: FastAPI):
    """Application lifespan: initialize Firestore and seed baseline demo content if empty."""
    try:
        # Initialize Firestore client
        get_firestore()
        # Seed baseline destinations and demo accounts if needed
        seed_firestore()
    except Exception as e:
        print(f"[Lifespan Warning] Firestore initialization note: {e}")
    yield

app = FastAPI(
    title=settings.PROJECT_NAME,
    version=settings.VERSION,
    description=settings.DESCRIPTION,
    openapi_url=f"{settings.API_V1_STR}/openapi.json",
    docs_url="/docs",
    redoc_url="/redoc",
    lifespan=lifespan
)

from app.core.middleware import (
    SecurityHeadersMiddleware, RateLimiterMiddleware, RequestTracingMiddleware
)

# CORS middleware for mobile apps and web clients
cors_origins = [str(origin).rstrip("/") for origin in settings.BACKEND_CORS_ORIGINS]

app.add_middleware(
    CORSMiddleware,
    allow_origins=cors_origins,
    allow_credentials=True,
    allow_methods=["GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"],
    allow_headers=["*"],
    expose_headers=["X-Request-ID", "X-Process-Time-Ms"]
)

# Serve static files for avatar uploads
app.mount("/static", StaticFiles(directory="static"), name="static")

# Enterprise Security & Tracing Middlewares
app.add_middleware(SecurityHeadersMiddleware)
app.add_middleware(RateLimiterMiddleware)
app.add_middleware(RequestTracingMiddleware)

@app.exception_handler(StarletteHTTPException)
async def http_exception_handler(request, exc):
    return JSONResponse(
        status_code=exc.status_code,
        content={
            "detail": exc.detail,
            "error": {"code": exc.status_code, "message": exc.detail}
        },
    )

@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request, exc):
    return JSONResponse(
        status_code=422,
        content={
            "detail": exc.errors() if hasattr(exc, "errors") else str(exc),
            "error": {"code": 422, "message": str(exc)}
        },
    )

# Mount master v1 API router
app.include_router(api_router, prefix=settings.API_V1_STR)

@app.get("/")
def root():
    """Welcome Root: Overview, documentation links and navigation structure."""
    return {
        "status": "online",
        "service": settings.PROJECT_NAME,
        "version": settings.VERSION,
        "interactive_docs": "/docs",
        "redoc_docs": "/redoc",
        "api_v1_base": settings.API_V1_STR,
        "bottom_navigation_spec": [
            {"tab": "Home", "endpoint": f"{settings.API_V1_STR}/destinations?popular_only=true"},
            {"tab": "Explore", "endpoint": f"{settings.API_V1_STR}/explore/categories"},
            {"tab": "Trips", "endpoint": f"{settings.API_V1_STR}/trips"},
            {"tab": "Profile", "endpoint": f"{settings.API_V1_STR}/users/me"}
        ],
        "emergency_sos_always_accessible": f"{settings.API_V1_STR}/safety/sos"
    }

@app.get("/health")
def health_check():
    """Enterprise health & connectivity diagnostic endpoint reporting REAL runtime state."""
    import firebase_admin
    from app.core.firestore_db import get_firestore, firestore_manager
    try:
        db = get_firestore()
        is_mock = firestore_manager.is_mock
        db_mode = "isolated_development_store" if is_mock else "firebase_cloud"
        
        # Verify read access
        if is_mock:
            db.collection("destinations").limit(1).get()
        else:
            list(db.collection("destinations").limit(1).stream())
            
        admin_status = "connected" if bool(firebase_admin._apps) else "not_initialized"
        storage_status = "configured" if bool(settings.FIREBASE_STORAGE_BUCKET) else "not_configured"

        # In production mode, if mock database is active, status is degraded
        if settings.ENVIRONMENT == "production":
            is_healthy = not is_mock
        else:
            is_healthy = True

        return {
            "status": "healthy" if is_healthy else "degraded",
            "service": settings.PROJECT_NAME,
            "version": settings.VERSION,
            "firebase_project": settings.FIREBASE_PROJECT_ID,
            "firebase_admin": admin_status,
            "firestore": "connected",
            "storage": storage_status,
            "mock_mode": is_mock,
            "database_mode": db_mode
        }
    except Exception as e:
        return {
            "status": "unhealthy",
            "service": settings.PROJECT_NAME,
            "firebase_project": settings.FIREBASE_PROJECT_ID,
            "database_mode": "disconnected",
            "error": str(e)
        }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
