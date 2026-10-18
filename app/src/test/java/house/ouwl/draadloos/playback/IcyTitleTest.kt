package house.ouwl.draadloos.playback

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class IcyTitleTest {

    private val station = "RSG"

    private fun parse(raw: String?) = IcyTitle.parse(raw, station)

    @Test
    fun artistAndTitle_splitOnDash() {
        assertThat(parse("Kobus Human - Môre")).isEqualTo(NowPlaying("Kobus Human", "Môre"))
    }

    @Test
    fun enAndEmDashes_splitToo() {
        assertThat(parse("Artist – Song")).isEqualTo(NowPlaying("Artist", "Song"))
        assertThat(parse("Artist — Song")).isEqualTo(NowPlaying("Artist", "Song"))
    }

    @Test
    fun onlyTheFirstSeparatorSplits() {
        assertThat(parse("A - B - C")).isEqualTo(NowPlaying("A", "B - C"))
    }

    @Test
    fun dashesInsideWords_doNotSplit() {
        assertThat(parse("Jan-Pierre Smit")).isEqualTo(NowPlaying(null, "Jan-Pierre Smit"))
    }

    @Test
    fun withoutSeparator_wholeTextIsTheTitle() {
        assertThat(parse("Nuus om sewe")).isEqualTo(NowPlaying(null, "Nuus om sewe"))
    }

    @Test
    fun emptySide_usesTheOtherAsTitle() {
        assertThat(parse(" - Song")).isEqualTo(NowPlaying(null, "Song"))
        assertThat(parse("Artist - ")).isEqualTo(NowPlaying(null, "Artist"))
    }

    @Test
    fun whitespaceAndQuotes_areCleaned() {
        assertThat(parse("  \"Artist\"   -   Song \n")).isEqualTo(NowPlaying("Artist", "Song"))
    }

    @Test
    fun nullBlankAndSeparatorOnly_haveNoSong() {
        assertThat(parse(null)).isNull()
        assertThat(parse("")).isNull()
        assertThat(parse("   ")).isNull()
        assertThat(parse(" - ")).isNull()
    }

    @Test
    fun stationNameOnly_hasNoSong() {
        assertThat(parse("rsg")).isNull()
        assertThat(parse(" RSG ")).isNull()
    }
}
