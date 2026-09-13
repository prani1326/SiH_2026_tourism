const fdb = require('./firestoreDb');
const { v4: uuidv4 } = require('uuid');
const { NotFoundError } = require('../utils/errors');
const { getPagination, getPaginationMeta } = require('../utils/pagination');

class PartnerService {
  async list(query) {
    const { page, limit, offset } = getPagination(query);
    let allPartners = await fdb.find('partners');

    if (query.type) {
      allPartners = allPartners.filter((p) => (p.type || p.category) === query.type);
    }
    if (query.verification_status) {
      allPartners = allPartners.filter((p) => (p.verification_status || p.status) === query.verification_status);
    }
    if (query.region) {
      allPartners = allPartners.filter((p) => p.region === query.region || p.city === query.region);
    }
    if (query.flagged) {
      allPartners = allPartners.filter((p) => p.flagged === 1 || p.flagged === true);
    }
    if (query.search) {
      const q = query.search.toLowerCase();
      allPartners = allPartners.filter((p) => p.name && p.name.toLowerCase().includes(q));
    }

    const total = allPartners.length;
    allPartners.sort((a, b) => ((b.created_at || '') > (a.created_at || '') ? 1 : -1));
    const data = allPartners.slice(offset, offset + limit);

    return { data, pagination: getPaginationMeta(page, limit, total) };
  }

  async getById(id) {
    const partner = await fdb.findById('partners', id);
    if (!partner) throw new NotFoundError('Partner');

    const listings = await fdb.find('listings', [['partner_id', '==', id]], {
      orderBy: ['created_at', 'desc'],
    });
    const complaints = await fdb.find('partner_complaints', [['partner_id', '==', id]], {
      orderBy: ['created_at', 'desc'],
      limit: 20,
    });

    return { ...partner, listings, recent_complaints: complaints };
  }

  async getListings(partnerId) {
    await this._getPartner(partnerId);
    return fdb.find('listings', [['partner_id', '==', partnerId]], {
      orderBy: ['created_at', 'desc'],
    });
  }

  async approvePartner(id, user, data = {}) {
    await this._getPartner(id);
    const now = new Date().toISOString();
    await fdb.update('partners', id, {
      verification_status: 'verified',
      status: 'verified',
      verified: 1,
      updated_at: now,
    });
    return { message: 'Partner approved' };
  }

  async rejectPartner(id, user, data = {}) {
    await this._getPartner(id);
    const now = new Date().toISOString();
    await fdb.update('partners', id, {
      verification_status: 'rejected',
      status: 'rejected',
      updated_at: now,
    });
    return { message: 'Partner rejected' };
  }

  async suspendPartner(id, user, data = {}) {
    await this._getPartner(id);
    const now = new Date().toISOString();
    await fdb.update('partners', id, {
      verification_status: 'suspended',
      status: 'suspended',
      updated_at: now,
    });
    return { message: 'Partner suspended' };
  }

  async getComplaints(partnerId) {
    await this._getPartner(partnerId);
    return fdb.find('partner_complaints', [['partner_id', '==', partnerId]], {
      orderBy: ['created_at', 'desc'],
    });
  }

  async flagPartner(id, user, reason) {
    await this._getPartner(id);
    const now = new Date().toISOString();
    await fdb.update('partners', id, {
      flagged: 1,
      flag_reason: reason,
      updated_at: now,
    });
    return { message: 'Partner flagged' };
  }

  async approveListing(listingId, user) {
    const listing = await fdb.findById('listings', listingId);
    if (!listing) throw new NotFoundError('Listing');
    const now = new Date().toISOString();
    await fdb.update('listings', listingId, {
      status: 'approved',
      reviewed_by: user.id,
      reviewed_at: now,
      updated_at: now,
    });
    return { message: 'Listing approved' };
  }

  async rejectListing(listingId, user, reason) {
    const listing = await fdb.findById('listings', listingId);
    if (!listing) throw new NotFoundError('Listing');
    const now = new Date().toISOString();
    await fdb.update('listings', listingId, {
      status: 'rejected',
      reviewed_by: user.id,
      rejection_reason: reason || 'Rejected',
      reviewed_at: now,
      updated_at: now,
    });
    return { message: 'Listing rejected' };
  }

  async suspendListing(listingId, user, reason) {
    const listing = await fdb.findById('listings', listingId);
    if (!listing) throw new NotFoundError('Listing');
    const now = new Date().toISOString();
    await fdb.update('listings', listingId, {
      status: 'suspended',
      suspension_reason: reason || 'Suspended',
      updated_at: now,
    });
    return { message: 'Listing suspended' };
  }

  async _getPartner(id) {
    const partner = await fdb.findById('partners', id);
    if (!partner) throw new NotFoundError('Partner');
    return partner;
  }
}

module.exports = new PartnerService();
