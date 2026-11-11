\# Handoff: Android presenter remote for PowerPoint (Windows)



\## Goal

Build an Android app (APK) that remotely controls a PowerPoint slide show running on a Windows PC over the local Wi-Fi network. Deliver two parts: a Windows companion app (Python) and the Android app (Kotlin). Build everything locally.



\## Why this architecture

Bluetooth HID was rejected because it can only send keystrokes. It cannot read the current slide or the speaker notes. Wi-Fi with a PC companion that drives PowerPoint through COM can do everything required.



\## Required features

1\. Next slide, previous slide

2\. Start show, stop show

3\. Blank (black) screen toggle

4\. Slide counter ("3 / 24"), updated live when the slide changes on the PC

5\. Speaker notes for the current slide, shown on the phone, updated live

6\. Timer on the phone: start, pause, reset (phone-side only)

7\. Trackpad mode: finger movement moves the Windows mouse cursor, tap = left click

8\. Laser pointer mode: while the finger is down, the companion holds Ctrl + left mouse button so PowerPoint shows its laser pointer; finger movement moves it; releasing the finger releases both



\## Components



\### 1. Windows companion (Python 3.11+)

\- Libraries: websockets, pywin32 (win32com), qrcode, and ctypes SendInput for mouse and keyboard (avoid heavy deps)

\- Runs a WebSocket server on 0.0.0.0, default port 8765

\- On start, prints and displays (console window is fine, small tkinter window is a bonus):

&#x20; - the PC's LAN IP

&#x20; - a 6-digit PIN, regenerated each run

&#x20; - a QR code encoding ws://<ip>:<port>?pin=<pin>

\- PowerPoint control via COM (win32com.client.Dispatch("PowerPoint.Application")):

&#x20; - start: Presentation.SlideShowSettings.Run()

&#x20; - stop: SlideShowWindows(1).View.Exit()

&#x20; - next/prev: SlideShowWindows(1).View.Next() / .Previous()

&#x20; - blank screen: toggle SlideShowWindows(1).View.State between running (1) and black screen (3)

&#x20; - slide index and total: View.CurrentShowPosition and Presentation.Slides.Count

&#x20; - notes: Slides(i).NotesPage.Shapes.Placeholders(2).TextFrame.TextRange.Text (handle missing placeholder gracefully)

\- Poll the slide position about every 250 ms, and push a state message to the phone only when it changes

\- Handle PowerPoint not running, no presentation open, or no show running: return a clear error state, never crash

\- Mouse: relative moves from phone deltas via SendInput, scaled by a sensitivity setting

\- Package as a single .exe with PyInstaller via build.ps1



\### 2. Android app (Kotlin, Jetpack Compose)

\- minSdk 26, targetSdk current stable

\- OkHttp WebSocket client

\- Connection screen: manual IP, port, PIN entry, and a QR scan button (CameraX + ML Kit barcode scanning). Remember the last host.

\- Three tabs:

&#x20; - Remote: big Next and Previous buttons, Start/Stop, Blank, slide counter, timer with start/pause/reset

&#x20; - Notes: current slide notes, scrollable, adjustable font size

&#x20; - Pointer: touch surface with a toggle between Trackpad and Laser modes

\- Keep screen on while connected

\- Volume up/down keys = previous/next while the Remote tab is open (nice to have)

\- Auto-reconnect with visible connection status

\- Cleartext ws:// is required on the LAN; scope it via a network security config rather than enabling it globally if feasible



\### 3. Protocol (JSON over WebSocket, one message per frame)

Phone to PC:

\- {"type":"auth","pin":"123456"}  (must be the first message; close the connection on failure)

\- {"type":"cmd","action":"next|prev|start|stop|blank"}

\- {"type":"move","dx":12,"dy":-3}

\- {"type":"click","button":"left|right"}

\- {"type":"laser","state":"down|up"}

PC to phone:

\- {"type":"auth","ok":true|false}

\- {"type":"state","slide":3,"total":24,"showRunning":true,"blank":false,"notes":"..."}

\- {"type":"error","message":"..."}



\### 4. Security requirements

\- PIN required before any command is accepted

\- Lock out a client IP for 60 seconds after 5 failed PIN attempts

\- Do not log PINs

\- Document the limitation: traffic is unencrypted on the LAN, so intended for trusted networks. Optional stretch goal: wss with a self-signed cert and certificate pinning from the QR code.

\- Document the Windows Firewall rule needed for the chosen port (provide the netsh command)



\## Repository layout

presenter-remote/

&#x20; companion/          Python source, requirements.txt, build.ps1 (PyInstaller)

&#x20; android/            Android Studio project

&#x20; README.md           setup, firewall, usage, troubleshooting



\## Build deliverables

\- Android project builds locally with ./gradlew assembleDebug

\- Final APK path: android/app/build/outputs/apk/debug/app-debug.apk

\- Companion runnable with python main.py and buildable to .exe via build.ps1

\- No CI and no GitHub Actions workflow



\## Acceptance criteria

\- Phone connects with QR or manual entry and a correct PIN; wrong PIN is rejected

\- Next/prev change slides within about 200 ms on the same Wi-Fi

\- Slide counter and notes update on the phone when the slide is changed on the PC

\- Blank toggles the black screen; start/stop work from a cold state

\- Trackpad moves the cursor; laser mode shows PowerPoint's laser pointer

\- Companion survives PowerPoint being closed and reopened

\- Connection drop and reconnect works without restarting either side



\## Out of scope

macOS/Linux, Google Slides, LibreOffice, iOS, cloud relay, internet access outside the LAN



\## Working instructions for the agent

\- Work in small, verifiable steps: companion first (testable with a simple WebSocket client), then the Android app

\- Write a test script that exercises the companion without a phone

\- Build and verify locally: run ./gradlew assembleDebug and fix all errors and warnings before reporting done

\- If the Android SDK or JDK is missing on the machine, say exactly what is needed and how to install it, rather than skipping the build

\- Flag anything you could not test (for example, PowerPoint COM behavior) in the README

\- Report the final APK path and the companion .exe path at the end

