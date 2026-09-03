package digital.tonima.retroamp.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [TrackEntity::class], version = 1, exportSchema = false)
@TypeConverters(UriConverters::class)
abstract class RetroAmpDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
}
