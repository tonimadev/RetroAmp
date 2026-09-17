package digital.tonima.retroamp.player

import android.net.Uri
import digital.tonima.retroamp.core.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PlaylistMutationsTest {

    private fun track(id: String) = Track(
        id = id,
        title = id,
        artist = "Artist",
        durationMs = 1000L,
        audioUrl = Uri.parse("content://media/external/audio/media/$id")
    )

    // --- mergeIncomingTracks ---

    @Test
    fun `merging into an empty playlist adds every incoming track`() {
        val incoming = listOf(track("a"), track("b"))

        val result = mergeIncomingTracks(emptyList(), incoming, hadDuplicateSelection = false)

        assertEquals(incoming, result.newTracksToAdd)
        assertEquals(incoming, result.updatedPlaylist)
        assertFalse(result.hadDuplicates)
    }

    @Test
    fun `tracks already in the playlist are not added again and are flagged as duplicates`() {
        val current = listOf(track("a"))
        val incoming = listOf(track("a"), track("b"))

        val result = mergeIncomingTracks(current, incoming, hadDuplicateSelection = false)

        assertEquals(listOf(track("b")), result.newTracksToAdd)
        assertEquals(listOf(track("a"), track("b")), result.updatedPlaylist)
        assertTrue(result.hadDuplicates)
    }

    @Test
    fun `selecting the same file twice in one batch is flagged as a duplicate even when new to the playlist`() {
        // Regression test: previously `hadDuplicates` compared the
        // already-deduped incoming list's size against the distinct URI
        // count, which could never differ (both were deduped by the same
        // id), so a same-file-twice selection was silently swallowed with no
        // feedback to the user.
        val incoming = listOf(track("a"))

        val result = mergeIncomingTracks(emptyList(), incoming, hadDuplicateSelection = true)

        assertEquals(listOf(track("a")), result.newTracksToAdd)
        assertTrue(result.hadDuplicates)
    }

    @Test
    fun `when every incoming track already exists nothing is added but duplicates are flagged`() {
        val current = listOf(track("a"), track("b"))
        val incoming = listOf(track("a"), track("b"))

        val result = mergeIncomingTracks(current, incoming, hadDuplicateSelection = false)

        assertTrue(result.newTracksToAdd.isEmpty())
        assertEquals(current, result.updatedPlaylist)
        assertTrue(result.hadDuplicates)
    }

    @Test
    fun `no duplicates means the flag stays false`() {
        val current = listOf(track("a"))
        val incoming = listOf(track("b"), track("c"))

        val result = mergeIncomingTracks(current, incoming, hadDuplicateSelection = false)

        assertFalse(result.hadDuplicates)
    }

    // --- removeTrackFromPlaylist ---

    @Test
    fun `removing an unknown track id returns null`() {
        val result = removeTrackFromPlaylist(listOf(track("a")), track("a"), "missing")

        assertNull(result)
    }

    @Test
    fun `removing a track that is not the current track keeps the current track unchanged`() {
        val playlist = listOf(track("a"), track("b"), track("c"))

        val result = removeTrackFromPlaylist(playlist, currentTrack = track("c"), trackId = "a")

        requireNotNull(result)
        assertEquals(listOf(track("b"), track("c")), result.newPlaylist)
        assertEquals(track("c"), result.newCurrentTrack)
        assertEquals(0, result.removedIndex)
    }

    @Test
    fun `removing the current track selects the track that took its place`() {
        val playlist = listOf(track("a"), track("b"), track("c"))

        val result = removeTrackFromPlaylist(playlist, currentTrack = track("b"), trackId = "b")

        requireNotNull(result)
        assertEquals(listOf(track("a"), track("c")), result.newPlaylist)
        assertEquals(track("c"), result.newCurrentTrack)
    }

    @Test
    fun `removing the last track while it is current falls back to the new last track`() {
        val playlist = listOf(track("a"), track("b"), track("c"))

        val result = removeTrackFromPlaylist(playlist, currentTrack = track("c"), trackId = "c")

        requireNotNull(result)
        assertEquals(listOf(track("a"), track("b")), result.newPlaylist)
        assertEquals(track("b"), result.newCurrentTrack)
    }

    @Test
    fun `removing the only track clears the current track`() {
        val playlist = listOf(track("a"))

        val result = removeTrackFromPlaylist(playlist, currentTrack = track("a"), trackId = "a")

        requireNotNull(result)
        assertTrue(result.newPlaylist.isEmpty())
        assertNull(result.newCurrentTrack)
    }
}
