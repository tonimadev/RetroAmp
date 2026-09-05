package digital.tonima.retroamp.core.database

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey
import digital.tonima.retroamp.core.model.Track

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String?,
    val durationMs: Long,
    val audioUrl: Uri,
    val coverArtUrl: Uri?,
    val coverArtData: ByteArray?
)

fun TrackEntity.asExternalModel() = Track(
    id = id,
    title = title,
    artist = artist,
    album = album,
    durationMs = durationMs,
    audioUrl = audioUrl,
    coverArtUrl = coverArtUrl,
    coverArtData = coverArtData
)

fun Track.asEntity() = TrackEntity(
    id = id,
    title = title,
    artist = artist,
    album = album,
    durationMs = durationMs,
    audioUrl = audioUrl,
    coverArtUrl = coverArtUrl,
    coverArtData = coverArtData
)
