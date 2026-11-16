#!/usr/bin/env bash
# B-49: the frame benchmark on the attached phone, by the protocol pre-registered in docs/backlog/B-49-frame-cost-on-a-pixel-6a.md.
#
#   scripts/device-bench.sh install                      build the release, install it, compile speed-profile
#   scripts/device-bench.sh run <out dir> [rounds] [s]   the screen scenes, then the instance arms; logs pulled into <out dir>
#   scripts/device-bench.sh instance <out dir>           the instance arms alone, likewise
#   scripts/device-bench.sh show <scene>                 one scene on screen, for a look
#   scripts/device-bench.sh stop                         takes the activity down
set -euo pipefail

PKG=io.github.youndie.mutant.bench
ACT="$PKG/io.github.youndie.mutant.bench.android.BenchActivity"
ADB="${ADB:-$HOME/Library/Android/sdk/platform-tools/adb}"
FILES="/sdcard/Android/data/$PKG/files"

wait_done() {
  local deadline=$(($(date +%s) + $1))
  while [ "$(date +%s)" -lt "$deadline" ]; do
    if "$ADB" logcat -d -s MutantBench:I | grep -q DONE; then return 0; fi
    sleep 5
  done
  echo "no DONE within $1 s" >&2
  return 1
}

start() {
  "$ADB" logcat -c
  "$ADB" shell input keyevent KEYCODE_WAKEUP
  "$ADB" shell am force-stop "$PKG"
  "$ADB" shell am start -n "$ACT" "$@"
}

commit() {
  local c
  c="$(git rev-parse --short HEAD)"
  [ -z "$(git status --short)" ] || c="$c-dirty"
  echo "$c"
}

case "${1:-}" in
  install)
    ./gradlew :bench-android:assembleRelease -q
    "$ADB" install -r bench-android/build/outputs/apk/release/bench-android-release.apk
    "$ADB" shell cmd package compile -m speed-profile -f "$PKG"
    ;;
  run)
    out="$2"
    rounds="${3:-6}"
    seconds="${4:-10}"
    mkdir -p "$out"
    start --es mode bench --es commit "$(commit)" --ei rounds "$rounds" --ei seconds "$seconds"
    # Five scenes a round, then the instance arms (about 28 cases × 3 s of warm-up and five rounds).
    wait_done $((rounds * 5 * (seconds + 2) + 600))
    for f in scenes.csv metrics.csv instance.txt; do "$ADB" pull "$FILES/$f" "$out/$f"; done
    ;;
  instance)
    out="$2"
    mkdir -p "$out"
    start --es mode instance --es commit "$(commit)"
    wait_done 600
    "$ADB" pull "$FILES/instance.txt" "$out/instance.txt"
    ;;
  show)
    start --es mode show --es scene "$2"
    ;;
  stop)
    "$ADB" shell am force-stop "$PKG"
    ;;
  *)
    sed -n 2,9p "$0"
    exit 2
    ;;
esac
