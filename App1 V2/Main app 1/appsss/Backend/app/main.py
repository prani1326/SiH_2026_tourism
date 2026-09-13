"""
Compatibility wrapper for FastAPI application.
Allows running with either `uvicorn main:app` or `uvicorn app.main:app`.
"""
from main import app

__all__ = ["app"]
