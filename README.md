# jw-habits

聖書通読・個人研究・奉仕時間を1タップで記録する個人用 Android アプリ。
Kotlin + Jetpack Compose + Room + Glance。保存は端末内だけ（通信の権限なし）。

## ビルド

```sh
JAVA_HOME=$(/usr/libexec/java_home -v 17 2>/dev/null || echo /opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home) \
  ./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

`local.properties` に `sdk.dir` を書く（Homebrew の場合 `/opt/homebrew/share/android-commandlinetools`）。

## インストール（スマホ）

GitHub にログインしたブラウザで開くと、いつも最新版の APK が落ちてくる:
https://github.com/tapeinooo/jw-habits/releases/latest/download/jw-habits.apk

## リリース

`app/build.gradle.kts` の versionCode / versionName を上げてコミットしてから:

```sh
scripts/release.sh 0.4.0 "変更の説明"
```

署名はこの Mac の debug 鍵。別の鍵で作った APK は上書きインストールできない。
