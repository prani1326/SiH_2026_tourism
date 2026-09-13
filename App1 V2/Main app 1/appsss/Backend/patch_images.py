import os
import sys

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), ".")))

from app.core.firestore_db import get_firestore

def patch():
    db = get_firestore()
    docs = db.collection("destinations").get()
    for doc in docs:
        data = doc.to_dict()
        if "hero_image_url" not in data or not data["hero_image_url"]:
            print(f"Patching {doc.id}")
            db.collection("destinations").document(doc.id).update({
                "hero_image_url": data.get("cover_image", "https://images.unsplash.com/photo-1564507592333-c60657eea523?auto=format&fit=crop&w=1200&q=80")
            })
    print("Patch complete")

if __name__ == "__main__":
    patch()
