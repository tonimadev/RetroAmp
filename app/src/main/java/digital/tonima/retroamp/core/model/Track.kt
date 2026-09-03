package digital.tonima.retroamp.core.model

import android.net.Uri
import androidx.compose.runtime.Immutable

@Immutable
data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    val durationMs: Long,
    val audioUrl: Uri,
    val coverArtUrl: Uri? = null
)
