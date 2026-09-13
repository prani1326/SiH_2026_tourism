import logging
from typing import Any, Dict, Generic, List, Optional, Type, TypeVar
from app.core.firestore_db import get_firestore
from app.models.firestore_models import FirestoreEntity

logger = logging.getLogger("tourist_app.repository")
T = TypeVar("T", bound=FirestoreEntity)

class BaseFirestoreRepository(Generic[T]):
    collection_name: str = ""
    model_class: Type[T] = FirestoreEntity

    def __init__(self, db=None):
        self.db = db or get_firestore()

    @property
    def collection(self):
        return self.db.collection(self.collection_name)

    def get_by_id(self, doc_id: str) -> Optional[T]:
        if not doc_id:
            return None
        doc_ref = self.collection.document(str(doc_id))
        snap = doc_ref.get()
        if not snap.exists:
            return None
        data = snap.to_dict()
        return self.model_class.from_dict(data, doc_id=snap.id)

    def list_all(self, limit: int = 100, offset: int = 0) -> List[T]:
        query = self.collection.limit(limit).offset(offset)
        snaps = query.get()
        return [self.model_class.from_dict(s.to_dict(), doc_id=s.id) for s in snaps]

    get_all = list_all

    def find_one(self, field: str, op: str, value: Any) -> Optional[T]:
        snaps = self.collection.where(field, op, value).limit(1).get()
        if not snaps:
            return None
        s = snaps[0]
        return self.model_class.from_dict(s.to_dict(), doc_id=s.id)

    def find_many(
        self,
        field: Optional[str] = None,
        op: Optional[str] = None,
        value: Any = None,
        filters: Optional[List[tuple]] = None,
        order_by: Optional[str] = None,
        limit: int = 100,
        offset: int = 0
    ) -> List[T]:
        query = self.collection
        if filters:
            for f_field, f_op, f_val in filters:
                query = query.where(f_field, f_op, f_val)
        elif field and op and value is not None:
            query = query.where(field, op, value)

        if order_by:
            query = query.order_by(order_by)

        query = query.limit(limit).offset(offset)
        snaps = query.get()
        return [self.model_class.from_dict(s.to_dict(), doc_id=s.id) for s in snaps]

    def create(self, entity: T) -> T:
        data = entity.to_dict()
        doc_id = str(entity.id)
        self.collection.document(doc_id).set(data)
        return entity

    def update(self, doc_id: str, updates: Dict[str, Any]) -> Optional[T]:
        doc_ref = self.collection.document(str(doc_id))
        snap = doc_ref.get()
        if not snap.exists:
            return None
        doc_ref.update(updates)
        updated_snap = doc_ref.get()
        return self.model_class.from_dict(updated_snap.to_dict(), doc_id=doc_id)

    def delete(self, doc_id: str) -> bool:
        doc_ref = self.collection.document(str(doc_id))
        snap = doc_ref.get()
        if not snap.exists:
            return False
        doc_ref.delete()
        return True

    def count(self, field: Optional[str] = None, op: Optional[str] = None, value: Any = None) -> int:
        query = self.collection
        if field and op and value is not None:
            query = query.where(field, op, value)
        snaps = query.get()
        return len(snaps)
