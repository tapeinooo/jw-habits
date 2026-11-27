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

- 【例外】署名の鍵は Mac の debug 鍵（app/debug.keystore、パスワード android、別名 androiddebugkey）。公開リポジトリなので鍵そのものは入れず、GitHub の Secret DEBUG_KEYSTORE_B64（base64）に置き、Actions がそこから app/debug.keystore を作る。Secret がない間は、GitHub Actions はビルドしない。app/debug.keystore は絶対にコミットしない。
- 【例外】版の番号：手で付けていた頃（0.7.0・versionCode 21）より大きくするため、gradle.properties の versionOffset=100 を使う（versionName は 1.0.<実行番号>）。
- 縮小（R8）は、Room・Glance・serialization の keep の確認が要るので、まだオフ。
- 通信はアプリの更新にだけ使う。記録のデータは外に送らない。
