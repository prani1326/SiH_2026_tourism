const { db } = require('../config/firebase');
const { COLLECTIONS } = require('../database/firestoreSchema');
const logger = require('../utils/logger');

const walletService = {
  getOrCreateWallet: async (vendorId) => {
    const vId = Number(vendorId) || vendorId;
    const col = db.collection(COLLECTIONS.WALLETS);
    const snap = await col.where('vendor_id', '==', vId).get();

    if (!snap.empty) {
      const doc = snap.docs[0];
      return { id: doc.id, ...doc.data() };
    }

    // Create new wallet
    const newWallet = {
      id: vId,
      vendor_id: vId,
      balance: 0.00,
      created_at: new Date().toISOString(),
      updated_at: new Date().toISOString(),
    };

    await col.doc(String(vId)).set(newWallet);
    return newWallet;
  },

  addTransaction: async (vendorId, type, amount, description = '', bookingId = null) => {
    const vId = Number(vendorId) || vendorId;
    const numAmount = parseFloat(amount) || 0;
    const bId = bookingId ? (Number(bookingId) || bookingId) : null;

    return await db.runTransaction(async (transaction) => {
      const walletCol = db.collection(COLLECTIONS.WALLETS);
      const walletSnap = await walletCol.where('vendor_id', '==', vId).get();

      let currentBalance = 0;
      let walletDocRef;

      if (walletSnap.empty) {
        walletDocRef = walletCol.doc(String(vId));
        currentBalance = 0;
      } else {
        const walletDoc = walletSnap.docs[0];
        walletDocRef = walletCol.doc(walletDoc.id);
        currentBalance = parseFloat(walletDoc.data().balance) || 0;
      }

      let balanceChange = 0;
      if (type === 'earning' || type === 'adjustment') {
        balanceChange = numAmount;
      } else if (type === 'withdrawal' || type === 'refund') {
        balanceChange = -numAmount;
      }

      const newBalance = currentBalance + balanceChange;

      await walletDocRef.set({
        id: vId,
        vendor_id: vId,
        balance: newBalance,
        updated_at: new Date().toISOString(),
      }, { merge: true });

      const txCol = db.collection(COLLECTIONS.TRANSACTIONS);
      const txSnap = await txCol.get();
      const nextTxId = txSnap.size + 1;

      const newTx = {
        id: nextTxId,
        vendor_id: vId,
        type,
        amount: numAmount,
        description,
        booking_id: bId,
        created_at: new Date().toISOString(),
      };

      await txCol.doc(String(nextTxId)).set(newTx);
      return newTx;
    });
  },

  getSummary: async (vendorId) => {
    const vId = Number(vendorId) || vendorId;
    const wallet = await walletService.getOrCreateWallet(vId);

    const txSnap = await db.collection(COLLECTIONS.TRANSACTIONS).where('vendor_id', '==', vId).get();
    let totalEarned = 0;
    let withdrawnAmount = 0;

    txSnap.forEach((doc) => {
      const data = doc.data();
      const amt = parseFloat(data.amount) || 0;
      if (data.type === 'earning') totalEarned += amt;
      if (data.type === 'withdrawal') withdrawnAmount += amt;
    });

    const bookingsSnap = await db.collection(COLLECTIONS.BOOKINGS).where('vendor_id', '==', vId).get();
    let pendingAmount = 0;
    bookingsSnap.forEach((doc) => {
      const data = doc.data();
      if (data.status === 'pending' || data.status === 'accepted') {
        pendingAmount += parseFloat(data.amount) || 0;
      }
    });

    return {
      wallet_id: wallet.id,
      vendor_id: wallet.vendor_id,
      balance: parseFloat(wallet.balance) || 0,
      available_balance: parseFloat(wallet.balance) || 0,
      total_earned: totalEarned,
      pending_amount: pendingAmount,
      withdrawn_amount: withdrawnAmount,
      updated_at: wallet.updated_at,
    };
  },

  getTransactions: async (vendorId, { page = 1, limit = 20, type = null } = {}) => {
    const vId = Number(vendorId) || vendorId;
    const pageNum = parseInt(page, 10) || 1;
    const limitNum = parseInt(limit, 10) || 20;

    let query = db.collection(COLLECTIONS.TRANSACTIONS).where('vendor_id', '==', vId);
    if (type) {
      query = query.where('type', '==', type);
    }

    const allTxSnap = await query.get();
    const total = allTxSnap.size;

    const sortedDocs = allTxSnap.docs.map((d) => d.data());
    sortedDocs.sort((a, b) => new Date(b.created_at) - new Date(a.created_at));

    const offset = (pageNum - 1) * limitNum;
    const pagedDocs = sortedDocs.slice(offset, offset + limitNum);

    // Join with booking and listing info
    const enriched = await Promise.all(
      pagedDocs.map(async (tx) => {
        let booking_date = null;
        let booking_amount = null;
        let listing_title = null;

        if (tx.booking_id) {
          const bDoc = await db.collection(COLLECTIONS.BOOKINGS).doc(String(tx.booking_id)).get();
          if (bDoc.exists) {
            const bData = bDoc.data();
            booking_date = bData.booking_date;
            booking_amount = bData.amount;
            if (bData.listing_id) {
              const lDoc = await db.collection(COLLECTIONS.LISTINGS).doc(String(bData.listing_id)).get();
              if (lDoc.exists) listing_title = lDoc.data().title;
            }
          }
        }

        return {
          ...tx,
          booking_date,
          booking_amount,
          listing_title,
        };
      })
    );

    return {
      transactions: enriched,
      total,
      page: pageNum,
      limit: limitNum,
    };
  },

  getTransactionById: async (vendorId, transactionId) => {
    const vId = Number(vendorId) || vendorId;
    const tDoc = await db.collection(COLLECTIONS.TRANSACTIONS).doc(String(transactionId)).get();
    if (!tDoc.exists) return null;

    const tx = tDoc.data();
    if (Number(tx.vendor_id) !== vId && String(tx.vendor_id) !== String(vId)) {
      return null;
    }

    let booking_date = null;
    let booking_amount = null;
    let listing_title = null;

    if (tx.booking_id) {
      const bDoc = await db.collection(COLLECTIONS.BOOKINGS).doc(String(tx.booking_id)).get();
      if (bDoc.exists) {
        const bData = bDoc.data();
        booking_date = bData.booking_date;
        booking_amount = bData.amount;
        if (bData.listing_id) {
          const lDoc = await db.collection(COLLECTIONS.LISTINGS).doc(String(bData.listing_id)).get();
          if (lDoc.exists) listing_title = lDoc.data().title;
        }
      }
    }

    return {
      ...tx,
      booking_date,
      booking_amount,
      listing_title,
    };
  },
};

module.exports = walletService;
