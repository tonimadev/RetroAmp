package digital.tonima.retroamp.player

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import digital.tonima.retroamp.core.datastore.UserPreferences
import digital.tonima.retroamp.core.datastore.UserPreferencesRepository
import digital.tonima.retroamp.core.model.Track
import digital.tonima.retroamp.core.repository.PlaylistRepository
import digital.tonima.retroamp.ui.theme.AppSkin
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PlayerViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    private lateinit var playerManager: PlayerManager
    private lateinit var playlistRepository: PlaylistRepository
    private lateinit var userPreferencesRepository: UserPreferencesRepository
    private lateinit var isReady: MutableStateFlow<Boolean>
    private lateinit var playlistFlow: MutableStateFlow<List<Track>>

    private fun track(id: String) = Track(
        id = id,
        title = id,
        artist = "Artist",
        durationMs = 1000L,
        audioUrl = Uri.parse("content://media/$id")
    )

    private fun newViewModel(): PlayerViewModel =
        PlayerViewModel(playerManager, playlistRepository, userPreferencesRepository)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)

        playerManager = mockk(relaxed = true)
        every { playerManager.context } returns ApplicationProvider.getApplicationContext<Context>()
        every { playerManager.getVolume() } returns 1f
        every { playerManager.getMediaItemCount() } returns 0
        every { playerManager.getCurrentTrackIndexSnapshot() } returns -1
        every { playerManager.isReady } returns MutableStateFlow(false).also { isReady = it }
        every { playerManager.isPlaying } returns MutableStateFlow(false)
        every { playerManager.currentTrackIndex } returns MutableStateFlow(-1)
        every { playerManager.amplitude } returns MutableStateFlow(0f)

        playlistRepository = mockk(relaxed = true)
        playlistFlow = MutableStateFlow(emptyList())
        every { playlistRepository.getPlaylist() } returns playlistFlow
        coEvery { playlistRepository.saveTracks(any()) } returns Unit
        coEvery { playlistRepository.clearPlaylist() } returns Unit

        userPreferencesRepository = mockk(relaxed = true)
        every { userPreferencesRepository.userPreferences } returns flowOf(UserPreferences())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `Play with an empty playlist shows a message instead of starting playback`() {
        val viewModel = newViewModel()

        viewModel.onIntent(PlayerIntent.Play)

        assertEquals(
            PlayerEffect.ShowMessage("No tracks in playlist"),
            viewModel.uiState.value.effect
        )
        verify(exactly = 0) { playerManager.play() }
    }

    @Test
    fun `RemoveTrack updates the playlist and persists the change`() {
        val viewModel = newViewModel()
        viewModel.setPlaylist(listOf(track("a"), track("b")))

        viewModel.onIntent(PlayerIntent.RemoveTrack("a"))

        assertEquals(listOf(track("b")), viewModel.uiState.value.playlist)
        verify { playerManager.removeTrack(0) }
        coVerify { playlistRepository.clearPlaylist() }
        coVerify { playlistRepository.saveTracks(listOf(track("b"))) }
    }

    @Test
    fun `RemoveTrack reselects the next track when the current track is removed`() {
        val viewModel = newViewModel()
        viewModel.setPlaylist(listOf(track("a"), track("b")))
        assertEquals(track("a"), viewModel.uiState.value.currentTrack)

        viewModel.onIntent(PlayerIntent.RemoveTrack("a"))

        assertEquals(track("b"), viewModel.uiState.value.currentTrack)
    }

    @Test
    fun `RemoveTrack with an unknown id leaves the playlist untouched`() {
        val viewModel = newViewModel()
        viewModel.setPlaylist(listOf(track("a")))

        viewModel.onIntent(PlayerIntent.RemoveTrack("missing"))

        assertEquals(listOf(track("a")), viewModel.uiState.value.playlist)
        verify(exactly = 0) { playerManager.removeTrack(any()) }
    }

    @Test
    fun `ClearPlaylist empties the state and the persisted playlist`() {
        val viewModel = newViewModel()
        viewModel.setPlaylist(listOf(track("a"), track("b")))

        viewModel.onIntent(PlayerIntent.ClearPlaylist)

        assertTrue(viewModel.uiState.value.playlist.isEmpty())
        assertNull(viewModel.uiState.value.currentTrack)
        verify { playerManager.clearPlaylist() }
        coVerify { playlistRepository.clearPlaylist() }
    }

    @Test
    fun `SelectTrack seeks the player to the chosen track and starts playback`() {
        val viewModel = newViewModel()
        viewModel.setPlaylist(listOf(track("a"), track("b")))

        viewModel.onIntent(PlayerIntent.SelectTrack("b"))

        verify { playerManager.seekTo(1) }
        verify { playerManager.play() }
    }

    @Test
    fun `SelectTrack with an unknown id does nothing`() {
        val viewModel = newViewModel()
        viewModel.setPlaylist(listOf(track("a")))

        viewModel.onIntent(PlayerIntent.SelectTrack("missing"))

        verify(exactly = 0) { playerManager.seekTo(any()) }
        verify(exactly = 0) { playerManager.play() }
    }

    @Test
    fun `SetVolume forwards the clamped-by-PlayerManager volume and updates state`() {
        val viewModel = newViewModel()

        viewModel.onIntent(PlayerIntent.SetVolume(0.5f))

        verify { playerManager.setVolume(0.5f) }
        assertEquals(0.5f, viewModel.uiState.value.volume)
    }

    @Test
    fun `ToggleVisualizer cycles between the two visualizer modes`() {
        val viewModel = newViewModel()

        viewModel.onIntent(PlayerIntent.ToggleVisualizer)
        assertEquals(1, viewModel.uiState.value.visualizerMode)

        viewModel.onIntent(PlayerIntent.ToggleVisualizer)
        assertEquals(0, viewModel.uiState.value.visualizerMode)
    }

    @Test
    fun `SwitchSkin updates state immediately and persists the choice`() {
        val viewModel = newViewModel()

        viewModel.onIntent(PlayerIntent.SwitchSkin(AppSkin.EightBit))

        assertEquals(AppSkin.EightBit, viewModel.uiState.value.currentSkin)
        coVerify { userPreferencesRepository.setSkinName(AppSkin.EightBit.name) }
    }

    @Test
    fun `ConsumeEffect clears the pending effect`() {
        val viewModel = newViewModel()
        viewModel.onIntent(PlayerIntent.Play) // playlist is empty, sets an effect

        viewModel.onIntent(PlayerIntent.ConsumeEffect)

        assertNull(viewModel.uiState.value.effect)
    }

    @Test
    fun `becoming ready restores the saved playlist and resumes at the persisted track index`() {
        playlistFlow.value = listOf(track("a"), track("b"), track("c"))
        every { playerManager.getCurrentTrackIndexSnapshot() } returns 2

        val viewModel = newViewModel()
        isReady.value = true

        assertEquals(listOf(track("a"), track("b"), track("c")), viewModel.uiState.value.playlist)
        assertEquals(track("c"), viewModel.uiState.value.currentTrack)
    }

    @Test
    fun `becoming ready falls back to the first track when the persisted index is out of range`() {
        playlistFlow.value = listOf(track("a"), track("b"))
        every { playerManager.getCurrentTrackIndexSnapshot() } returns 99

        val viewModel = newViewModel()
        isReady.value = true

        assertEquals(track("a"), viewModel.uiState.value.currentTrack)
    }
}
