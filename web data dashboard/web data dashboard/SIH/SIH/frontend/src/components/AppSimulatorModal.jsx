import React, { useState } from 'react';
import { X, Send, Smartphone, Store, Shield, Compass, CreditCard } from 'lucide-react';
import { simulateAppIngest, createPaidTripRequest } from '../api/client';

export default function AppSimulatorModal({ onClose, onSuccess }) {
  const [appType, setAppType] = useState('traveler_trip'); // 'traveler_trip', 'traveler', 'vendor', 'leader'
  const [loading, setLoading] = useState(false);
  const [msg, setMsg] = useState(null);

  // Paid Trip Form states
  const [tripTraveler, setTripTraveler] = useState('Simran Malhotra');
  const [tripDest, setTripDest] = useState('Manali & Solang Valley');
  const [tripStartDate, setTripStartDate] = useState('2026-09-20');
  const [tripEndDate, setTripEndDate] = useState('2026-09-25');
  const [tripPax, setTripPax] = useState(2);
  const [tripBudget, setTripBudget] = useState(38000);
  const [tripItinerary, setTripItinerary] = useState('Day 1: Arrival & Riverside Camp, Day 2: Solang Paragliding, Day 3: Waterfall Hike');

  // Traveler Registration Form states
  const [travelerName, setTravelerName] = useState('Priya Nair');
  const [travelerEmail, setTravelerEmail] = useState('priya.nair@gmail.com');
  const [travelerPhone, setTravelerPhone] = useState('+91 9888877777');
  const [travelerProfile, setTravelerProfile] = useState('Solo Female Traveler');
  const [emergencyInfo, setEmergencyInfo] = useState('Contact: Father (+91 9777766666)');

  // Vendor Form states
  const [businessName, setBusinessName] = useState('Goa Kayak & Beach Stays');
  const [ownerContact, setOwnerContact] = useState('Rohan Dsouza (+91 9822114455)');
  const [kycInfo, setKycInfo] = useState('GSTIN: 30AABCK9988H1Z1, Tourism License Submitted');
  const [listings, setListings] = useState('Sunset Kayaking Tour, Beach Huts');

  // Leader Form states
  const [leaderName, setLeaderName] = useState('Captain Suresh Menon');
  const [opsAction, setOpsAction] = useState('Dispatched Local Guide to Manali Trail');

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setMsg(null);
    try {
      if (appType === 'traveler_trip') {
        const res = await createPaidTripRequest({
          traveler_name: tripTraveler,
          destination: tripDest,
          start_date: tripStartDate,
          end_date: tripEndDate,
          number_of_travelers: tripPax,
          budget: tripBudget,
          itinerary: tripItinerary
        });
        setMsg(`Success: Paid trip ${res.trip_id} created in TravelApp! Payment verified. Now waiting for Web Dashboard approval.`);
      } else {
        let payload = {};
        if (appType === 'traveler') {
          payload = {
            action: 'Traveler App Profile & Booking Request',
            name: travelerName,
            email: travelerEmail,
            phone: travelerPhone,
            profile_info: travelerProfile,
            emergency_info: emergencyInfo
          };
        } else if (appType === 'vendor') {
          payload = {
            action: 'Vendor App Onboarding & KYC Submission',
            business_name: businessName,
            owner_contact: ownerContact,
            kyc_info: kycInfo,
            listings: listings
          };
        } else {
          payload = {
            action: 'Leader/Ops App Incident Dispatch',
            leader_name: leaderName,
            issue_resolved: opsAction
          };
        }

        const res = await simulateAppIngest(appType, payload);
        setMsg(`Success: Data collected into Central DB! (${res.message})`);
      }

      setTimeout(() => {
        onSuccess();
        onClose();
      }, 1400);
    } catch (err) {
      setMsg(`Error: ${err.message}`);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal-card" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '580px' }}>
        <div className="modal-header">
          <h3 className="modal-title">Simulate App Data Flow (Traveler / Vendor / Leader Apps)</h3>
          <button className="btn btn-outline" style={{ padding: '4px 8px' }} onClick={onClose}>
            <X size={16} />
          </button>
        </div>

        <div style={{ display: 'flex', gap: '8px', marginBottom: '20px', flexWrap: 'wrap' }}>
          <button
            type="button"
            className={`btn ${appType === 'traveler_trip' ? 'btn-primary' : 'btn-outline'}`}
            onClick={() => setAppType('traveler_trip')}
            style={{ fontSize: '0.8rem', padding: '6px 12px' }}
          >
            <CreditCard size={14} />
            <span>💳 Paid Trip (Approval Flow)</span>
          </button>
          <button
            type="button"
            className={`btn ${appType === 'traveler' ? 'btn-primary' : 'btn-outline'}`}
            onClick={() => setAppType('traveler')}
            style={{ fontSize: '0.8rem', padding: '6px 12px' }}
          >
            <Smartphone size={14} />
            <span>Traveler Profile</span>
          </button>
          <button
            type="button"
            className={`btn ${appType === 'vendor' ? 'btn-primary' : 'btn-outline'}`}
            onClick={() => setAppType('vendor')}
            style={{ fontSize: '0.8rem', padding: '6px 12px' }}
          >
            <Store size={14} />
            <span>Vendor KYC</span>
          </button>
          <button
            type="button"
            className={`btn ${appType === 'leader' ? 'btn-primary' : 'btn-outline'}`}
            onClick={() => setAppType('leader')}
            style={{ fontSize: '0.8rem', padding: '6px 12px' }}
          >
            <Shield size={14} />
            <span>Leader Ops</span>
          </button>
        </div>

        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
          {appType === 'traveler_trip' && (
            <>
              <div style={{ backgroundColor: '#eff6ff', padding: '10px 14px', borderRadius: '8px', fontSize: '0.8rem', color: '#1e40af' }}>
                💡 <strong>Flow:</strong> Traveler selects destination & completes online payment. Trip is sent to <strong>Web Dashboard (Trip Approvals)</strong> awaiting admin clearance before dispatching to Leader App.
              </div>
              <div>
                <label className="field-label">Traveler Name</label>
                <input className="search-input" style={{ width: '100%' }} value={tripTraveler} onChange={(e) => setTripTraveler(e.target.value)} required />
              </div>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                <div>
                  <label className="field-label">Destination</label>
                  <select 
                    className="search-input" 
                    style={{ width: '100%' }} 
                    value={tripDest} 
                    onChange={(e) => setTripDest(e.target.value)}
                  >
                    <option value="Manali & Solang Valley">Manali & Solang Valley</option>
                    <option value="Goa (North & South)">Goa (North & South)</option>
                    <option value="Jaipur & Jaisalmer">Jaipur & Jaisalmer</option>
                    <option value="Munnar & Alleppey">Munnar & Alleppey</option>
                  </select>
                </div>
                <div>
                  <label className="field-label">Number of Travelers</label>
                  <input type="number" min="1" className="search-input" style={{ width: '100%' }} value={tripPax} onChange={(e) => setTripPax(e.target.value)} required />
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                <div>
                  <label className="field-label">Start Date</label>
                  <input type="date" className="search-input" style={{ width: '100%' }} value={tripStartDate} onChange={(e) => setTripStartDate(e.target.value)} required />
                </div>
                <div>
                  <label className="field-label">End Date</label>
                  <input type="date" className="search-input" style={{ width: '100%' }} value={tripEndDate} onChange={(e) => setTripEndDate(e.target.value)} required />
                </div>
              </div>

              <div>
                <label className="field-label">Amount Paid (₹ UPI / Card)</label>
                <input type="number" className="search-input" style={{ width: '100%' }} value={tripBudget} onChange={(e) => setTripBudget(e.target.value)} required />
              </div>

              <div>
                <label className="field-label">Trip Itinerary</label>
                <input className="search-input" style={{ width: '100%' }} value={tripItinerary} onChange={(e) => setTripItinerary(e.target.value)} required />
              </div>
            </>
          )}

          {appType === 'traveler' && (
            <>
              <div>
                <label className="field-label">Traveler Name</label>
                <input className="search-input" style={{ width: '100%' }} value={travelerName} onChange={(e) => setTravelerName(e.target.value)} required />
              </div>
              <div>
                <label className="field-label">Email</label>
                <input className="search-input" style={{ width: '100%' }} value={travelerEmail} onChange={(e) => setTravelerEmail(e.target.value)} required />
              </div>
              <div>
                <label className="field-label">Mobile</label>
                <input className="search-input" style={{ width: '100%' }} value={travelerPhone} onChange={(e) => setTravelerPhone(e.target.value)} required />
              </div>
              <div>
                <label className="field-label">Profile Information</label>
                <input className="search-input" style={{ width: '100%' }} value={travelerProfile} onChange={(e) => setTravelerProfile(e.target.value)} required />
              </div>
              <div>
                <label className="field-label">Emergency Information</label>
                <input className="search-input" style={{ width: '100%' }} value={emergencyInfo} onChange={(e) => setEmergencyInfo(e.target.value)} required />
              </div>
            </>
          )}

          {appType === 'vendor' && (
            <>
              <div>
                <label className="field-label">Business Name</label>
                <input className="search-input" style={{ width: '100%' }} value={businessName} onChange={(e) => setBusinessName(e.target.value)} required />
              </div>
              <div>
                <label className="field-label">Owner / Contact</label>
                <input className="search-input" style={{ width: '100%' }} value={ownerContact} onChange={(e) => setOwnerContact(e.target.value)} required />
              </div>
              <div>
                <label className="field-label">KYC Information</label>
                <input className="search-input" style={{ width: '100%' }} value={kycInfo} onChange={(e) => setKycInfo(e.target.value)} required />
              </div>
              <div>
                <label className="field-label">Listings</label>
                <input className="search-input" style={{ width: '100%' }} value={listings} onChange={(e) => setListings(e.target.value)} required />
              </div>
            </>
          )}

          {appType === 'leader' && (
            <>
              <div>
                <label className="field-label">Leader / Ops Name</label>
                <input className="search-input" style={{ width: '100%' }} value={leaderName} onChange={(e) => setLeaderName(e.target.value)} required />
              </div>
              <div>
                <label className="field-label">Ops Action / Incident Handled</label>
                <input className="search-input" style={{ width: '100%' }} value={opsAction} onChange={(e) => setOpsAction(e.target.value)} required />
              </div>
            </>
          )}

          {msg && <div style={{ fontSize: '0.85rem', color: msg.startsWith('Success') ? '#10b981' : '#ef4444', fontWeight: 600 }}>{msg}</div>}

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '10px' }}>
            <button type="button" className="btn btn-outline" onClick={onClose}>Cancel</button>
            <button type="submit" className="btn btn-primary" disabled={loading}>
              <Send size={14} />
              <span>{loading ? 'Submitting to Firestore...' : (appType === 'traveler_trip' ? 'Pay & Submit for Approval' : 'Transmit to Backend API')}</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
