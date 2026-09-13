const { db } = require('../../config/firebase');
const { COLLECTIONS } = require('../../database/firestoreSchema');
const { sendSuccess, sendError } = require('../../utils/responseHelper');
const auditService = require('../../services/auditService');

const listingController = {
  /**
   * Get all listings for authenticated vendor
   */
  getListings: async (req, res) => {
    try {
      const vId = Number(req.vendorId) || req.vendorId;
      const { status, search, page = 1, limit = 20 } = req.query;
      const pageNum = parseInt(page, 10) || 1;
      const limitNum = parseInt(limit, 10) || 20;

      const col = db.collection(COLLECTIONS.LISTINGS);
      const snap = await col.where('vendor_id', '==', vId).get();

      let items = snap.docs.map((d) => d.data());

      if (status) {
        items = items.filter((item) => item.status === status);
      }

      if (search && search.trim()) {
        const q = search.trim().toLowerCase();
        items = items.filter((item) => {
          const t = (item.title || '').toLowerCase();
          const d = (item.destination || '').toLowerCase();
          const desc = (item.description || '').toLowerCase();
          return t.includes(q) || d.includes(q) || desc.includes(q);
        });
      }

      items.sort((a, b) => new Date(b.created_at) - new Date(a.created_at));

      const total = items.length;
      const offset = (pageNum - 1) * limitNum;
      const paged = items.slice(offset, offset + limitNum);

      return sendSuccess(res, 'Listings retrieved successfully', {
        listings: paged,
        total,
        page: pageNum,
        limit: limitNum,
      });
    } catch (err) {
      return sendError(res, 'Failed to fetch listings: ' + err.message, null, 500);
    }
  },

  /**
   * Get listing by ID with vendor isolation
   */
  getListingById: async (req, res) => {
    try {
      const { id } = req.params;
      const vId = Number(req.vendorId) || req.vendorId;

      const col = db.collection(COLLECTIONS.LISTINGS);
      let doc = await col.doc(String(id)).get();
      let listing = null;

      if (doc.exists) {
        listing = doc.data();
      } else {
        const snap = await col.where('id', '==', Number(id) || id).get();
        if (!snap.empty) listing = snap.docs[0].data();
      }

      if (!listing || (Number(listing.vendor_id) !== vId && String(listing.vendor_id) !== String(vId))) {
        return sendError(res, 'Listing not found or access denied', null, 404);
      }

      // Aggregate bookings stats for this listing
      const bSnap = await db.collection(COLLECTIONS.BOOKINGS).where('listing_id', '==', Number(listing.id) || listing.id).get();
      let totalBookings = 0;
      let completedBookings = 0;
      let acceptedBookings = 0;

      bSnap.forEach((bDoc) => {
        const b = bDoc.data();
        totalBookings++;
        if (b.status === 'completed') completedBookings++;
        if (b.status === 'accepted') acceptedBookings++;
      });

      return sendSuccess(res, 'Listing retrieved successfully', {
        ...listing,
        stats: {
          total_bookings: totalBookings,
          completed_bookings: completedBookings,
          accepted_bookings: acceptedBookings,
        },
      });
    } catch (err) {
      return sendError(res, 'Failed to fetch listing: ' + err.message, null, 500);
    }
  },

  /**
   * Create new listing
   */
  createListing: async (req, res) => {
    try {
      const vId = Number(req.vendorId) || req.vendorId;
      const { title, description, destination, price, duration, max_travellers, status = 'active' } = req.body;

      if (!title || !destination || price === undefined || !duration) {
        return sendError(res, 'Title, destination, price, and duration are required', null, 422);
      }

      const numPrice = parseFloat(price);
      if (isNaN(numPrice) || numPrice < 0) {
        return sendError(res, 'Price must be a valid positive number', null, 422);
      }

      const numTravellers = parseInt(max_travellers || '10', 10);
      if (isNaN(numTravellers) || numTravellers <= 0) {
        return sendError(res, 'max_travellers must be a positive integer', null, 422);
      }

      const col = db.collection(COLLECTIONS.LISTINGS);
      const allSnap = await col.get();
      let maxId = 0;
      allSnap.forEach((d) => {
        const nid = Number(d.data().id || d.id);
        if (!isNaN(nid) && nid > maxId) maxId = nid;
      });
      const listingId = maxId + 1;

      const newListing = {
        id: listingId,
        vendor_id: vId,
        title: title.trim(),
        description: description ? description.trim() : '',
        destination: destination.trim(),
        price: numPrice,
        duration: duration.trim(),
        max_travellers: numTravellers,
        status: status === 'inactive' ? 'inactive' : 'active',
        created_at: new Date().toISOString(),
        updated_at: new Date().toISOString(),
      };

      await col.doc(String(listingId)).set(newListing);

      await auditService.logAction({
        actorType: 'vendor',
        actorId: vId,
        action: 'Listing Created',
        entityType: 'listing',
        entityId: listingId,
        details: { title: newListing.title, price: newListing.price },
        ipAddress: req.ip,
      });

      try {
        await db.collection(COLLECTIONS.APP_ACTIVITIES).add({
          app_source: 'Vendor Mobile App',
          action: 'Tour Package Created',
          details: `Vendor VND-${vId} published "${newListing.title}" for ₹${newListing.price}`,
          timestamp: new Date().toISOString(),
        });
      } catch (_) {}

      return sendSuccess(res, 'Listing created successfully', newListing, 201);
    } catch (err) {
      return sendError(res, 'Failed to create listing: ' + err.message, null, 500);
    }
  },

  /**
   * Update listing with vendor isolation
   */
  updateListing: async (req, res) => {
    try {
      const { id } = req.params;
      const vId = Number(req.vendorId) || req.vendorId;

      const col = db.collection(COLLECTIONS.LISTINGS);
      let docRef = col.doc(String(id));
      let doc = await docRef.get();

      if (!doc.exists) {
        const snap = await col.where('id', '==', Number(id) || id).get();
        if (!snap.empty) {
          docRef = snap.docs[0].ref;
          doc = snap.docs[0];
        }
      }

      if (!doc.exists) {
        return sendError(res, 'Listing not found or unauthorized', null, 404);
      }

      const existing = doc.data();
      if (Number(existing.vendor_id) !== vId && String(existing.vendor_id) !== String(vId)) {
        return sendError(res, 'Listing not found or unauthorized', null, 404);
      }

      const { title, description, destination, price, duration, max_travellers, status } = req.body;

      const updatedTitle = title !== undefined ? title.trim() : existing.title;
      const updatedDesc = description !== undefined ? description.trim() : existing.description;
      const updatedDest = destination !== undefined ? destination.trim() : existing.destination;
      const updatedPrice = price !== undefined ? parseFloat(price) : existing.price;
      const updatedDuration = duration !== undefined ? duration.trim() : existing.duration;
      const updatedMaxTravellers = max_travellers !== undefined ? parseInt(max_travellers, 10) : existing.max_travellers;
      const updatedStatus = status !== undefined ? status : existing.status;

      if (isNaN(updatedPrice) || updatedPrice < 0) {
        return sendError(res, 'Price must be a valid non-negative number', null, 422);
      }

      if (isNaN(updatedMaxTravellers) || updatedMaxTravellers <= 0) {
        return sendError(res, 'max_travellers must be a positive integer', null, 422);
      }

      const updates = {
        title: updatedTitle,
        description: updatedDesc,
        destination: updatedDest,
        price: updatedPrice,
        duration: updatedDuration,
        max_travellers: updatedMaxTravellers,
        status: updatedStatus,
        updated_at: new Date().toISOString(),
      };

      await docRef.set(updates, { merge: true });
      const finalDoc = await docRef.get();

      await auditService.logAction({
        actorType: 'vendor',
        actorId: vId,
        action: 'Listing Updated',
        entityType: 'listing',
        entityId: Number(id) || id,
        details: { title: updatedTitle },
        ipAddress: req.ip,
      });

      return sendSuccess(res, 'Listing updated successfully', finalDoc.data());
    } catch (err) {
      return sendError(res, 'Failed to update listing: ' + err.message, null, 500);
    }
  },

  /**
   * Delete listing with vendor isolation
   */
  deleteListing: async (req, res) => {
    try {
      const { id } = req.params;
      const vId = Number(req.vendorId) || req.vendorId;

      const col = db.collection(COLLECTIONS.LISTINGS);
      let docRef = col.doc(String(id));
      let doc = await docRef.get();

      if (!doc.exists) {
        const snap = await col.where('id', '==', Number(id) || id).get();
        if (!snap.empty) {
          docRef = snap.docs[0].ref;
          doc = snap.docs[0];
        }
      }

      if (!doc.exists) {
        return sendError(res, 'Listing not found or unauthorized', null, 404);
      }

      const existing = doc.data();
      if (Number(existing.vendor_id) !== vId && String(existing.vendor_id) !== String(vId)) {
        return sendError(res, 'Listing not found or unauthorized', null, 404);
      }

      await docRef.delete();

      await auditService.logAction({
        actorType: 'vendor',
        actorId: vId,
        action: 'Listing Deleted',
        entityType: 'listing',
        entityId: Number(id) || id,
        ipAddress: req.ip,
      });

      return sendSuccess(res, 'Listing deleted successfully');
    } catch (err) {
      return sendError(res, 'Failed to delete listing: ' + err.message, null, 500);
    }
  },
};

module.exports = listingController;
