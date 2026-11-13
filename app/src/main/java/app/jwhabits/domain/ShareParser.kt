package app.jwhabits.domain

/** JW Library などの「共有」で受け取った内容。link は無いこともある。issue は "2026-11" の形。 */
data class SharedPayload(val link: String?, val title: String, val issue: String?)

/** 共有テキストの解析。共有の形式は未確認なので、リンクと題名の推測だけをする。 */
object ShareParser {
    private val urlRegex = Regex("""https?://\S+""")
    private val issueRegex = Regex("""issue=(\d{4})(\d{2})""")
    private const val TRAILING = ".,;:)]}>」』）。、"

    fun parse(text: String?, subject: String?): SharedPayload? {
        val body = text.orEmpty()
        val link = urlRegex.find(body)?.value?.trimEnd { it in TRAILING }?.takeIf { it.length > "https://".length }
        val title = subject?.trim()?.takeIf { it.isNotEmpty() }
            ?: urlRegex.replace(body, "").lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() }.orEmpty()
        if (link == null && title.isEmpty()) return null
        val issue = link?.let { issueRegex.find(it) }?.let { "${it.groupValues[1]}-${it.groupValues[2]}" }
        return SharedPayload(link, title, issue)
    }
}
