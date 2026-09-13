import json
import os
import sys

# Add parent directory to sys.path
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

import firebase_admin
from firebase_admin import credentials, firestore

def sync_all():
    print("=" * 70)
    print("Starting Comprehensive Firebase Cloud Sync...")
    print("=" * 70)

    # Path to service account key and local store
    base_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
    cred_path = os.path.join(base_dir, "serviceAccountKey.json")
    local_store_path = os.path.join(base_dir, "firestore_local_store.json")

    if not os.path.exists(cred_path):
        print(f"Error: Credentials not found at {cred_path}")
        return
    if not os.path.exists(local_store_path):
        print(f"Error: Local store not found at {local_store_path}")
        return

    # Initialize Firebase Admin if not already initialized
    if not firebase_admin._apps:
        cred = credentials.Certificate(cred_path)
        firebase_admin.initialize_app(cred, {"projectId": "trip-planner-version-1"})

    db = firestore.client()

    with open(local_store_path, "r", encoding="utf-8") as f:
        local_store = json.load(f)

    summary = {}
    total_added = 0
    total_already_existed = 0

    for col_name, items in local_store.items():
        if not isinstance(items, dict):
            continue

        col_ref = db.collection(col_name)
        existing_ids = {doc.id for doc in col_ref.stream()}

        added_in_col = 0
        already_in_col = 0

        batch = db.batch()
        batch_count = 0

        for doc_id, doc_data in items.items():
            str_id = str(doc_id).strip()
            if not str_id:
                continue

            if str_id in existing_ids:
                already_in_col += 1
            else:
                # Need to upload to Cloud Firestore
                data = dict(doc_data) if isinstance(doc_data, dict) else {"value": doc_data}
                data.setdefault("id", str_id)
                target_doc = col_ref.document(str_id)
                batch.set(target_doc, data)
                batch_count += 1
                added_in_col += 1
                existing_ids.add(str_id)

                if batch_count >= 400:
                    batch.commit()
                    batch = db.batch()
                    batch_count = 0

        if batch_count > 0:
            batch.commit()

        summary[col_name] = {
            "local_count": len(items),
            "added_to_cloud": added_in_col,
            "already_in_cloud": already_in_col
        }
        total_added += added_in_col
        total_already_existed += already_in_col
        print(f"Collection '{col_name}': {added_in_col} uploaded, {already_in_col} already existed.")

    print("=" * 70)
    print(f"Sync complete! Total new records saved to Firebase: {total_added}")
    print(f"Total existing records verified: {total_already_existed}")
    print("=" * 70)

if __name__ == "__main__":
    sync_all()
