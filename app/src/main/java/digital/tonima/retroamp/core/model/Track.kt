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
    val coverArtUrl: Uri? = null,
    val coverArtData: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Track

        if (id != other.id) return false
        if (title != other.title) return false
        if (artist != other.artist) return false
        if (album != other.album) return false
        if (durationMs != other.durationMs) return false
        if (audioUrl != other.audioUrl) return false
        if (coverArtUrl != other.coverArtUrl) return false
        if (coverArtData != null) {
            if (other.coverArtData == null) return false
            if (!coverArtData.contentEquals(other.coverArtData)) return false
        } else if (other.coverArtData != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + artist.hashCode()
        result = 31 * result + (album?.hashCode() ?: 0)
        result = 31 * result + durationMs.hashCode()
        result = 31 * result + audioUrl.hashCode()
        result = 31 * result + (coverArtUrl?.hashCode() ?: 0)
        result = 31 * result + (coverArtData?.contentHashCode() ?: 0)
        return result
    }
}
