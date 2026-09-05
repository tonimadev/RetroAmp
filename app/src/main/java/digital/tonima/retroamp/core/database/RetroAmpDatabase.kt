package digital.tonima.retroamp.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [TrackEntity::class], version = 2, exportSchema = false)
@TypeConverters(UriConverters::class)
abstract class RetroAmpDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao

    companion object {
        @Volatile
        private var INSTANCE: RetroAmpDatabase? = null

        fun getDatabase(context: Context): RetroAmpDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RetroAmpDatabase::class.java,
                    "retroamp.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
