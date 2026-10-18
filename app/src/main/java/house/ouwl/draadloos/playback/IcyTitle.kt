package house.ouwl.draadloos.playback

/** What is on air right now. [artist] is null when the stream gave only a title. */
data class NowPlaying(val artist: String?, val title: String) {
    /** The text shown for the song: "artist – title", or just the title. */
    val line: String get() = if (artist == null) title else "$artist – $title"
}

/** Turns the `StreamTitle` of a Shoutcast (ICY) stream into a [NowPlaying]. */
object IcyTitle {

    // Spaces on both sides, so hyphens inside names ("Jan-Pierre") don't split.
    private val separator = Regex("\\s[-–—]\\s")
    private val whitespace = Regex("\\s+")

    /**
     * Returns null when the text says nothing about a song: it is missing, blank, only a
     * separator, or just the [stationName] that streams send between songs.
     */
    fun parse(raw: String?, stationName: String): NowPlaying? {
        val text = collapse(raw.orEmpty())
        if (text.isEmpty() || text.equals(stationName, ignoreCase = true)) return null

        // Pad so a leading or trailing separator still has its spaces to match.
        val padded = " $text "
        val match = separator.find(padded)
        val (artist, title) = if (match == null) {
            "" to clean(text)
        } else {
            clean(padded.substring(0, match.range.first)) to clean(padded.substring(match.range.last + 1))
        }
        return when {
            title.isEmpty() && artist.isEmpty() -> null
            title.isEmpty() -> NowPlaying(null, artist)
            else -> NowPlaying(artist.ifEmpty { null }, title)
        }
    }

    private fun collapse(text: String): String = text.replace(whitespace, " ").trim()

    /**
     * Drops one pair of double quotes around the whole part. A lone apostrophe stays:
     * "'n Brief vir Simone" starts with one on purpose.
     */
    private fun clean(part: String): String {
        val text = collapse(part)
        val quoted = text.length >= 2 &&
            ((text.first() == '"' && text.last() == '"') || (text.first() == '“' && text.last() == '”'))
        return if (quoted) collapse(text.substring(1, text.length - 1)) else text
    }
}
