package digital.tonima.retroamp.player

import androidx.compose.runtime.Immutable
import digital.tonima.retroamp.core.model.Track
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class PlayerUiState(
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val playlist: ImmutableList<Track> = persistentListOf(),
    val currentPositionMs: Long = 0L,
    val volume: Float = 1f,
    val effect: PlayerEffect? = null,
    val amplitude: Float = 0f
)
