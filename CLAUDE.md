# jw-habits

聖書通読・個人研究・奉仕時間を1タップで記録する自分用の Android アプリとウィジェット。記録は端末内だけ。

## 共通ルール

このアプリは自分用アプリの共通ルールに従う：
https://github.com/tapeinooo/app-rules/blob/main/RULES.md

作業を始める前に読むこと（読めないときは `add_repo` で `tapeinooo/app-rules` を足すか、
https://raw.githubusercontent.com/tapeinooo/app-rules/main/RULES.md を開く）。
主な点：GitHub Actions でビルド・main への push で自動リリース・版の番号は自動・同じ署名の鍵・アプリの中の更新・
コミットの作者は tapeinooo の noreply・秘密はリポジトリに入れない・利用者への説明は日本語でやさしく。

## このアプリだけの決まり

- 【例外・移行中】リリースはまだ Mac の scripts/release.sh（署名が Mac の debug 鍵のため）。版の番号も手で上げる（タグは「v」＋ versionName、添付は jw-habits.apk。アプリの中の更新は versionName をくらべる）。GitHub Actions はビルドとテストの確認だけ。鍵をリポジトリに移したら共通ルールの方式に切り替える。
- 通信はアプリの更新にだけ使う。記録のデータは外に送らない。
