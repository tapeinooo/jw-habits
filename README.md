# jw-habits

聖書通読・個人研究・奉仕時間を1タップで記録する個人用 Android アプリ。
Kotlin + Jetpack Compose + Room + Glance。保存は端末内だけ。通信はアプリの更新（GitHub の Releases を見る・落とす）にだけ使う。

## ビルド

```sh
JAVA_HOME=$(/usr/libexec/java_home -v 17 2>/dev/null || echo /opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home) \
  ./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

`local.properties` に `sdk.dir` を書く（Homebrew の場合 `/opt/homebrew/share/android-commandlinetools`）。

## インストール（スマホ）

Releases の最新の APK（`jw-habits-v1.0.<番号>.apk`）をスマホで開いて入れる：
https://github.com/tapeinooo/jw-habits/releases/latest

## アプリの中の更新

起動時と、設定のいちばん上の「更新を確認」で最新のリリースを見て、新しければ知らせが出る（「更新する」で落としてインストール）。

## リリース

main に変更が入るたびに GitHub Actions が APK を作って Releases に出す（版の番号は自動。共通ルール https://github.com/tapeinooo/app-rules/blob/main/RULES.md ）。
署名は前から使っている Mac の debug 鍵（`app/debug.keystore`）。別の鍵で作った APK は上書きインストールできない。
