# CASPER · Android RF Research Console

**Version:** 0.1.0-alpha  
**Application ID:** `com.casper.lab`  
**UI:** monochrome, dark-green linear gradient, original user-supplied ghost launcher icon  
**Platforms:** Android 8+ native Kotlin app; separate browser UI preview

## What is implemented

- Native Android application with six screens: Home, RF Spectrum, HRV, 3D Lab, Devices, Logs.
- Android USB Host enumeration, explicit USB permission request, CDC-ACM-compatible bulk serial diagnostics, safe `help` command, and connection/detachment handling.
- Animated **synthetic** RF waveform: not actual spectrum scanning and not raw IQ capture.
- Offline RMSSD, SDNN, pNN50 and mean heart-rate calculations from user-entered, artifact-screened NN intervals.
- Interactive synthetic wireframe room and mathematical point cloud; rotate and zoom gestures.
- Local activity history, text export through Android share sheet and log clearing.
- No Internet permission. No GPS, camera, or microphone access. No account, cloud upload, or background scanning.
- Responsive HTML/CSS/JavaScript preview in `preview/` with working charts, HRV math and 3D demo.

**Critical distinction:** The Flipper USB bridge is *diagnostic*, not a radar scanner. No module tracks people, maps private building interiors, infers HRV from ordinary RF traffic, or claims the simulated points are real measurements. Radar, SDR IQ, and ECG sensors need separately verified hardware adapters and reference datasets.

## Android build

### Requirements

- Android Studio with Android SDK Platform 35 and matching SDK Build Tools
- JDK 17 (or a compatible newer JDK)
- Internet on the development computer to fetch Gradle 8.9, Android Gradle Plugin 8.7.3 and Kotlin plugin 2.0.21 the first time
- A USB-capable Android phone. USB Host support and cable capabilities vary.

### Windows / PowerShell

From the extracted project root, run:

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\build-debug.ps1
```

### Linux / macOS

```bash
bash tools/build-debug.sh
```

Or import the **root folder** into Android Studio, select Gradle 8.9 or generate a standard Gradle wrapper, sync and use **Build > Build APK(s)**. This archive deliberately does not claim to include Google's Android SDK or a compiled APK.

Expected output on a successful local build:

`app/build/outputs/apk/debug/app-debug.apk`

For first-time device installation you may transfer that debug APK via USB or use Android Studio's Run button. The debug APK is not a production-signed release.

## Browser preview (runnable today)

Open `preview/index.html` in a current browser. Chrome works when opened from local disk, though installing as a PWA/service-worker offline cache requires a hosted HTTPS or localhost origin.

Serve it locally with:

```bash
cd preview
python -m http.server 8080
```

Then open `http://localhost:8080` on the computer. To open it from a phone on the same network, use the computer's local address, with network settings/firewall configured by the owner. The browser preview intentionally **cannot** access Flipper USB diagnostics.

## Hardware architecture

```
Flipper Zero USB-C (CDC-ACM-compatible firmware / CLI)
        │ Android USB Host permission
        ▼
FlipperUsb.kt → diagnostic read/write → local logs
        │
        └───────── Native Home / Devices UI

Synthetic spectra  → SpectrumDemoView (label: SYNTHETIC)
Entered NN samples → HrvMath            (no clinical interpretation)
Synthetic 3D scene → RoomSceneView       (label: SIMULATION)
Future SDR/Radar   → [separate driver + validation; NOT IMPLEMENTED]
```

Not every Flipper firmware or USB mode offers a CDC-ACM-compatible interface; if no matching bulk endpoints are found, the app reports `UNSUPPORTED USB INTERFACE` instead of falsely showing connected telemetry. An optional Flipper `.fap` companion could be developed later, but **no FAP is shipped or required for the basic CLI diagnostic**.

## Validation

`tools/HrvMathTest.kt` tests constant and varying intervals, RMS difference, SDNN, pNN50, parsing and invalid input rejection. Run with an installed Kotlin compiler:

```bash
kotlinc app/src/main/kotlin/com/casper/lab/HrvMath.kt tools/HrvMathTest.kt -include-runtime -d /tmp/hrv-test.jar
java -jar /tmp/hrv-test.jar
```

The local implementation and web preview are provided as source because this preparation environment does not have the Android SDK, Gradle distribution or Android emulator installed. **The actual APK and USB behavior have not yet been device-tested**. Run a normal Android Studio compile and USB connection check before treating it as a working hardware integration.

## Reference reading (for future consent-based experiments)

- MIT RF-Pose3D: https://rfpose3d.csail.mit.edu/
- MIT EQ-Radio: https://eqradio.csail.mit.edu/
- Radar-based noncontact heartbeat: https://arxiv.org/abs/2307.09766
- RF tensor tomography: https://arxiv.org/abs/1712.04919
- Complex-wall electromagnetic simulation: https://arxiv.org/abs/2102.07990
- Experimental floor-map reconstruction: https://arxiv.org/abs/2206.10750
- Android USB Host: https://developer.android.com/develop/connectivity/usb/host
- Flipper documentation: https://docs.flipper.net/

All future human-sensing experiments should be opt-in, appropriately authorized, and validated against independent reference measurements.