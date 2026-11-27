package app.jwhabits.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.flow.MutableStateFlow
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * アプリ自身の更新。GitHub の最新リリース（タグ「v0.6.10」＝ versionName。添付は jw-habits.apk）を見て、
 * 今の版より新しければ APK を落としてインストール画面を開く。
 * 通信するのはここだけ（記録のデータは外に送らない）。
 */
object Updater {
    private const val LATEST = "https://api.github.com/repos/tapeinooo/jw-habits/releases/latest"

    data class Release(val tag: String, val version: String, val apkUrl: String, val notes: String)

    /** 新しい版があれば入る（起動時と、設定の「更新を確認」で確かめる） */
    val available = MutableStateFlow<Release?>(null)

    fun currentVersion(context: Context): String =
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0"

    /** "0.6.10" と "0.6.9" を数としてくらべる（a が新しければ true） */
    fun isNewer(a: String, b: String): Boolean {
        val x = a.split(".").map { it.toIntOrNull() ?: 0 }
        val y = b.split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(x.size, y.size)) {
            val d = x.getOrElse(i) { 0 } - y.getOrElse(i) { 0 }
            if (d != 0) return d > 0
        }
        return false
    }

    private fun open(url: URL): HttpURLConnection = (url.openConnection() as HttpURLConnection).apply {
        connectTimeout = 20_000
        readTimeout = 60_000
        instanceFollowRedirects = false
        setRequestProperty("User-Agent", "jw-habits-app")
        setRequestProperty("Accept", "application/vnd.github+json")
    }

    private fun latest(): Release? {
        val c = open(URL(LATEST))
        try {
            if (c.responseCode !in 200..299) throw IOException("GitHub ${c.responseCode}")
            val j = JSONObject(c.inputStream.bufferedReader().readText())
            val tag = j.getString("tag_name")
            val assets = j.optJSONArray("assets") ?: return null
            val apk = (0 until assets.length()).map { assets.getJSONObject(it) }
                .firstOrNull { it.optString("name").endsWith(".apk") } ?: return null
            return Release(tag, tag.removePrefix("v"), apk.getString("browser_download_url"), j.optString("body"))
        } finally {
            c.disconnect()
        }
    }

    /** 確かめて、新しければ available に入れる。戻り値は画面に出す文（通信は呼ぶ側で IO スレッドに） */
    fun check(context: Context): String = try {
        val r = latest()
        val cur = currentVersion(context)
        if (r != null && isNewer(r.version, cur)) {
            available.value = r
            "新しい版 ${r.tag} があります"
        } else {
            available.value = null
            "最新です（$cur）"
        }
    } catch (e: Exception) {
        "確認できませんでした: ${e.message?.take(100)}"
    }

    fun download(context: Context, r: Release, onProgress: (Float) -> Unit): File {
        val dir = File(context.cacheDir, "update").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val out = File(dir, "jw-habits-${r.tag}.apk")
        var url = URL(r.apkUrl)
        var c: HttpURLConnection
        var hops = 0
        // github.com → 保存場所への転送をたどる
        while (true) {
            c = open(url)
            val code = c.responseCode
            if (code in 300..399 && hops < 5) {
                url = URL(url, c.getHeaderField("Location"))
                c.disconnect()
                hops++
                continue
            }
            if (code !in 200..299) throw IOException("ダウンロードできません（$code）")
            break
        }
        val total = c.contentLengthLong
        c.inputStream.use { i ->
            out.outputStream().use { o ->
                val buf = ByteArray(64 * 1024)
                var done = 0L
                while (true) {
                    val n = i.read(buf)
                    if (n <= 0) break
                    o.write(buf, 0, n)
                    done += n
                    if (total > 0) onProgress(done.toFloat() / total)
                }
            }
        }
        c.disconnect()
        return out
    }

    /** false = 「この提供元のアプリを許可」の画面を開いた（許可してからもう一度） */
    fun install(context: Context, apk: File): Boolean {
        if (!context.packageManager.canRequestPackageInstalls()) {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            return false
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", apk)
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        return true
    }
}
