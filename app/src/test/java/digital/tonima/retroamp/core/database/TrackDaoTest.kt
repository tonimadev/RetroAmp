package digital.tonima.retroamp.core.database

import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TrackDaoTest {

    private lateinit var database: RetroAmpDatabase
    private lateinit var dao: TrackDao

    private fun entity(id: String) = TrackEntity(
        id = id,
        title = id,
        artist = "Artist",
        album = null,
        durationMs = 1000L,
        audioUrl = Uri.parse("content://media/$id"),
        coverArtUrl = null,
        coverArtData = null
    )

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RetroAmpDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.trackDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `tracks are returned in insertion order`() = runTest {
        dao.insertAll(listOf(entity("a"), entity("b"), entity("c")))

        val ids = dao.getAllTracks().first().map { it.id }

        assertEquals(listOf("a", "b", "c"), ids)
    }

    @Test
    fun `order survives a clear and re-insert of the remaining tracks, as done when removing a track`() = runTest {
        dao.insertAll(listOf(entity("a"), entity("b"), entity("c")))

        // Mirrors PlayerViewModel.handleRemoveTrack: clear then re-save the
        // remaining tracks in their current order.
        dao.clearPlaylist()
        dao.insertAll(listOf(entity("a"), entity("c")))

        val ids = dao.getAllTracks().first().map { it.id }
        assertEquals(listOf("a", "c"), ids)
    }

    @Test
    fun `appending new tracks keeps existing tracks first`() = runTest {
        dao.insertAll(listOf(entity("a"), entity("b")))

        // Mirrors PlayerViewModel.handleAddTracks: existing rows are ignored
        // on conflict, only the new ones actually get inserted.
        dao.insertAll(listOf(entity("a"), entity("b"), entity("c")))

        val ids = dao.getAllTracks().first().map { it.id }
        assertEquals(listOf("a", "b", "c"), ids)
    }

    @Test
    fun `clearing the playlist removes every track`() = runTest {
        dao.insertAll(listOf(entity("a"), entity("b")))

        dao.clearPlaylist()

        assertEquals(emptyList<TrackEntity>(), dao.getAllTracks().first())
    }
}
