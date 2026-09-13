const { execSync } = require('child_process');
const path = require('path');

const adb = path.join(process.env.LOCALAPPDATA, 'Android', 'Sdk', 'platform-tools', 'adb.exe');

function sleep(ms) {
  Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, ms);
}

// 1. Restart App to get fresh clean state
console.log('Restarting app...');
execSync(`"${adb}" shell am force-stop com.travellikepro.opsleader`);
sleep(500);
execSync(`"${adb}" shell am start -n com.travellikepro.opsleader/.MainActivity`);
sleep(2000);

// 2. Tap Email field
console.log('Tapping Email field (640, 1080)...');
execSync(`"${adb}" shell input tap 640 1080`);
sleep(500);

// 3. Type Email
console.log('Typing email...');
execSync(`"${adb}" shell input text 'priya.ops@leaderops.internal'`);
sleep(500);

// 4. Tap Password field
console.log('Tapping Password field (640, 1320)...');
execSync(`"${adb}" shell input tap 640 1320`);
sleep(500);

// 5. Type Password
console.log('Typing password...');
execSync(`"${adb}" shell input text 'Admin@123456'`);
sleep(500);

// 6. Dismiss keyboard
execSync(`"${adb}" shell input keyevent 111`);
sleep(500);

// 7. Tap Sign In button
console.log('Tapping Sign In button (640, 1550)...');
execSync(`"${adb}" shell input tap 640 1550`);
sleep(3000);

// 8. Capture Dashboard screenshot
console.log('Capturing Dashboard screenshot...');
execSync(`"${adb}" shell screencap -p /data/local/tmp/dashboard_live.png`);
execSync(`"${adb}" pull /data/local/tmp/dashboard_live.png C:\\Users\\97018\\.gemini\\antigravity-ide\\brain\\26c904ae-ca29-4725-9854-92c6801481b8\\dashboard_live.png`);
console.log('✓ Successfully captured dashboard screenshot!');
