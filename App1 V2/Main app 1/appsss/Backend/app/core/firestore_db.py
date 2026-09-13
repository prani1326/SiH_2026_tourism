import json
import logging
import os
import uuid
from datetime import datetime, timezone
from typing import Any, Dict, List, Optional, Union, Tuple, Iterator

from app.core.config import settings

logger = logging.getLogger("tourist_app.firestore")

class MockDocumentSnapshot:
    def __init__(self, doc_id: str, data: Optional[Dict[str, Any]], exists: bool = True):
        self.id = doc_id
        self._data = data.copy() if data else {}
        self.exists = exists

    def to_dict(self) -> Dict[str, Any]:
        return self._data.copy() if self._data else {}

    def get(self, field_path: str, default: Any = None) -> Any:
        return self._data.get(field_path, default)

    @property
    def create_time(self):
        return self._data.get("created_at")

    @property
    def update_time(self):
        return self._data.get("updated_at")


class MockQuery:
    def __init__(self, collection_ref, filters=None, order_by_fields=None, limit_val=None, offset_val=None):
        self.collection_ref = collection_ref
        self.filters = filters or []  # list of (field, op, val)
        self.order_by_fields = order_by_fields or []  # list of (field, direction)
        self.limit_val = limit_val
        self.offset_val = offset_val

    def where(self, field_or_filter=None, op=None, value=None, filter=None):
        new_filters = list(self.filters)
        if filter is not None:
            # FieldFilter object support
            field_name = getattr(filter, "field_path", None) or getattr(filter, "field", "id")
            operator_str = getattr(filter, "op_string", None) or getattr(filter, "operator", "==")
            filter_val = getattr(filter, "value", None)
            new_filters.append((field_name, operator_str, filter_val))
        elif field_or_filter is not None and op is not None:
            new_filters.append((field_or_filter, op, value))
        return MockQuery(self.collection_ref, new_filters, self.order_by_fields, self.limit_val, self.offset_val)

    def order_by(self, field: str, direction: str = "ASCENDING"):
        new_orders = list(self.order_by_fields)
        new_orders.append((field, direction))
        return MockQuery(self.collection_ref, self.filters, new_orders, self.limit_val, self.offset_val)

    def limit(self, count: int):
        return MockQuery(self.collection_ref, self.filters, self.order_by_fields, count, self.offset_val)

    def offset(self, count: int):
        return MockQuery(self.collection_ref, self.filters, self.order_by_fields, self.limit_val, count)

    def _execute(self) -> List[MockDocumentSnapshot]:
        docs = self.collection_ref._get_all_docs()
        filtered = []
        for d in docs:
            match = True
            for field, op, val in self.filters:
                doc_val = d.get(field)
                if op in ("==", "="):
                    if doc_val != val:
                        match = False
                        break
                elif op == "!=":
                    if doc_val == val:
                        match = False
                        break
                elif op == ">":
                    if doc_val is None or not (doc_val > val):
                        match = False
                        break
                elif op == ">=":
                    if doc_val is None or not (doc_val >= val):
                        match = False
                        break
                elif op == "<":
                    if doc_val is None or not (doc_val < val):
                        match = False
                        break
                elif op == "<=":
                    if doc_val is None or not (doc_val <= val):
                        match = False
                        break
                elif op == "in":
                    if doc_val not in (val or []):
                        match = False
                        break
                elif op == "not-in":
                    if doc_val in (val or []):
                        match = False
                        break
                elif op == "array-contains":
                    if not isinstance(doc_val, list) or val not in doc_val:
                        match = False
                        break
            if match:
                filtered.append(d)

        # Ordering
        for field, direction in reversed(self.order_by_fields):
            desc = "DESC" in str(direction).upper()
            filtered.sort(key=lambda x: str(x.get(field, "")), reverse=desc)

        # Offset & Limit
        if self.offset_val:
            filtered = filtered[self.offset_val:]
        if self.limit_val is not None:
            filtered = filtered[:self.limit_val]

        return [MockDocumentSnapshot(d["id"], d, exists=True) for d in filtered]

    def stream(self) -> Iterator[MockDocumentSnapshot]:
        return iter(self._execute())

    def get(self) -> List[MockDocumentSnapshot]:
        return self._execute()


class MockDocumentReference:
    def __init__(self, collection_ref, doc_id: str):
        self.collection_ref = collection_ref
        self.id = doc_id

    def get(self) -> MockDocumentSnapshot:
        doc = self.collection_ref._get_doc(self.id)
        if doc is None:
            return MockDocumentSnapshot(self.id, None, exists=False)
        return MockDocumentSnapshot(self.id, doc, exists=True)

    def set(self, data: Dict[str, Any], merge: bool = False):
        self.collection_ref._set_doc(self.id, data, merge=merge)

    def update(self, data: Dict[str, Any]):
        doc = self.collection_ref._get_doc(self.id)
        if doc is None:
            raise ValueError(f"Document {self.id} does not exist for update")
        self.collection_ref._set_doc(self.id, data, merge=True)

    def delete(self):
        self.collection_ref._delete_doc(self.id)


