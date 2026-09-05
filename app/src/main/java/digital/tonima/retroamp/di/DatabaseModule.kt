package digital.tonima.retroamp.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import digital.tonima.retroamp.core.database.RetroAmpDatabase
import digital.tonima.retroamp.core.database.TrackDao
import digital.tonima.retroamp.core.repository.PlaylistRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): RetroAmpDatabase {
        return RetroAmpDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideTrackDao(database: RetroAmpDatabase): TrackDao {
        return database.trackDao()
    }

    @Provides
    @Singleton
    fun providePlaylistRepository(trackDao: TrackDao): PlaylistRepository {
        return PlaylistRepository(trackDao)
    }
}
