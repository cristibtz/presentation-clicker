# Presenter Remote

Android app + Windows companion to control PowerPoint presentations over Wi-Fi.

## Features

- **Slide control** — next, previous, start/stop show, blank screen
- **Slide counter** — live "3 / 24" display, updates when slides change on PC
- **Speaker notes** — shows PowerPoint notes + your own editable notes per slide
- **Timer** — start, pause, reset
- **Trackpad mode** — move the Windows cursor from your phone
- **Laser pointer mode** — activates PowerPoint's laser pointer
- **QR code connection** — scan to connect, PIN-secured

## Setup

### Windows Companion

```
pip install -r companion/requirements.txt
python companion/main.py
```

Or build a standalone exe:

```powershell
companion/build.ps1
# Output: companion/dist/PresenterCompanion.exe
```

### Android App

```
cd android
./gradlew assembleDebug
# APK: android/app/build/outputs/apk/debug/app-debug.apk
```

Install via `adb install app-debug.apk` or transfer the file to your phone.

### Firewall

Allow the companion through Windows Firewall:

```
netsh advfirewall firewall add rule name="Presenter Remote" dir=in action=allow protocol=TCP localport=8765
```

## Usage

1. Run the companion on your PC — it shows IP, PIN, and a QR code
2. Open Presenter Remote on your phone
3. Scan the QR code or enter IP/port/PIN manually
4. Control your presentation from the Remote, Notes, or Pointer tabs

## Notes

- Both devices must be on the same Wi-Fi network
- Traffic is unencrypted (ws://) — use on trusted networks only
- The companion survives PowerPoint being closed and reopened
- Volume up/down on the phone maps to previous/next slide
