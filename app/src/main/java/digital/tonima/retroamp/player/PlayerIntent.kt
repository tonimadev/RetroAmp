package digital.tonima.retroamp.player

import android.net.Uri

sealed interface PlayerIntent {
    data object Play : PlayerIntent
    data object Pause : PlayerIntent
    data object Stop : PlayerIntent
    data object SkipNext : PlayerIntent
    data object SkipPrevious : PlayerIntent
    data class SelectTrack(val trackId: String) : PlayerIntent
    data class AddTracks(val uris: List<Uri>) : PlayerIntent
    data class RemoveTrack(val trackId: String) : PlayerIntent
    data object ClearPlaylist : PlayerIntent
    data class SeekTo(val positionMs: Long) : PlayerIntent
    data class SetVolume(val volume: Float) : PlayerIntent
    data class ShowMessage(val message: String) : PlayerIntent
    data object ConsumeEffect : PlayerIntent
}
