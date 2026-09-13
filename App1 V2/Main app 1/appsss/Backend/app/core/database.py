import logging
from contextlib import contextmanager
from typing import Any, Generator
from app.core.firestore_db import get_firestore, firestore_manager

logger = logging.getLogger("tourist_app.database")

class FirestoreSessionAdapter:
    """
    Session adapter providing both direct Firestore client access
    and compatibility methods for request lifecycle.
    """
    def __init__(self, client=None):
        self.client = client or get_firestore()

    def collection(self, name: str):
        return self.client.collection(name)

    def commit(self):
        """No-op for Firestore auto-commit."""
        pass

    def rollback(self):
        """No-op for Firestore auto-commit."""
        pass

    def close(self):
        """Cleanup handler."""
        pass

    def add(self, entity):
        pass

    def refresh(self, entity):
        pass


def get_db() -> Generator[Any, None, None]:
    """
    Primary database dependency for FastAPI routes.
    Supplies the active Firebase Firestore client / adapter.
    """
    adapter = FirestoreSessionAdapter()
    try:
        yield adapter.client
    finally:
        adapter.close()


def get_firestore_db() -> Any:
    """Direct dependency for obtaining the Firestore client."""
    return get_firestore()


@contextmanager
def atomic_transaction(db=None):
    """
    Context manager for transactions.
    """
    try:
        yield db
    except Exception:
        raise
