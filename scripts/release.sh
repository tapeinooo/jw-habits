#!/bin/bash
# 使い方: scripts/release.sh 0.4.0 "変更の一行説明"
# app/build.gradle.kts の versionName がこの版と一致し、作業ツリーがきれいなことを確かめてから、
# release APK を作り、タグを付けて GitHub Release に jw-habits.apk として上げる。
# 署名はこの Mac の debug 鍵（~/.android/debug.keystore）。別の鍵で作ると上書きインストールできない。
set -euo pipefail
cd "$(dirname "$0")/.."
VERSION="$1"; NOTES="${2:-}"
grep -q "versionName = \"$VERSION\"" app/build.gradle.kts || { echo "versionName が $VERSION ではありません"; exit 1; }
[ -z "$(git status --porcelain)" ] || { echo "コミットしていない変更があります"; exit 1; }
export JAVA_HOME="${JAVA_HOME_17:-/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home}"
./gradlew testDebugUnitTest assembleRelease -q
OUT=$(mktemp -d)/jw-habits.apk
cp app/build/outputs/apk/release/app-release.apk "$OUT"
git tag "v$VERSION" && git push -q origin "v$VERSION"
gh release create "v$VERSION" "$OUT" --title "$VERSION" --notes "$NOTES" --latest
echo "https://github.com/tapeinooo/jw-habits/releases/latest/download/jw-habits.apk"
