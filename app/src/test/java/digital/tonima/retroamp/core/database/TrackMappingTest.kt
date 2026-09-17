package digital.tonima.retroamp.core.database

import android.net.Uri
import digital.tonima.retroamp.core.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TrackMappingTest {

    @Test
    fun `track survives a round trip through TrackEntity`() {
        val track = Track(
            id = "content://media/1",
            title = "Song",
            artist = "Artist",
            album = "Album",
            durationMs = 123_456L,
            audioUrl = Uri.parse("content://media/1"),
            coverArtUrl = Uri.parse("content://media/1/art"),
            coverArtData = byteArrayOf(1, 2, 3)
        )

        val roundTripped = track.asEntity().asExternalModel()

        assertEquals(track, roundTripped)
    }

    @Test
    fun `null album and cover art survive the round trip as null`() {
        val track = Track(
            id = "content://media/2",
            title = "Song",
            artist = "Artist",
            album = null,
            durationMs = 0L,
            audioUrl = Uri.parse("content://media/2"),
            coverArtUrl = null,
            coverArtData = null
        )

        val roundTripped = track.asEntity().asExternalModel()

        assertEquals(track, roundTripped)
        assertNull(roundTripped.album)
        assertNull(roundTripped.coverArtData)
    }

    @Test
    fun `entity id is used as the primary key regardless of other fields`() {
        val entity = TrackEntity(
            id = "content://media/3",
            title = "Title",
            artist = "Artist",
            album = null,
            durationMs = 10L,
            audioUrl = Uri.parse("content://media/3"),
            coverArtUrl = null,
            coverArtData = null
        )

        assertEquals("content://media/3", entity.asExternalModel().id)
    }
}
