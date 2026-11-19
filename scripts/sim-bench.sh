#!/usr/bin/env bash
# B-138: the sample on the iOS simulator, with no Xcode project: the Kotlin/Native executable bundled by hand into an app,
# signed ad hoc, installed into a simulator and launched. Its numbers are the Mac's through the simulator, not a phone's.
#
#   scripts/sim-bench.sh bundle                          build the release executable and bundle it
#   scripts/sim-bench.sh show                            install and open the sample as a user sees it
#   scripts/sim-bench.sh run <log> [rounds] [scene-ms] [Scene,Scene]
#                                                        install, run the frame bench, its stdout into <log>, frames.csv beside
#
# SIM=<name or udid> picks the simulator ("iPhone 17" by default); it is booted if it is not.
set -euo pipefail

SIM="${SIM:-iPhone 17}"
ID=io.github.youndie.mutant.sample
APP=sample/build/ios-sim/MutantSample.app
BIN=sample/build/bin/iosSimulatorArm64/releaseExecutable/sample.kexe

bundle() {
  ./gradlew -q :sample:linkReleaseExecutableIosSimulatorArm64 :mutant-core:iosSimulatorArm64ProcessResources
  rm -rf "$APP"
  mkdir -p "$APP"
  cp "$BIN" "$APP/MutantSample"
  # Compose reads the bundled fonts from <bundle>/compose-resources/composeResources, as its Xcode integration lays them out.
  local res=mutant-core/build/processedResources/iosSimulatorArm64/main/composeResources
  [ -d "$res/io.github.youndie.mutant.resources/font" ] || { echo "no fonts in $res" >&2; exit 1; }
  mkdir -p "$APP/compose-resources"
  cp -R "$res" "$APP/compose-resources/"
  cat > "$APP/Info.plist" <<PLIST
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0"><dict>
  <key>CFBundleExecutable</key><string>MutantSample</string>
  <key>CFBundleIdentifier</key><string>$ID</string>
  <key>CFBundleName</key><string>mutant-ui</string>
  <key>CFBundlePackageType</key><string>APPL</string>
  <key>CFBundleShortVersionString</key><string>0.1</string>
  <key>CFBundleVersion</key><string>1</string>
  <key>CFBundleSupportedPlatforms</key><array><string>iPhoneSimulator</string></array>
  <key>MinimumOSVersion</key><string>16.0</string>
  <key>UIDeviceFamily</key><array><integer>1</integer></array>
  <key>UILaunchScreen</key><dict/>
  <key>CADisableMinimumFrameDurationOnPhone</key><true/>
</dict></plist>
PLIST
  codesign --force --sign - "$APP" >/dev/null
}

install() {
  xcrun simctl boot "$SIM" 2>/dev/null || true
  xcrun simctl bootstatus "$SIM" -b >/dev/null
  xcrun simctl install "$SIM" "$APP"
}

commit() {
  local c
  c="$(git rev-parse --short HEAD)"
  [ -z "$(git status --short)" ] || c="$c-dirty"
  echo "$c"
}

case "${1:-}" in
  bundle)
    bundle
    ;;
  show)
    install
    xcrun simctl launch --terminate-running-process "$SIM" "$ID"
    ;;
  run)
    log="$2"
    rounds="${3:-6}"
    ms="${4:-10000}"
    args=(-bench YES -rounds "$rounds" -sceneMs "$ms")
    [ -n "${5:-}" ] && args+=(-scenes "$5")
    install
    mkdir -p "$(dirname "$log")"
    {
      echo "# host: $(sysctl -n machdep.cpu.brand_string), macOS $(sw_vers -productVersion); simulator $(xcrun simctl list devices | grep -m1 -F "    $SIM (" | sed 's/^ *//')"
      echo "# runtime: $(xcrun simctl list runtimes | grep -m1 iOS | sed 's/^ *//')"
      echo "# commit $(commit); binary sha256 $(shasum -a 256 "$APP/MutantSample" | cut -c1-16)"
    } > "$log"
    # The console ends when the app exits, and the app never exits: read until DONE, then take it down from inside the
    # loop. Breaking alone leaves `simctl launch` waiting for a write that never comes, and the script hangs.
    xcrun simctl launch --console-pty --terminate-running-process "$SIM" "$ID" "${args[@]}" 2>&1 | while IFS= read -r line; do
      echo "$line" >> "$log"
      [[ "$line" == DONE* ]] && { xcrun simctl terminate "$SIM" "$ID" || true; break; }
    done || true
    cp "$(xcrun simctl get_app_container "$SIM" "$ID" data)/Documents/frames.csv" "${log%.log}-frames.csv"
    tail -15 "$log"
    ;;
  *)
    sed -n 2,10p "$0"
    exit 2
    ;;
esac
