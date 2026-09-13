package digital.tonima.retroamp.player

import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import digital.tonima.retroamp.core.datastore.UserPreferencesRepository
import digital.tonima.retroamp.core.model.Track
import digital.tonima.retroamp.core.repository.PlaylistRepository
import digital.tonima.retroamp.ui.theme.AppSkin
import javax.inject.Inject
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerManager: PlayerManager,
    private val playlistRepository: PlaylistRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs = _currentPositionMs.asStateFlow()

    private val _amplitude = MutableStateFlow(0f)
    val amplitude = _amplitude.asStateFlow()

    private val playlistMutex = Mutex()

    private var smoothedAmplitude = 0f
    private val smoothingFactor = 0.3f // For decay speed

    init {
        _uiState.update { it.copy(volume = playerManager.getVolume()) }

        viewModelScope.launch {
            playerManager.isReady.collect { isReady ->
                if (isReady) {
                    _uiState.update { it.copy(volume = playerManager.getVolume()) }
                    
                    // Initial load from repository
                    playlistMutex.withLock {
                        if (_uiState.value.playlist.isEmpty()) {
                            val savedPlaylist = playlistRepository.getPlaylist().first().distinctBy { it.id }
                            val hasPlayerItems = playerManager.getMediaItemCount() > 0

                            if (savedPlaylist.isNotEmpty()) {
                                _uiState.update { state ->
                                    val currentIndex = playerManager.getCurrentTrackIndex()
                                    state.copy(
                                        playlist = savedPlaylist.toImmutableList(),
                                        currentTrack = if (currentIndex in savedPlaylist.indices) {
                                            savedPlaylist[currentIndex]
                                        } else {
                                            savedPlaylist.firstOrNull()
                                        }
                                    )
                                }
                                
                                // Only set playlist if player is empty
                                if (!hasPlayerItems) {
                                    playerManager.setPlaylist(savedPlaylist)
                                }
                            }
                        }
                    }
                }
            }
        }

        viewModelScope.launch {
            playerManager.isPlaying.collect { isPlaying ->
                _uiState.update { it.copy(isPlaying = isPlaying) }
            }
        }
        
        viewModelScope.launch {
            playerManager.currentTrackIndex.collect { index ->
                val track = if (index >= 0 && index < _uiState.value.playlist.size) {
                    _uiState.value.playlist[index]
                } else null
                _uiState.update { it.copy(currentTrack = track) }
            }
        }

        viewModelScope.launch {
            playerManager.amplitude.collect { rawAmplitude ->
                // Envelope follower: Instant attack, smoothed decay
                if (rawAmplitude > smoothedAmplitude) {
                    smoothedAmplitude = rawAmplitude
                } else {
                    smoothedAmplitude = smoothedAmplitude + smoothingFactor * (rawAmplitude - smoothedAmplitude)
                }
                _amplitude.value = smoothedAmplitude
            }
        }

        viewModelScope.launch {
            while (isActive) {
                if (_uiState.value.isPlaying) {
                    val position = playerManager.getCurrentPosition()
                    _currentPositionMs.value = position
                }
                delay(500.milliseconds)
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.userPreferences.collect { prefs ->
                _uiState.update {
                    it.copy(
                        visualizerEnabled = prefs.visualizerEnabled,
                        visualizerBatterySaver = prefs.visualizerBatterySaver,
                        keepScreenOnWhilePlaying = prefs.keepScreenOnWhilePlaying,
                        currentSkin = AppSkin.fromName(prefs.skinName)
                    )
                }
            }
        }
    }

    fun onIntent(intent: PlayerIntent) {
        when (intent) {
            PlayerIntent.Play -> {
                if (_uiState.value.playlist.isEmpty()) {
                    _uiState.update { it.copy(effect = PlayerEffect.ShowMessage("No tracks in playlist")) }
                } else {
                    playerManager.play()
                }
            }
            PlayerIntent.Pause -> playerManager.pause()
            PlayerIntent.Stop -> playerManager.stop()
            PlayerIntent.SkipNext -> playerManager.skipNext()
            PlayerIntent.SkipPrevious -> playerManager.skipPrevious()
            is PlayerIntent.SelectTrack -> {
                val index = _uiState.value.playlist.indexOfFirst { it.id == intent.trackId }
                if (index != -1) {
                    playerManager.seekTo(index)
                    playerManager.play()
                }
            }
            is PlayerIntent.AddTracks -> handleAddTracks(intent.uris)
            is PlayerIntent.RemoveTrack -> handleRemoveTrack(intent.trackId)
            PlayerIntent.ClearPlaylist -> handleClearPlaylist()
            is PlayerIntent.SeekTo -> playerManager.seekToPosition(intent.positionMs)
            is PlayerIntent.SetVolume -> {
                playerManager.setVolume(intent.volume)
                _uiState.update { it.copy(volume = intent.volume) }
            }
            PlayerIntent.ToggleVisualizer -> {
                _uiState.update { it.copy(visualizerMode = (it.visualizerMode + 1) % 2) }
            }
            PlayerIntent.ToggleVisualizerFullScreen -> {
                _uiState.update { it.copy(isVisualizerFullScreen = !it.isVisualizerFullScreen) }
            }
            is PlayerIntent.ShowMessage -> _uiState.update { it.copy(effect = PlayerEffect.ShowMessage(intent.message)) }
            PlayerIntent.ConsumeEffect -> _uiState.update { it.copy(effect = null) }
            PlayerIntent.RefreshVisualizer -> playerManager.refreshVisualizer()
            is PlayerIntent.SwitchSkin -> {
                _uiState.update { it.copy(currentSkin = intent.skin) }
                viewModelScope.launch { userPreferencesRepository.setSkinName(intent.skin.name) }
            }
            is PlayerIntent.SetVisualizerEnabled -> {
                _uiState.update { it.copy(visualizerEnabled = intent.enabled) }
                viewModelScope.launch { userPreferencesRepository.setVisualizerEnabled(intent.enabled) }
            }
            is PlayerIntent.SetVisualizerBatterySaver -> {
                _uiState.update { it.copy(visualizerBatterySaver = intent.enabled) }
                viewModelScope.launch { userPreferencesRepository.setVisualizerBatterySaver(intent.enabled) }
            }
            is PlayerIntent.SetKeepScreenOnWhilePlaying -> {
                _uiState.update { it.copy(keepScreenOnWhilePlaying = intent.enabled) }
                viewModelScope.launch { userPreferencesRepository.setKeepScreenOnWhilePlaying(intent.enabled) }
            }
        }
    }

    private fun handleRemoveTrack(trackId: String) {
        viewModelScope.launch {
            playlistMutex.withLock {
                val currentPlaylist = _uiState.value.playlist
                val index = currentPlaylist.indexOfFirst { it.id == trackId }
                if (index != -1) {
                    val newList = currentPlaylist.toMutableList().apply { removeAt(index) }.toImmutableList()
                    _uiState.update { state ->
                        state.copy(
                            playlist = newList,
                            currentTrack = if (state.currentTrack?.id == trackId) {
                                if (newList.isNotEmpty()) newList[index.coerceAtMost(newList.size - 1)] else null
                            } else state.currentTrack
                        )
                    }
                    playerManager.removeTrack(index)
                    playlistRepository.clearPlaylist()
                    playlistRepository.saveTracks(newList)
                }
            }
        }
    }

    private fun handleClearPlaylist() {
        viewModelScope.launch {
            playlistMutex.withLock {
                _uiState.update { it.copy(playlist = listOf<Track>().toImmutableList(), currentTrack = null) }
                playerManager.clearPlaylist()
                playlistRepository.clearPlaylist()
            }
        }
    }

    private fun handleAddTracks(uris: List<Uri>) {
        viewModelScope.launch {
            val incomingTracks = uris.map { uri ->
                // Try to persist permission for the URI
                try {
                    playerManager.context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: SecurityException) {
                    // Not a persistable URI or permission already granted
                }
                extractMetadata(uri)
            }.distinctBy { it.id } // Ensure incoming tracks are unique by ID

            if (incomingTracks.isEmpty()) {
                _uiState.update { it.copy(effect = PlayerEffect.ShowMessage("No valid audio files found")) }
                return@launch
            }

            playlistMutex.withLock {
                val currentPlaylist = _uiState.value.playlist
                val currentIds = currentPlaylist.map { it.id }.toSet()

                val newTracksToAdd = incomingTracks.filter { it.id !in currentIds }.distinctBy { it.id }
                val hadDuplicates = incomingTracks.size < uris.distinctBy { it.toString() }.size || incomingTracks.size > newTracksToAdd.size

                if (newTracksToAdd.isNotEmpty()) {
                    val updatedPlaylist = (currentPlaylist + newTracksToAdd).distinctBy { it.id }.toImmutableList()
                    _uiState.update { state ->
                        state.copy(
                            playlist = updatedPlaylist,
                            currentTrack = state.currentTrack ?: newTracksToAdd.firstOrNull(),
                            effect = if (hadDuplicates) PlayerEffect.ShowMessage("Some tracks are already in the playlist") else state.effect
                        )
                    }
                    playerManager.addTracks(newTracksToAdd)
                    playlistRepository.saveTracks(updatedPlaylist)
                } else if (hadDuplicates) {
                    _uiState.update { it.copy(effect = PlayerEffect.ShowMessage("Some tracks are already in the playlist")) }
                }
            }
        }
    }

    private fun extractMetadata(uri: Uri): Track {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(playerManager.context, uri)
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
            val coverArtData = retriever.embeddedPicture
            
            Track(
                id = uri.toString(),
                title = title ?: uri.lastPathSegment ?: "Unknown Title",
                artist = artist ?: "Unknown Artist",
                album = album,
                durationMs = duration,
                audioUrl = uri,
                coverArtUrl = null,
                coverArtData = coverArtData
            )
        } catch (_: Exception) {
            Track(
                id = uri.toString(),
                title = uri.lastPathSegment ?: "Unknown",
                artist = "Unknown Artist",
                durationMs = 0L,
                audioUrl = uri,
                coverArtUrl = null,
                coverArtData = null
            )
        } finally {
            retriever.release()
        }
    }
    
    fun setPlaylist(tracks: List<Track>) {
        viewModelScope.launch {
            playlistMutex.withLock {
                val distinctTracks = tracks.distinctBy { it.id }
                _uiState.update {
                    it.copy(
                        playlist = distinctTracks.toImmutableList(),
                        currentTrack = if (distinctTracks.isNotEmpty() && it.currentTrack == null) distinctTracks[0] else it.currentTrack
                    )
                }
                playerManager.setPlaylist(distinctTracks)
            }
        }
    }

    // Deliberately does not call playerManager.release(): PlayerManager is an
    // app-wide singleton whose MediaController connection must outlive any
    // single ViewModel instance. This ViewModel can be destroyed (and a new
    // one created on the next hiltViewModel() call) without playback ever
    // stopping - e.g. "Don't keep activities", low memory, or just minimizing
    // the app on some OEM skins. Releasing here previously left the singleton
    // permanently disconnected, which is what desynced the UI from the real
    // playback state after returning to the app. See PlayerManager.release().
}