class MockCollectionReference:
    def __init__(self, db, name: str):
        self.db = db
        self.name = name

    def document(self, doc_id: Optional[str] = None) -> MockDocumentReference:
        if not doc_id:
            doc_id = str(uuid.uuid4())
        return MockDocumentReference(self, doc_id)

    def add(self, data: Dict[str, Any], doc_id: Optional[str] = None) -> Tuple[Any, MockDocumentReference]:
        if not doc_id:
            doc_id = data.get("id") or str(uuid.uuid4())
        ref = self.document(doc_id)
        ref.set(data)
        return datetime.now(timezone.utc), ref

    def where(self, field_or_filter=None, op=None, value=None, filter=None) -> MockQuery:
        q = MockQuery(self)
        return q.where(field_or_filter=field_or_filter, op=op, value=value, filter=filter)

    def order_by(self, field: str, direction: str = "ASCENDING") -> MockQuery:
        return MockQuery(self).order_by(field, direction)

    def limit(self, count: int) -> MockQuery:
        return MockQuery(self).limit(count)

    def offset(self, count: int) -> MockQuery:
        return MockQuery(self).offset(count)

    def stream(self) -> Iterator[MockDocumentSnapshot]:
        return MockQuery(self).stream()

    def get(self) -> List[MockDocumentSnapshot]:
        return MockQuery(self).get()

    def _get_doc(self, doc_id: str) -> Optional[Dict[str, Any]]:
        return self.db._store.get(self.name, {}).get(doc_id)

    def _set_doc(self, doc_id: str, data: Dict[str, Any], merge: bool = False):
        if self.name not in self.db._store:
            self.db._store[self.name] = {}
        payload = data.copy()
        if "id" not in payload:
            payload["id"] = doc_id
        if merge and doc_id in self.db._store[self.name]:
            existing = self.db._store[self.name][doc_id]
            existing.update(payload)
            existing["updated_at"] = payload.get("updated_at") or datetime.now(timezone.utc).isoformat()
        else:
            now_iso = datetime.now(timezone.utc).isoformat()
            if "created_at" not in payload:
                payload["created_at"] = now_iso
            payload["updated_at"] = now_iso
            self.db._store[self.name][doc_id] = payload
        self.db._persist()

    def _delete_doc(self, doc_id: str):
        if self.name in self.db._store and doc_id in self.db._store[self.name]:
            del self.db._store[self.name][doc_id]
            self.db._persist()

    def _get_all_docs(self) -> List[Dict[str, Any]]:
        return list(self.db._store.get(self.name, {}).values())


class MockWriteBatch:
    def __init__(self, db: "MockFirestoreClient"):
        self.db = db
        self.operations = []

    def set(self, doc_ref: "MockDocumentReference", data: Dict[str, Any], merge: bool = False):
        self.operations.append(("set", doc_ref, data, merge))

    def update(self, doc_ref: "MockDocumentReference", data: Dict[str, Any]):
        self.operations.append(("update", doc_ref, data, False))

    def delete(self, doc_ref: "MockDocumentReference"):
        self.operations.append(("delete", doc_ref, None, False))

    def commit(self):
        prev_persist = self.db.auto_persist
        self.db.auto_persist = False
        try:
            for op, ref, data, merge in self.operations:
                if op in ("set", "update"):
                    ref.set(data, merge=merge)
                elif op == "delete":
                    ref.delete()
        finally:
            self.db.auto_persist = prev_persist
            self.db._persist()


BASE_DIR = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
DEFAULT_PERSISTENCE_FILE = os.path.join(BASE_DIR, "firestore_local_store.json")
BACKUP_PERSISTENCE_FILE = os.path.join(BASE_DIR, "backups", "firestore_local_store_backup.json")

class MockFirestoreClient:
    def __init__(self, persistence_file: Optional[str] = None):
        if persistence_file:
            self.persistence_file = os.path.abspath(persistence_file) if not os.path.isabs(persistence_file) else persistence_file
        else:
            self.persistence_file = DEFAULT_PERSISTENCE_FILE
        self.auto_persist: bool = True
        self._store: Dict[str, Dict[str, Dict[str, Any]]] = {}
        self._load()

    def batch(self) -> MockWriteBatch:
        return MockWriteBatch(self)

    def _load(self):
        loaded = False
        if self.persistence_file and os.path.exists(self.persistence_file):
            try:
                with open(self.persistence_file, "r", encoding="utf-8") as f:
                    self._store = json.load(f)
                    if self._store and "destinations" in self._store and len(self._store["destinations"]) > 0:
                        loaded = True
            except Exception as e:
                logger.warning(f"Could not load local firestore store: {e}")
                self._store = {}

        if not loaded and os.path.exists(BACKUP_PERSISTENCE_FILE):
            try:
                with open(BACKUP_PERSISTENCE_FILE, "r", encoding="utf-8") as f:
                    self._store = json.load(f)
                    logger.info("Loaded baseline seed data from firestore backup store.")
                    self._persist()
            except Exception as e:
                logger.warning(f"Could not load fallback backup firestore store: {e}")

    def _persist(self):
        if not self.auto_persist:
            return
        if self.persistence_file:
            try:
                with open(self.persistence_file, "w", encoding="utf-8") as f:
                    json.dump(self._store, f, indent=2, default=str)
            except Exception as e:
                logger.warning(f"Could not persist local firestore store: {e}")

    def collection(self, collection_name: str) -> MockCollectionReference:
        return MockCollectionReference(self, collection_name)

    def collections(self) -> List[MockCollectionReference]:
        return [MockCollectionReference(self, name) for name in self._store.keys()]


