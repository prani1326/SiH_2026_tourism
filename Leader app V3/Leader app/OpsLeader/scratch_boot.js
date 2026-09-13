const { execSync } = require('child_process');
const path = require('path');

const adb = path.join(process.env.LOCALAPPDATA, 'Android', 'Sdk', 'platform-tools', 'adb.exe');
console.log('Using ADB at:', adb);

let booted = false;
for (let i = 0; i < 40; i++) {
  try {
    const res = execSync(`"${adb}" shell getprop sys.boot_completed`, { encoding: 'utf8' }).trim();
    if (res === '1') {
      booted = true;
      console.log('✓ Emulator boot completed!');
      break;
    }
  } catch (e) {}
  console.log(`Waiting for boot... (${i + 1}/40)`);
  execSync('timeout /t 3 /nobreak > nul', { shell: 'cmd.exe' });
}

if (booted) {
  // Dismiss keyguard
  try {
    execSync(`"${adb}" shell wm dismiss-keyguard`);
  } catch (e) {}

  console.log('Installing latest app build...');
  execSync(`"${adb}" install -r app\\build\\outputs\\apk\\debug\\app-debug.apk`, { stdio: 'inherit' });

  console.log('Launching com.travellikepro.opsleader.MainActivity...');
  execSync(`"${adb}" shell am start -n com.travellikepro.opsleader/.MainActivity`, { stdio: 'inherit' });

  console.log('Capturing screenshot...');
  execSync(`timeout /t 3 /nobreak > nul`, { shell: 'cmd.exe' });
  execSync(`"${adb}" shell screencap -p /data/local/tmp/app_screen.png`);
  execSync(`"${adb}" pull /data/local/tmp/app_screen.png C:\\Users\\97018\\.gemini\\antigravity-ide\\brain\\26c904ae-ca29-4725-9854-92c6801481b8\\app_screen.png`);
  console.log('✓ Done!');
} else {
  console.log('Emulator boot timed out.');
}
