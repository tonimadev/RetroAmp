package digital.tonima.retroamp.core.repository

import digital.tonima.retroamp.core.database.TrackDao
import digital.tonima.retroamp.core.database.asEntity
import digital.tonima.retroamp.core.database.asExternalModel
import digital.tonima.retroamp.core.model.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PlaylistRepository(
    private val trackDao: TrackDao
) {
    fun getPlaylist(): Flow<List<Track>> = trackDao.getAllTracks().map { entities ->
        entities.map { it.asExternalModel() }
    }

    suspend fun saveTracks(tracks: List<Track>) {
        trackDao.insertAll(tracks.map { it.asEntity() })
    }

    suspend fun clearPlaylist() {
        trackDao.clearPlaylist()
    }
}
