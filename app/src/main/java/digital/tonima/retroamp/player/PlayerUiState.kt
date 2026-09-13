package digital.tonima.retroamp.player

import androidx.compose.runtime.Immutable
import digital.tonima.retroamp.core.model.Track
import digital.tonima.retroamp.ui.theme.AppSkin
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class PlayerUiState(
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val playlist: ImmutableList<Track> = persistentListOf(),
    val volume: Float = 1f,
    val effect: PlayerEffect? = null,
    val visualizerMode: Int = 0,
    val isVisualizerFullScreen: Boolean = false,
    val currentSkin: AppSkin = AppSkin.Winamp,
    val visualizerEnabled: Boolean = true,
    val visualizerBatterySaver: Boolean = false,
    val keepScreenOnWhilePlaying: Boolean = false
)

@Immutable
data class PlaybackProgressState(
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L
)