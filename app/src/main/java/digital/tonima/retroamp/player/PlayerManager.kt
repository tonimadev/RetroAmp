package digital.tonima.retroamp.player

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.media.audiofx.Visualizer
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
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
        connect()
    }

    private fun connect() {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, sessionToken)
            .setListener(object : MediaController.Listener {
                override fun onDisconnected(controller: MediaController) {
                    // The session went away from under us - e.g. PlaybackService
                    // stopped itself (onTaskRemoved) while paused. Drop the dead
                    // reference and reconnect so playback/observation commands
                    // resume working instead of silently talking to a corpse.
                    this@PlayerManager.controller = null
                    _isReady.value = false
                    connect()
                }
            })
            .buildAsync()
        controllerFuture = future
        future.addListener({
            try {
                controller = future.get()
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

    fun refreshVisualizer() {
        if (controller?.isPlaying == true) {
            stopVisualizer()
            requestAudioSessionIdAndStartVisualizer()
        }
    }

    private fun requestAudioSessionIdAndStartVisualizer() {
        val player = controller ?: return
        
        // If already running, don't restart unless requested
        if (visualizer != null && visualizer?.enabled == true) return

        val command = SessionCommand(PlaybackService.CUSTOM_COMMAND_GET_AUDIO_SESSION_ID, Bundle.EMPTY)
        val future = player.sendCustomCommand(command, Bundle.EMPTY)
        future.addListener({
            try {
                val result = future.get()
                if (result.resultCode == SessionResult.RESULT_SUCCESS) {
                    val audioSessionId = result.extras.getInt("audio_session_id", 0)
                    // Some devices might report 0 for a moment, or we might need to fallback to 0 (system mix)
                    // but usually 0 doesn't work well on modern Android for app audio without special permissions.
                    // We'll try to start it with whatever we get, but startVisualizer will check for 0.
                    startVisualizer(audioSessionId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, MoreExecutors.directExecutor())
    }

    private fun startVisualizer(audioSessionId: Int) {
        if (visualizer != null) return
        
        // Ensure RECORD_AUDIO permission is granted
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        try {
            // If audioSessionId is 0, it might capture system output (needs permission) 
            // but we try anyway as a last resort if it's all we have.
            visualizer = Visualizer(audioSessionId).apply {
                captureSize = Visualizer.getCaptureSizeRange()[1]
                setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(v: Visualizer?, waveform: ByteArray?, samplingRate: Int) {
                        waveform?.let {
                            var sum = 0f
                            for (i in it.indices) {
                                // Waveform is unsigned 8-bit, 128 is center
                                val sample = (it[i].toInt() and 0xFF) - 128
                                sum += (sample * sample).toFloat()
                            }
                            val rms = Math.sqrt((sum / it.size).toDouble()).toFloat()
                            
                            // More aggressive scaling for better reactivity
                            // RMS typically 0-127. 32-64 is common for music.
                            val normalized = (rms / 64f).coerceIn(0f, 1f)
                            
                            // Boost and non-linear response for "pop"
                            val boosted = (normalized * 1.8f).coerceIn(0f, 1f)
                            _amplitude.value = boosted
                        }
                    }

                    override fun onFftDataCapture(v: Visualizer?, fft: ByteArray?, samplingRate: Int) {}
                }, Visualizer.getMaxCaptureRate(), true, false)
                enabled = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            visualizer = null
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
                        .apply {
                            if (track.coverArtData != null) {
                                setArtworkData(track.coverArtData, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                            }
                        }
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
                        .apply {
                            if (track.coverArtData != null) {
                                setArtworkData(track.coverArtData, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                            }
                        }
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

    fun getMediaItemCount(): Int = controller?.mediaItemCount ?: 0

    // Named distinctly from the `currentTrackIndex` StateFlow property above:
    // Kotlin compiles that property's getter to the JVM method
    // `getCurrentTrackIndex()`, so a same-named function here would silently
    // clash with it at the bytecode level (legal, since return types differ,
    // but it confuses reflection-based tooling - e.g. MockK - and any Java
    // caller).
    fun getCurrentTrackIndexSnapshot(): Int = controller?.currentMediaItemIndex ?: -1

    // Intentionally NOT called from PlayerViewModel.onCleared(): this class is
    // an app-wide @Singleton, but its owning ViewModel can be destroyed (and
    // recreated) independently of actual playback ending - e.g. "Don't keep
    // activities", the OEM reclaiming memory, or simply backgrounding the app
    // on some devices. Releasing the controller here would leave this
    // singleton permanently disconnected for the rest of the process's life,
    // which is exactly what caused the UI to desync from real playback state
    // after minimizing and reopening the app. Nothing currently calls this;
    // it's kept for a future explicit "quit app" action if one is added.
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
