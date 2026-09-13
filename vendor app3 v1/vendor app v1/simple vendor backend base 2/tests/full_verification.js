async function testAll() {
  console.log('=== RUNNING COMPREHENSIVE SYSTEM VERIFICATION ===\n');
  const BASE = 'http://localhost:5000';
  let passed = 0;
  let total = 0;

  async function check(name, fn) {
    total++;
    try {
      await fn();
      console.log('✅ PASS: ' + name);
      passed++;
    } catch (err) {
      console.error('❌ FAIL: ' + name + ' -> ' + err.message);
    }
  }

  // 1. Health check
  await check('Backend Health API (/api/health)', async () => {
    const res = await fetch(BASE + '/api/health').then(r => r.json());
    if (!res.success) throw new Error('Health check unsuccessful');
  });

  // 2. Web Dashboard Endpoints
  await check('Web Dashboard Overview API (/api/overview)', async () => {
    const data = await fetch(BASE + '/api/overview').then(r => r.json());
    if (typeof data.totalVendors !== 'number') throw new Error('Invalid overview response');
  });

  await check('Web Dashboard Vendors API (/api/vendors)', async () => {
    const data = await fetch(BASE + '/api/vendors').then(r => r.json());
    if (!Array.isArray(data) || data.length === 0) throw new Error('Vendors list empty');
  });

  await check('Web Dashboard Travelers API (/api/travelers)', async () => {
    const data = await fetch(BASE + '/api/travelers').then(r => r.json());
    if (!Array.isArray(data) || data.length === 0) throw new Error('Travelers list empty');
  });

  await check('Web Dashboard Bookings API (/api/bookings)', async () => {
    const data = await fetch(BASE + '/api/bookings').then(r => r.json());
    if (!Array.isArray(data) || data.length === 0) throw new Error('Bookings list empty');
  });

  await check('Web Dashboard Trips API (/api/trips)', async () => {
    const data = await fetch(BASE + '/api/trips').then(r => r.json());
    if (!Array.isArray(data) || data.length === 0) throw new Error('Trips list empty');
  });

  await check('Web Dashboard Analytics API (/api/analytics)', async () => {
    const data = await fetch(BASE + '/api/analytics').then(r => r.json());
    if (!data.bookingStats || !data.usersGrowth) throw new Error('Invalid analytics response');
  });

  // 3. Vendor App APIs
  let vendorToken = '';
  await check('Vendor App Login API (/api/vendor/auth/login)', async () => {
    const res = await fetch(BASE + '/api/vendor/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ identifier: 'rahul@himalayanguides.com', password: 'vendor123' })
    }).then(r => r.json());
    if (!res.success || !res.data.token) throw new Error('Login failed');
    vendorToken = res.data.token;
  });

  await check('Vendor App Dashboard API (/api/vendor/dashboard)', async () => {
    const res = await fetch(BASE + '/api/vendor/dashboard', {
      headers: { 'Authorization': 'Bearer ' + vendorToken }
    }).then(r => r.json());
    if (!res.success || !res.data.vendor) throw new Error('Vendor dashboard failed');
  });

  await check('Vendor App Listings API (/api/vendor/listings)', async () => {
    const res = await fetch(BASE + '/api/vendor/listings', {
      headers: { 'Authorization': 'Bearer ' + vendorToken }
    }).then(r => r.json());
    if (!res.success) throw new Error('Listings fetch failed');
  });

  await check('Vendor App Bookings API (/api/vendor/bookings)', async () => {
    const res = await fetch(BASE + '/api/vendor/bookings', {
      headers: { 'Authorization': 'Bearer ' + vendorToken }
    }).then(r => r.json());
    if (!res.success) throw new Error('Bookings fetch failed');
  });

  await check('Vendor App Wallet API (/api/vendor/wallet)', async () => {
    const res = await fetch(BASE + '/api/vendor/wallet', {
      headers: { 'Authorization': 'Bearer ' + vendorToken }
    }).then(r => r.json());
    if (!res.success) throw new Error('Wallet fetch failed');
  });

  console.log('\n==================================================');
  console.log('TEST SUMMARY: ' + passed + '/' + total + ' Tests Passed (' + Math.round((passed/total)*100) + '%)');
  console.log('==================================================');
}

testAll();
