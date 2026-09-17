package digital.tonima.retroamp.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {
    // ORDER BY rowid preserves insertion order. Without it, SQLite makes no
    // ordering guarantee, so a restored playlist could silently come back
    // shuffled relative to what the user built and to the MediaController's
    // own item order, desyncing "now playing" after a process restart.
    @Query("SELECT * FROM tracks ORDER BY rowid ASC")
    fun getAllTracks(): Flow<List<TrackEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(tracks: List<TrackEntity>)

    @Query("DELETE FROM tracks")
    suspend fun clearPlaylist()
}
