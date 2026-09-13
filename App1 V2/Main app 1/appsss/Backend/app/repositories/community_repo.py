from typing import Any, Dict, List, Optional
from app.models.firestore_models import CommunityForum, CommunityPost, CommunityComment, CreatorItinerary
from app.repositories.base_repo import BaseFirestoreRepository

class CommunityRepository(BaseFirestoreRepository[CommunityForum]):
    collection_name = "community_forums"
    model_class = CommunityForum

    def list_forums(self) -> List[CommunityForum]:
        return self.list_all(limit=50)

    # Posts
    def list_posts(self, forum_id: Optional[str] = None, limit: int = 50) -> List[CommunityPost]:
        query = self.db.collection("community_posts")
        if forum_id:
            query = query.where("forum_id", "==", str(forum_id))
        snaps = query.limit(limit).get()
        return [CommunityPost.from_dict(s.to_dict(), doc_id=s.id) for s in snaps]

    def get_post(self, post_id: str) -> Optional[CommunityPost]:
        snap = self.db.collection("community_posts").document(str(post_id)).get()
        if not snap.exists:
            return None
        return CommunityPost.from_dict(snap.to_dict(), doc_id=snap.id)

    def create_post(self, post: CommunityPost) -> CommunityPost:
        self.db.collection("community_posts").document(str(post.id)).set(post.to_dict())
        return post

    # Comments
    def list_comments(self, post_id: str) -> List[CommunityComment]:
        snaps = self.db.collection("community_comments").where("post_id", "==", str(post_id)).limit(100).get()
        return [CommunityComment.from_dict(s.to_dict(), doc_id=s.id) for s in snaps]

    def add_comment(self, comment: CommunityComment) -> CommunityComment:
        self.db.collection("community_comments").document(str(comment.id)).set(comment.to_dict())
        # increment comments_count on post
        post = self.get_post(comment.post_id)
        if post:
            self.db.collection("community_posts").document(str(post.id)).update({
                "comments_count": post.comments_count + 1
            })
        return comment

    # Creator Itineraries
    def list_creator_itineraries(self, destination: Optional[str] = None, limit: int = 50) -> List[CreatorItinerary]:
        query = self.db.collection("creator_itineraries")
        if destination:
            query = query.where("destination_name", "==", destination)
        snaps = query.limit(limit).get()
        return [CreatorItinerary.from_dict(s.to_dict(), doc_id=s.id) for s in snaps]

community_repo = CommunityRepository()
