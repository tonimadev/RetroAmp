package digital.tonima.retroamp.player

import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import digital.tonima.retroamp.core.model.Track
import digital.tonima.retroamp.core.repository.PlaylistRepository
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class PlayerViewModel(
    private val playerManager: PlayerManager,
    private val playlistRepository: PlaylistRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    init {
        _uiState.update { it.copy(volume = playerManager.getVolume()) }

        viewModelScope.launch {
            playerManager.isReady.collect { isReady ->
                if (isReady) {
                    _uiState.update { it.copy(volume = playerManager.getVolume()) }
                    
                    // Initial load from repository
                    if (_uiState.value.playlist.isEmpty()) {
                        val savedPlaylist = playlistRepository.getPlaylist().first()
                        if (savedPlaylist.isNotEmpty()) {
                            _uiState.update { state ->
                                state.copy(
                                    playlist = savedPlaylist.toImmutableList(),
                                    currentTrack = savedPlaylist.firstOrNull()
                                )
                            }
                            playerManager.setPlaylist(savedPlaylist)
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
            playerManager.amplitude.collect { amplitude ->
                _uiState.update { it.copy(amplitude = amplitude) }
            }
        }

        viewModelScope.launch {
            while (isActive) {
                if (_uiState.value.isPlaying) {
                    val position = playerManager.getCurrentPosition()
                    _uiState.update { it.copy(currentPositionMs = position) }
                }
                delay(500.milliseconds)
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
            is PlayerIntent.ShowMessage -> _uiState.update { it.copy(effect = PlayerEffect.ShowMessage(intent.message)) }
            PlayerIntent.ConsumeEffect -> _uiState.update { it.copy(effect = null) }
        }
    }

    private fun handleRemoveTrack(trackId: String) {
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
            viewModelScope.launch {
                playlistRepository.clearPlaylist()
                playlistRepository.saveTracks(newList)
            }
        }
    }

    private fun handleClearPlaylist() {
        _uiState.update { it.copy(playlist = listOf<Track>().toImmutableList(), currentTrack = null) }
        playerManager.clearPlaylist()
        viewModelScope.launch {
            playlistRepository.clearPlaylist()
        }
    }

    private fun handleAddTracks(uris: List<Uri>) {
        viewModelScope.launch {
            val newTracks = uris.map { uri ->
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
            }
            
            if (newTracks.isNotEmpty()) {
                val currentPlaylist = _uiState.value.playlist.toMutableList()
                currentPlaylist.addAll(newTracks)
                
                val updatedPlaylist = currentPlaylist.toImmutableList()
                _uiState.update { state ->
                    state.copy(
                        playlist = updatedPlaylist,
                        currentTrack = state.currentTrack ?: newTracks.firstOrNull()
                    )
                }
                playerManager.addTracks(newTracks)
                playlistRepository.saveTracks(updatedPlaylist)
            } else {
                _uiState.update { it.copy(effect = PlayerEffect.ShowMessage("No valid audio files found")) }
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
            
            Track(
                id = uri.toString(),
                title = title ?: uri.lastPathSegment ?: "Unknown Title",
                artist = artist ?: "Unknown Artist",
                album = album,
                durationMs = duration,
                audioUrl = uri,
                coverArtUrl = null
            )
        } catch (_: Exception) {
            Track(
                id = uri.toString(),
                title = uri.lastPathSegment ?: "Unknown",
                artist = "Unknown Artist",
                durationMs = 0L,
                audioUrl = uri,
                coverArtUrl = null
            )
        } finally {
            retriever.release()
        }
    }
    
    fun setPlaylist(tracks: List<Track>) {
        _uiState.update { it.copy(
            playlist = tracks.toImmutableList(),
            currentTrack = if (tracks.isNotEmpty() && it.currentTrack == null) tracks[0] else it.currentTrack
        ) }
        playerManager.setPlaylist(tracks)
    }

    override fun onCleared() {
        playerManager.release()
    }
}
