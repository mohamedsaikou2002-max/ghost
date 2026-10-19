#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
if [[ -z "${ANDROID_HOME:-}" && -z "${ANDROID_SDK_ROOT:-}" ]]; then
  echo "Install Android SDK Platform 35 and set ANDROID_HOME or ANDROID_SDK_ROOT." >&2
  exit 1
fi
GRADLE="$ROOT/.tooling/gradle-8.9/bin/gradle"
if [[ ! -x "$GRADLE" ]]; then
  mkdir -p "$ROOT/.tooling"
  curl -fSL https://services.gradle.org/distributions/gradle-8.9-bin.zip -o "$ROOT/.tooling/gradle-8.9-bin.zip"
  unzip -oq "$ROOT/.tooling/gradle-8.9-bin.zip" -d "$ROOT/.tooling"
  rm -f "$ROOT/.tooling/gradle-8.9-bin.zip"
fi
cd "$ROOT"
"$GRADLE" :app:assembleDebug --no-daemon
printf 'Built APK: %s\n' "$ROOT/app/build/outputs/apk/debug/app-debug.apk"
