package digital.tonima.retroamp.player

import android.content.ComponentName
import android.content.Context
import android.media.audiofx.Visualizer
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import digital.tonima.retroamp.core.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class PlayerManager(val context: Context) {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()
    
    private val _currentTrackIndex = MutableStateFlow(-1)
    val currentTrackIndex = _currentTrackIndex.asStateFlow()

    private val _amplitude = MutableStateFlow(0f)
    val amplitude = _amplitude.asStateFlow()

    private val _isReady = MutableStateFlow(false)
    val isReady = _isReady.asStateFlow()

    private var visualizer: Visualizer? = null

    init {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                controller = controllerFuture?.get()
                setupController()
                _isReady.value = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, MoreExecutors.directExecutor())
    }

    private fun setupController() {
        val player = controller ?: return
        
        _isPlaying.value = player.isPlaying
        _currentTrackIndex.value = player.currentMediaItemIndex
        
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) {
                    requestAudioSessionIdAndStartVisualizer()
                } else {
                    stopVisualizer()
                }
            }
            
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                _currentTrackIndex.value = player.currentMediaItemIndex
            }
            
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY && player.isPlaying) {
                    requestAudioSessionIdAndStartVisualizer()
                }
            }
        })
        
        if (player.isPlaying) {
            requestAudioSessionIdAndStartVisualizer()
        }
    }

    private fun requestAudioSessionIdAndStartVisualizer() {
        val player = controller ?: return
        val command = SessionCommand(PlaybackService.CUSTOM_COMMAND_GET_AUDIO_SESSION_ID, Bundle.EMPTY)
        val future = player.sendCustomCommand(command, Bundle.EMPTY)
        future.addListener({
            try {
                val result = future.get()
                if (result.resultCode == 0) { // RESULT_SUCCESS
                    val audioSessionId = result.extras.getInt("audio_session_id", 0)
                    if (audioSessionId != 0) {
                        startVisualizer(audioSessionId)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, MoreExecutors.directExecutor())
    }

    private fun startVisualizer(audioSessionId: Int) {
        if (visualizer != null || audioSessionId == 0) return
        try {
            visualizer = Visualizer(audioSessionId).apply {
                captureSize = Visualizer.getCaptureSizeRange()[1]
                setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(v: Visualizer?, waveform: ByteArray?, samplingRate: Int) {
                        waveform?.let {
                            var sum = 0f
                            for (i in it.indices) {
                                val sample = (it[i].toInt() and 0xFF) - 128
                                sum += (sample * sample).toFloat()
                            }
                            val rms = Math.sqrt((sum / it.size).toDouble()).toFloat()
                            _amplitude.value = (rms / 128f).coerceIn(0f, 1f)
                        }
                    }

                    override fun onFftDataCapture(v: Visualizer?, fft: ByteArray?, samplingRate: Int) {}
                }, Visualizer.getMaxCaptureRate() / 2, true, false)
                enabled = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopVisualizer() {
        visualizer?.enabled = false
        visualizer?.release()
        visualizer = null
        _amplitude.value = 0f
    }

    fun setPlaylist(tracks: List<Track>) {
        val player = controller ?: return
        val mediaItems = tracks.map { track ->
            MediaItem.Builder()
                .setMediaId(track.id)
                .setUri(track.audioUrl)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(track.title)
                        .setArtist(track.artist)
                        .setAlbumTitle(track.album)
                        .setDurationMs(track.durationMs)
                        .setArtworkUri(track.coverArtUrl)
                        .build()
                )
                .build()
        }
        player.setMediaItems(mediaItems)
        player.prepare()
    }

    fun addTracks(tracks: List<Track>) {
        val player = controller ?: return
        val mediaItems = tracks.map { track ->
            MediaItem.Builder()
                .setMediaId(track.id)
                .setUri(track.audioUrl)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(track.title)
                        .setArtist(track.artist)
                        .setAlbumTitle(track.album)
                        .setDurationMs(track.durationMs)
                        .setArtworkUri(track.coverArtUrl)
                        .build()
                )
                .build()
        }
        val wasEmpty = player.mediaItemCount == 0
        player.addMediaItems(mediaItems)
        
        if (wasEmpty || player.playbackState == Player.STATE_IDLE) {
            player.prepare()
        }
        
        if (_currentTrackIndex.value == -1 && player.mediaItemCount > 0) {
            _currentTrackIndex.value = player.currentMediaItemIndex
        }
    }

    fun removeTrack(index: Int) {
        val player = controller ?: return
        if (index >= 0 && index < player.mediaItemCount) {
            player.removeMediaItem(index)
        }
    }

    fun clearPlaylist() {
        val player = controller ?: return
        player.clearMediaItems()
        _currentTrackIndex.value = -1
    }

    fun play() {
        controller?.play()
    }

    fun pause() {
        controller?.pause()
    }

    fun stop() {
        controller?.stop()
    }

    fun skipNext() {
        val player = controller ?: return
        if (player.hasNextMediaItem()) {
            player.seekToNext()
        }
    }

    fun skipPrevious() {
        val player = controller ?: return
        if (player.hasPreviousMediaItem()) {
            player.seekToPrevious()
        }
    }

    fun seekTo(index: Int) {
        val player = controller ?: return
        if (index >= 0 && index < player.mediaItemCount) {
            player.seekTo(index, 0)
        }
    }

    fun seekToPosition(positionMs: Long) {
        controller?.seekTo(positionMs)
    }

    fun setVolume(volume: Float) {
        controller?.volume = volume.coerceIn(0f, 1f)
    }

    fun getVolume(): Float = controller?.volume ?: 1.0f

    fun getCurrentPosition(): Long = controller?.currentPosition ?: 0L

    fun release() {
        stopVisualizer()
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
        controller = null
        controllerFuture = null
        _isReady.value = false
    }

    @OptIn(UnstableApi::class)
    fun getExoPlayer(): Player? = controller
}
