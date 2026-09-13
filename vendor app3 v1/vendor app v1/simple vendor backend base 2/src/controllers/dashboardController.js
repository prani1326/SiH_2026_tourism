const { db } = require('../config/firebase');
const { COLLECTIONS } = require('../database/firestoreSchema');
const auditService = require('../services/auditService');
const notificationService = require('../services/notificationService');

const dashboardController = {
  // 1. Central Overview Metrics
  getOverview: async (req, res) => {
    try {
      const travelersSnap = await db.collection(COLLECTIONS.TRAVELLERS).get();
      const vendorsSnap = await db.collection(COLLECTIONS.VENDORS).get();
      const leadersSnap = await db.collection(COLLECTIONS.LEADERS).get();
      const tripsSnap = await db.collection(COLLECTIONS.TRIPS).get();
      const bookingsSnap = await db.collection(COLLECTIONS.BOOKINGS).get();
      const ticketsSnap = await db.collection(COLLECTIONS.TICKETS).get();

      let activeTrips = 0;
      tripsSnap.forEach((d) => {
        const s = (d.data().status || '').toLowerCase();
        if (s === 'active') activeTrips++;
      });

      let openTickets = 0;
      ticketsSnap.forEach((d) => {
        const s = (d.data().status || '').toLowerCase();
        if (s === 'open' || s === 'in progress') openTickets++;
      });

      const actSnap = await db.collection(COLLECTIONS.APP_ACTIVITIES).get();
      const activities = actSnap.docs.map((d) => d.data());
      activities.sort((a, b) => new Date(b.timestamp || b.created_at) - new Date(a.timestamp || a.created_at));
      const recentActivity = activities.slice(0, 10);

      return res.json({
        totalTravelers: travelersSnap.size,
        totalVendors: vendorsSnap.size,
        totalLeaders: leadersSnap.size,
        totalTrips: tripsSnap.size,
        totalBookingRequests: bookingsSnap.size,
        activeTrips,
        openTickets,
        recentActivity: recentActivity || [],
      });
    } catch (err) {
      return res.status(500).json({ error: err.message });
    }
  },

  // 2. Traveler Data
  getTravelers: async (req, res) => {
    try {
      const tSnap = await db.collection(COLLECTIONS.TRAVELLERS).get();
      const travelers = await Promise.all(
        tSnap.docs.map(async (doc) => {
          const t = doc.data();
          const tid = Number(t.id) || t.id;
          const bSnap = await db.collection(COLLECTIONS.BOOKINGS).where('traveller_id', '==', tid).get();
          const bookingCount = bSnap.size;

          let tripsCount = 0;
          for (const bDoc of bSnap.docs) {
            const trSnap = await db.collection(COLLECTIONS.TRIPS).where('booking_id', '==', Number(bDoc.data().id) || bDoc.data().id).get();
            tripsCount += trSnap.size;
          }

          const padId = String(t.id).padStart(4, '0');
          return {
            id: t.id,
            traveler_id: `TRV-${padId}`,
            name: t.name,
            email: t.email,
            phone: t.mobile || t.phone || '',
            profile_info: 'Registered Traveler',
            booking_requests_count: bookingCount,
            trips_count: tripsCount,
            trip_status: 'Active',
            emergency_info: `Self / Mobile: ${t.mobile || t.phone || ''}`,
            created_at: t.created_at,
          };
        })
      );

      travelers.sort((a, b) => (Number(b.id) || 0) - (Number(a.id) || 0));
      return res.json(travelers);
    } catch (err) {
      return res.status(500).json({ error: err.message });
    }
  },

  // 3. Vendor Central Records
  getVendors: async (req, res) => {
    try {
      const vSnap = await db.collection(COLLECTIONS.VENDORS).get();
      const vendors = await Promise.all(
        vSnap.docs.map(async (doc) => {
          const v = doc.data();
          const vid = Number(v.id) || v.id;

          const kycSnap = await db.collection(COLLECTIONS.VENDOR_KYC).where('vendor_id', '==', vid).get();
          let kycData = null;
          if (!kycSnap.empty) kycData = kycSnap.docs[0].data();

          const wSnap = await db.collection(COLLECTIONS.WALLETS).where('vendor_id', '==', vid).get();
          let walletBalance = 0;
          if (!wSnap.empty) walletBalance = parseFloat(wSnap.docs[0].data().balance) || 0;

          const lSnap = await db.collection(COLLECTIONS.LISTINGS).where('vendor_id', '==', vid).get();
          const listingTitles = lSnap.docs.slice(0, 3).map((ld) => ld.data().title);

          const bSnap = await db.collection(COLLECTIONS.BOOKINGS).where('vendor_id', '==', vid).get();
          let acceptedCount = 0;
          let rejectedCount = 0;
          bSnap.forEach((bd) => {
            const s = bd.data().status;
            if (s === 'accepted') acceptedCount++;
            if (s === 'rejected') rejectedCount++;
          });

          const docSnap = await db.collection(COLLECTIONS.VENDOR_DOCUMENTS).where('vendor_id', '==', vid).get();

          const padId = String(v.id).padStart(4, '0');
          let verificationStatus = 'Pending';
          if (v.kyc_status === 'approved') verificationStatus = 'Verified';
          if (v.kyc_status === 'rejected') verificationStatus = 'Rejected';

          return {
            id: v.id,
            vendor_id: `VND-${padId}`,
            business_name: (kycData && kycData.business_name) ? kycData.business_name : v.name,
            owner_contact: `${v.name} (${v.mobile})`,
            email: v.email,
            mobile: v.mobile,
            kyc_status: v.kyc_status,
            verification_status: verificationStatus,
            kyc_info: `${(kycData && kycData.business_type) || 'Tour Operator'} | ${(kycData && kycData.city) || 'India'}`,
            listings: listingTitles.join(', '),
            booking_requests_count: bSnap.size,
            accepted_bookings_count: acceptedCount,
            rejected_bookings_count: rejectedCount,
            vendor_activity: v.verification_remarks || 'Vendor active on platform',
            wallet_balance: walletBalance,
            document_count: docSnap.size,
            created_at: v.created_at,
          };
        })
      );

      vendors.sort((a, b) => (Number(b.id) || 0) - (Number(a.id) || 0));
      return res.json(vendors);
    } catch (err) {
      return res.status(500).json({ error: err.message });
    }
  },

  // 3b. Single Vendor Details for Modal / Verification
  getVendorDetails: async (req, res) => {
    try {
      const { id } = req.params;
      const vid = Number(id) || id;

      const vCol = db.collection(COLLECTIONS.VENDORS);
      let vDoc = await vCol.doc(String(vid)).get();
      let vendor = null;

      if (vDoc.exists) {
        vendor = vDoc.data();
      } else {
        const snap = await vCol.where('id', '==', vid).get();
        if (!snap.empty) vendor = snap.docs[0].data();
      }

      if (!vendor) return res.status(404).json({ error: 'Vendor not found' });

      const kycSnap = await db.collection(COLLECTIONS.VENDOR_KYC).where('vendor_id', '==', vid).get();
      const kyc = !kycSnap.empty ? kycSnap.docs[0].data() : {};

      const wSnap = await db.collection(COLLECTIONS.WALLETS).where('vendor_id', '==', vid).get();
      const walletBalance = !wSnap.empty ? wSnap.docs[0].data().balance : 0;

      const docsSnap = await db.collection(COLLECTIONS.VENDOR_DOCUMENTS).where('vendor_id', '==', vid).get();
      const documents = docsSnap.docs.map((d) => d.data());

      const listSnap = await db.collection(COLLECTIONS.LISTINGS).where('vendor_id', '==', vid).get();
      const listings = listSnap.docs.map((d) => d.data());

      const bSnap = await db.collection(COLLECTIONS.BOOKINGS).where('vendor_id', '==', vid).get();
      const bookings = bSnap.docs.map((d) => d.data());
      bookings.sort((a, b) => (Number(b.id) || 0) - (Number(a.id) || 0));

      const mergedVendor = {
        ...vendor,
        ...kyc,
        wallet_balance: walletBalance,
      };

      return res.json({
        vendor: mergedVendor,
        documents,
        listings,
        bookings,
      });
    } catch (err) {
      return res.status(500).json({ error: err.message });
    }
  },

  // 3c. Direct KYC Verification from Web Dashboard
  verifyVendorKyc: async (req, res) => {
    try {
      const { id } = req.params;
      const vid = Number(id) || id;
      const { action, remarks, rejection_reason } = req.body;

      if (!action || !['approve', 'reject'].includes(action.toLowerCase())) {
        return res.status(422).json({ error: "Action must be 'approve' or 'reject'" });
      }

      const vCol = db.collection(COLLECTIONS.VENDORS);
      let vDocRef = vCol.doc(String(vid));
      let vDoc = await vDocRef.get();

      if (!vDoc.exists) {
        const snap = await vCol.where('id', '==', vid).get();
        if (!snap.empty) {
          vDocRef = snap.docs[0].ref;
          vDoc = snap.docs[0];
        }
      }

      if (!vDoc.exists) return res.status(404).json({ error: 'Vendor not found' });

      const vendor = vDoc.data();
      const isApprove = action.toLowerCase() === 'approve';
      const newStatus = isApprove ? 'approved' : 'rejected';
      const reason = isApprove ? null : (rejection_reason || 'Incomplete or unclear documentation submitted');
      const verificationRemarks = remarks || (isApprove ? 'KYC verified and approved by admin dashboard' : reason);

      await vDocRef.set({
        kyc_status: newStatus,
        status: 'active',
        rejection_reason: reason,
        verification_remarks: verificationRemarks,
        verified_at: new Date().toISOString(),
        updated_at: new Date().toISOString(),
      }, { merge: true });

      const kycSnap = await db.collection(COLLECTIONS.VENDOR_KYC).where('vendor_id', '==', vid).get();
      if (!kycSnap.empty) {
        await kycSnap.docs[0].ref.set({
          status: newStatus,
          updated_at: new Date().toISOString(),
        }, { merge: true });
      }

      await auditService.logAction({
        actorType: 'admin_dashboard',
        actorId: 1,
        action: isApprove ? 'KYC Approved' : 'KYC Rejected',
        entityType: 'vendor',
        entityId: vid,
        details: { vendor: vendor.name, action: newStatus, remarks: verificationRemarks },
        ipAddress: req.ip,
      });

      await notificationService.createNotification({
        recipientType: 'vendor',
        recipientId: vid,
        title: isApprove ? 'KYC Approved!' : 'KYC Application Rejected',
        message: isApprove
          ? 'Congratulations! Your KYC application has been verified and approved by Admin. You can now create tour listings and accept bookings.'
          : `Your KYC application was rejected. Reason: ${reason}. Please update your details and resubmit.`,
        type: isApprove ? 'kyc_approved' : 'kyc_rejected',
      });

      await db.collection(COLLECTIONS.APP_ACTIVITIES).add({
        app_source: 'Web Dashboard',
        action: isApprove ? 'Vendor KYC Approved' : 'Vendor KYC Rejected',
        details: `Admin ${isApprove ? 'approved' : 'rejected'} KYC for ${vendor.name} (VND-${vid})`,
        timestamp: new Date().toISOString(),
      });

      return res.json({
        success: true,
        message: `Vendor KYC has been ${newStatus} successfully`,
        vendor_id: id,
        kyc_status: newStatus,
      });
    } catch (err) {
      return res.status(500).json({ error: err.message });
    }
  },

  // 4. Leaders / Ops Staff
  getLeaders: async (req, res) => {
    try {
      const snap = await db.collection(COLLECTIONS.LEADERS).get();
      const leaders = snap.docs.map((d) => d.data());
      leaders.sort((a, b) => (Number(b.id) || 0) - (Number(a.id) || 0));
      return res.json(leaders);
    } catch (err) {
      return res.status(500).json({ error: err.message });
    }
  },

  // 5. Destinations / Content
  getDestinations: async (req, res) => {
    try {
      const snap = await db.collection(COLLECTIONS.DESTINATIONS).get();
      const destinations = snap.docs.map((d) => d.data());
      destinations.sort((a, b) => (Number(b.id) || 0) - (Number(a.id) || 0));
      return res.json(destinations);
    } catch (err) {
      return res.status(500).json({ error: err.message });
    }
  },

  // 6. Bookings Data
  getBookings: async (req, res) => {
    try {
      const bSnap = await db.collection(COLLECTIONS.BOOKINGS).get();
      const bookings = await Promise.all(
        bSnap.docs.map(async (doc) => {
          const b = doc.data();
          let traveler_name = 'Traveler';
          let vendor_name = 'Vendor';
          let service = 'Tour Package';
          let trip_name = `TRP-${String(b.id).padStart(4, '0')}`;

          if (b.traveller_id) {
            const tDoc = await db.collection(COLLECTIONS.TRAVELLERS).doc(String(b.traveller_id)).get();
            if (tDoc.exists) traveler_name = tDoc.data().name;
          }

          if (b.vendor_id) {
            const vDoc = await db.collection(COLLECTIONS.VENDORS).doc(String(b.vendor_id)).get();
            if (vDoc.exists) {
              const v = vDoc.data();
              const kycSnap = await db.collection(COLLECTIONS.VENDOR_KYC).where('vendor_id', '==', Number(v.id) || v.id).get();
              if (!kycSnap.empty && kycSnap.docs[0].data().business_name) {
                vendor_name = kycSnap.docs[0].data().business_name;
              } else {
                vendor_name = v.name;
              }
            }
          }

          if (b.listing_id) {
            const lDoc = await db.collection(COLLECTIONS.LISTINGS).doc(String(b.listing_id)).get();
            if (lDoc.exists) service = lDoc.data().title;
          }

          const trSnap = await db.collection(COLLECTIONS.TRIPS).where('booking_id', '==', Number(b.id) || b.id).get();
          if (!trSnap.empty) {
            trip_name = `TRP-${String(trSnap.docs[0].data().id).padStart(4, '0')}`;
          }

          const statusFormatted = b.status ? (b.status.charAt(0).toUpperCase() + b.status.slice(1)) : 'Pending';

          return {
            id: b.id,
            booking_id: `BKG-${String(b.id).padStart(4, '0')}`,
            traveler_name,
            vendor_name,
            trip_name,
            service,
            booking_date: b.booking_date,
            amount: b.amount,
            status: statusFormatted,
            created_at: b.created_at,
          };
        })
      );

      bookings.sort((a, b) => (Number(b.id) || 0) - (Number(a.id) || 0));
      return res.json(bookings);
    } catch (err) {
      return res.status(500).json({ error: err.message });
    }
  },

  // 7. Trips Data & Location Trail
  getTrips: async (req, res) => {
    try {
      const trSnap = await db.collection(COLLECTIONS.TRIPS).get();
      const trips = await Promise.all(
        trSnap.docs.map(async (doc) => {
          const tr = doc.data();
          let traveler_name = 'Traveler';
          let destination = 'India';
          let start_date = tr.start_time;
          let end_date = tr.end_time;
          let number_of_travelers = 1;
          let budget = 0;
          let itinerary = 'Trip Package';

          if (tr.booking_id) {
            const bDoc = await db.collection(COLLECTIONS.BOOKINGS).doc(String(tr.booking_id)).get();
            if (bDoc.exists) {
              const b = bDoc.data();
              budget = b.amount;
              number_of_travelers = b.traveller_count;
              if (!start_date) start_date = b.booking_date;
              if (!end_date) end_date = b.booking_date;

              if (b.traveller_id) {
                const tDoc = await db.collection(COLLECTIONS.TRAVELLERS).doc(String(b.traveller_id)).get();
                if (tDoc.exists) traveler_name = tDoc.data().name;
              }

              if (b.listing_id) {
                const lDoc = await db.collection(COLLECTIONS.LISTINGS).doc(String(b.listing_id)).get();
                if (lDoc.exists) {
                  const l = lDoc.data();
                  destination = l.destination;
                  itinerary = `${l.title} (${l.duration})`;
                }
              }
            }
          }

          const locSnap = await db.collection(COLLECTIONS.TRIP_LOCATIONS).where('trip_id', '==', Number(tr.id) || tr.id).get();
          const locs = locSnap.docs.map((d) => d.data());
          locs.sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp));
          const gpsTrail = locs.slice(0, 3).map((l) => `${l.latitude},${l.longitude}`).join(' | ');

          let statusFormatted = 'Planned';
          if (tr.status === 'active') statusFormatted = 'Active';
          if (tr.status === 'completed') statusFormatted = 'Completed';

          return {
            id: tr.id,
            trip_id: `TRP-${String(tr.id).padStart(4, '0')}`,
            traveler_name,
            destination,
            start_date,
            end_date,
            number_of_travelers,
            budget,
            itinerary,
            status: statusFormatted,
            gps_trail: gpsTrail,
            created_at: tr.created_at,
          };
        })
      );

      trips.sort((a, b) => (Number(b.id) || 0) - (Number(a.id) || 0));
      return res.json(trips);
    } catch (err) {
      return res.status(500).json({ error: err.message });
    }
  },

  // 8. Tickets / Incidents
  getTickets: async (req, res) => {
    try {
      const snap = await db.collection(COLLECTIONS.TICKETS).get();
      const tickets = snap.docs.map((d) => d.data());
      tickets.sort((a, b) => (Number(b.id) || 0) - (Number(a.id) || 0));
      return res.json(tickets);
    } catch (err) {
      return res.status(500).json({ error: err.message });
    }
  },

  // 9. Safety / Emergency Data
  getSafety: async (req, res) => {
    try {
      const snap = await db.collection(COLLECTIONS.SAFETY_EVENTS).get();
      const safety = snap.docs.map((d) => d.data());
      safety.sort((a, b) => (Number(b.id) || 0) - (Number(a.id) || 0));
      return res.json(safety);
    } catch (err) {
      return res.status(500).json({ error: err.message });
    }
  },

  // 10. Platform Analytics
  getAnalytics: async (req, res) => {
    try {
      const tSnap = await db.collection(COLLECTIONS.TRAVELLERS).get();
      const vSnap = await db.collection(COLLECTIONS.VENDORS).get();
      const bSnap = await db.collection(COLLECTIONS.BOOKINGS).get();
      const trSnap = await db.collection(COLLECTIONS.TRIPS).get();
      const sSnap = await db.collection(COLLECTIONS.SAFETY_EVENTS).get();
      const lSnap = await db.collection(COLLECTIONS.LISTINGS).get();

      let verifiedVendors = 0;
      let pendingVendors = 0;
      vSnap.forEach((d) => {
        const s = d.data().kyc_status;
        if (s === 'approved') verifiedVendors++;
        if (s === 'pending') pendingVendors++;
      });

      let completedBookings = 0;
      let acceptedBookings = 0;
      let pendingBookings = 0;
      let rejectedBookings = 0;
      bSnap.forEach((d) => {
        const s = d.data().status;
        if (s === 'completed') completedBookings++;
        if (s === 'accepted') acceptedBookings++;
        if (s === 'pending') pendingBookings++;
        if (s === 'rejected') rejectedBookings++;
      });

      let activeTrips = 0;
      let plannedTrips = 0;
      let completedTrips = 0;
      trSnap.forEach((d) => {
        const s = d.data().status;
        if (s === 'active') activeTrips++;
        if (s === 'upcoming') plannedTrips++;
        if (s === 'completed') completedTrips++;
      });

      let activeIncidents = 0;
      let resolvedIncidents = 0;
      sSnap.forEach((d) => {
        const s = d.data().resolution_status;
        if (s === 'Resolved') resolvedIncidents++;
        else activeIncidents++;
      });

      // Popular destinations
      const destCountMap = {};
      bSnap.forEach((bDoc) => {
        const b = bDoc.data();
        if (b.listing_id) {
          const lDoc = lSnap.docs.find((ld) => String(ld.data().id) === String(b.listing_id));
          if (lDoc) {
            const dest = lDoc.data().destination;
            destCountMap[dest] = (destCountMap[dest] || 0) + 1;
          }
        }
      });

      const popularDestinations = Object.keys(destCountMap).map((k) => ({
        destination: k,
        trip_count: destCountMap[k],
      }));
      popularDestinations.sort((a, b) => b.trip_count - a.trip_count);

      // Vendor performance
      const vendorPerformance = await Promise.all(
        vSnap.docs.map(async (vDoc) => {
          const v = vDoc.data();
          const vid = Number(v.id) || v.id;

          const kycSnap = await db.collection(COLLECTIONS.VENDOR_KYC).where('vendor_id', '==', vid).get();
          let bName = v.name;
          if (!kycSnap.empty && kycSnap.docs[0].data().business_name) {
            bName = kycSnap.docs[0].data().business_name;
          }

          let requestsCount = 0;
          let acceptedCount = 0;
          let rejectedCount = 0;

          bSnap.forEach((bd) => {
            const b = bd.data();
            if (Number(b.vendor_id) === vid || String(b.vendor_id) === String(vid)) {
              requestsCount++;
              if (b.status === 'accepted') acceptedCount++;
              if (b.status === 'rejected') rejectedCount++;
            }
          });

          return {
            business_name: bName,
            booking_requests_count: requestsCount,
            accepted_bookings_count: acceptedCount,
            rejected_bookings_count: rejectedCount,
            verification_status: v.kyc_status === 'approved' ? 'Verified' : 'Pending',
          };
        })
      );

      vendorPerformance.sort((a, b) => b.accepted_bookings_count - a.accepted_bookings_count);

      return res.json({
        usersGrowth: { totalTravelers: tSnap.size, totalVendors: vSnap.size },
        vendorsOnboarded: { totalVendors: vSnap.size, verifiedVendors, pendingVendors },
        bookingStats: {
          totalBookings: bSnap.size,
          completedBookings,
          acceptedBookings,
          pendingBookings,
          rejectedBookings,
        },
        tripStats: { activeTrips, plannedTrips, completedTrips },
        incidentsStats: { totalIncidents: sSnap.size, activeIncidents, resolvedIncidents },
        popularDestinations: popularDestinations || [],
        vendorPerformance: vendorPerformance || [],
      });
    } catch (err) {
      return res.status(500).json({ error: err.message });
    }
  },

  // Ingestion Simulation Endpoints
  ingestTraveler: async (req, res) => {
    try {
      const { name, email, phone, action, destination } = req.body;
      const tCol = db.collection(COLLECTIONS.TRAVELLERS);
      const allT = await tCol.get();
      const nextId = allT.size + 1;

      const newTraveler = {
        id: nextId,
        name: name || 'New App Traveler',
        email: email || `traveler_${Date.now()}@app.com`,
        mobile: phone || '+91 9000000000',
        created_at: new Date().toISOString(),
      };

      await tCol.doc(String(nextId)).set(newTraveler);

      await db.collection(COLLECTIONS.APP_ACTIVITIES).add({
        app_source: 'Traveler App',
        action: action || 'New Traveler Registration',
        details: `${name || 'Traveler'} signed up via Traveler App`,
        timestamp: new Date().toISOString(),
      });

      return res.status(201).json({
        success: true,
        traveler_id: `TRV-${nextId}`,
        message: 'Traveler collected into Central Database',
      });
    } catch (err) {
      return res.status(500).json({ error: err.message });
    }
  },

  ingestVendor: async (req, res) => {
    try {
      const { business_name, owner_contact, kyc_info, listings, action } = req.body;
      const vCol = db.collection(COLLECTIONS.VENDORS);
      const allV = await vCol.get();
      const nextId = allV.size + 1;

      const newVendor = {
        id: nextId,
        name: business_name || 'New Vendor Business',
        email: `vendor_${Date.now()}@app.com`,
        mobile: owner_contact || `9${Math.floor(100000000 + Math.random() * 900000000)}`,
        password_hash: '$2a$10$dummyhash',
        status: 'active',
        kyc_status: 'pending',
        created_at: new Date().toISOString(),
        updated_at: new Date().toISOString(),
      };

      await vCol.doc(String(nextId)).set(newVendor);

      await db.collection(COLLECTIONS.VENDOR_KYC).doc(String(nextId)).set({
        id: nextId,
        vendor_id: nextId,
        full_name: business_name || 'Vendor Owner',
        business_name: business_name || 'New Vendor Business',
        business_type: 'Tour Operator',
        status: 'pending',
        submitted_at: new Date().toISOString(),
        updated_at: new Date().toISOString(),
      });

      await db.collection(COLLECTIONS.APP_ACTIVITIES).add({
        app_source: 'Vendor App',
        action: action || 'Vendor App Onboarding',
        details: `${business_name || 'Vendor'} submitted KYC & onboarding data`,
        timestamp: new Date().toISOString(),
      });

      return res.status(201).json({
        success: true,
        vendor_id: `VND-${nextId}`,
        message: 'Vendor data collected into Central Database',
      });
    } catch (err) {
      return res.status(500).json({ error: err.message });
    }
  },

  ingestLeader: async (req, res) => {
    try {
      const { action, leader_name, issue_resolved, incident_created } = req.body;
      await db.collection(COLLECTIONS.APP_ACTIVITIES).add({
        app_source: 'Leader/Ops App',
        action: action || 'Ops Update',
        details: `${leader_name || 'Leader'}: ${issue_resolved || incident_created || 'Logged operation detail'}`,
        timestamp: new Date().toISOString(),
      });

      return res.status(201).json({
        success: true,
        message: 'Leader/Ops data logged into Central Database',
      });
    } catch (err) {
      return res.status(500).json({ error: err.message });
    }
  },
};

module.exports = dashboardController;
