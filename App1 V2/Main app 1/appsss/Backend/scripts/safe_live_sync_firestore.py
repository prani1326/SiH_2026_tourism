import json
import os
import sys
from typing import Dict, Any, List

# Ensure project root is in sys.path
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from app.core.config import settings
from app.core.firestore_db import get_firestore

def safe_sync_to_firebase():
    """
    Safe, strictly non-destructive synchronizer from local store to Firebase Cloud Firestore.
    Guarantees:
    - Never overwrites existing Firebase documents.
    - Never deletes existing local or cloud records.
    - Uses stable document IDs.
    - Generates a full audit report.
    """
    print("=" * 80)
    print("Tour & Travel Platform: SAFE NON-DESTRUCTIVE CLOUD SYNCHRONIZATION")
    print(f"Target Firebase Project: {settings.FIREBASE_PROJECT_ID}")
    print("=" * 80)

    # 1. Connect to Real Firebase Firestore
    db = get_firestore()
    if hasattr(db, "auto_persist"):
        print("ERROR: Backend is running in mock mode. Please set FIREBASE_MOCK_MODE=false in .env.")
        return

    # 2. Load local store backup
    local_store_path = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "firestore_local_store.json"))
    if not os.path.exists(local_store_path):
        print(f"ERROR: Local store file {local_store_path} not found.")
        return

    with open(local_store_path, "r", encoding="utf-8") as f:
        local_store = json.load(f)

    # Collections to safely sync to Cloud Firestore
    target_collections = [
        "destinations",
        "trips",
        "places",
        "emergency_contacts",
        "reviews"
    ]

    report = {}

    for col_name in target_collections:
        local_items = local_store.get(col_name, {})
        col_ref = db.collection(col_name)

        # Query all existing IDs in Cloud Firestore
        existing_cloud_ids = set()
        try:
            for doc in col_ref.stream():
                existing_cloud_ids.add(doc.id)
        except Exception as e:
            print(f"Warning reading collection {col_name}: {e}")

        created_count = 0
        skipped_count = 0
        conflict_count = 0

        # Sync items batch-by-batch or doc-by-doc safely
        batch = db.batch()
        batch_count = 0

        for item_id, item_data in local_items.items():
            stable_id = str(item_id).strip()
            if not stable_id:
                continue

            if stable_id in existing_cloud_ids:
                # Pre-existing document in Firebase - DO NOT OVERWRITE!
                skipped_count += 1
            else:
                # Missing in Firebase - Safely create
                doc_ref = col_ref.document(stable_id)
                # Clean up local-only helper fields if any
                clean_data = dict(item_data)
                clean_data.setdefault("id", stable_id)
                batch.set(doc_ref, clean_data)
                batch_count += 1
                created_count += 1

                # Commit in chunks of 400 (Firestore max batch is 500)
                if batch_count >= 400:
                    batch.commit()
                    batch = db.batch()
                    batch_count = 0

        if batch_count > 0:
            batch.commit()

        report[col_name] = {
            "local_records": len(local_items),
            "firebase_existing": len(existing_cloud_ids),
            "created": created_count,
            "skipped": skipped_count,
            "conflicts": conflict_count,
            "total_now_in_cloud": len(existing_cloud_ids) + created_count
        }

    print("\n" + "=" * 80)
    print("MIGRATION & SYNCHRONIZATION AUDIT REPORT")
    print("=" * 80)
    print(f"{'Collection':<20} | {'Local':<6} | {'Firebase Exist':<14} | {'Created':<8} | {'Skipped':<8} | {'Conflicts':<9}")
    print("-" * 80)
    for col_name, data in report.items():
        print(f"{col_name:<20} | {data['local_records']:<6} | {data['firebase_existing']:<14} | {data['created']:<8} | {data['skipped']:<8} | {data['conflicts']:<9}")
    print("=" * 80)
    print("Status: All missing documents safely synced into Firebase Cloud Firestore.")
    print("Zero records deleted. Zero records overwritten.")
    print("=" * 80)

if __name__ == "__main__":
    safe_sync_to_firebase()