class FirestoreManager:
    """
    Enterprise Firestore Manager.
    Provides singleton access to Cloud Firestore with automatic emulator support,
    GCP authentication, or resilient development mock store.
    """
    _instance: Optional["FirestoreManager"] = None

    def __init__(self):
        self.client = None
        self.is_mock: bool = False
        self._initialize()

    def _initialize(self):
        # 1. Check if emulator host is configured
        emulator_host = settings.FIRESTORE_EMULATOR_HOST or os.environ.get("FIRESTORE_EMULATOR_HOST")
        if emulator_host:
            try:
                from google.cloud import firestore
                project = settings.FIRESTORE_PROJECT_ID or settings.FIREBASE_PROJECT_ID or "tourist-app-production"
                self.client = firestore.Client(project=project)
                self.is_mock = False
                logger.info(f"Connected to Firestore Emulator at {emulator_host}")
                return
            except Exception as exc:
                logger.warning(f"Failed to connect to Firestore emulator: {exc}")

        # 2. Check if Firebase service account credentials file is configured
        cred_path = settings.FIREBASE_CREDENTIALS_PATH or os.environ.get("FIREBASE_CREDENTIALS_PATH") or os.environ.get("GOOGLE_APPLICATION_CREDENTIALS")
        if cred_path:
            if not os.path.exists(cred_path):
                msg = f"Configured Firebase credentials file does not exist: {cred_path}"
                logger.error(msg)
                if settings.ENVIRONMENT == "production":
                    raise FileNotFoundError(msg)
            else:
                try:
                    import firebase_admin
                    from firebase_admin import credentials, firestore
                    if not firebase_admin._apps:
                        cred = credentials.Certificate(cred_path)
                        firebase_admin.initialize_app(cred, {
                            "projectId": settings.FIREBASE_PROJECT_ID,
                            "storageBucket": settings.FIREBASE_STORAGE_BUCKET
                        })
                    self.client = firestore.client()
                    # Verify connectivity
                    list(self.client.collection("destinations").limit(1).stream())
                    self.is_mock = False
                    logger.info("Connected to live Firebase Firestore using Service Account credentials.")
                    return
                except Exception as exc:
                    logger.error(f"Live Firestore connection failed using certificate {cred_path}: {exc}")
                    if settings.ENVIRONMENT == "production" or not settings.FIREBASE_MOCK_MODE:
                        if settings.ENVIRONMENT == "production":
                            raise RuntimeError(f"Production Firestore initialization failed: {exc}")

        # 3. Check for Application Default Credentials (ADC) in cloud environments
        has_adc = "GOOGLE_APPLICATION_CREDENTIALS" in os.environ or "GOOGLE_CLOUD_PROJECT" in os.environ
        if settings.ENVIRONMENT == "production" or (not settings.FIREBASE_MOCK_MODE and has_adc and not cred_path):
            try:
                import firebase_admin
                from firebase_admin import credentials, firestore
                if not firebase_admin._apps:
                    cred = credentials.ApplicationDefault()
                    firebase_admin.initialize_app(cred, {
                        "projectId": settings.FIREBASE_PROJECT_ID,
                        "storageBucket": settings.FIREBASE_STORAGE_BUCKET
                    })
                test_client = firestore.client()
                # Test connectivity
                list(test_client.collection("destinations").limit(1).stream())
                self.client = test_client
                self.is_mock = False
                logger.info("Connected to live Firebase Firestore using Application Default Credentials.")
                return
            except Exception as exc:
                if settings.ENVIRONMENT == "production":
                    logger.error(f"FATAL: Production Firestore connection via ADC failed: {exc}")
                    raise RuntimeError(
                        "Production Firebase Firestore connection failed. "
                        "Please configure a valid Service Account via FIREBASE_CREDENTIALS_PATH or GOOGLE_APPLICATION_CREDENTIALS."
                    )
                else:
                    logger.info(f"Cloud Firestore ADC not available ({exc}). Falling back to local store for development/testing.")

        # 4. Isolated development / testing mock store fallback
        self.client = MockFirestoreClient()
        self.is_mock = True
        logger.info("Initialized local persistent Firestore client for isolated development / testing.")

    @classmethod
    def get_instance(cls) -> "FirestoreManager":
        if cls._instance is None:
            cls._instance = FirestoreManager()
        return cls._instance

    @classmethod
    def get_client(cls):
        return cls.get_instance().client

    @classmethod
    def reset_instance(cls):
        cls._instance = None


firestore_manager = FirestoreManager.get_instance()

def get_firestore() -> Any:
    """FastAPI Dependency for Firestore client."""
    return firestore_manager.get_client()
