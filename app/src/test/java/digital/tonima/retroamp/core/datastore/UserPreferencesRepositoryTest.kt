package digital.tonima.retroamp.core.datastore

import androidx.test.core.app.ApplicationProvider
import digital.tonima.retroamp.ui.theme.AppSkin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UserPreferencesRepositoryTest {

    // The underlying DataStore is cached behind a process-wide singleton
    // delegate (Context.settingsDataStore), not scoped to the Context
    // instance handed to it - so it outlives any single test's Context and
    // its values leak between test methods. Restore the defaults after every
    // test so each one (including the "defaults" test itself) always starts
    // from a clean slate regardless of run order.
    private val repository = UserPreferencesRepository(ApplicationProvider.getApplicationContext())

    @After
    fun tearDown() = runTest {
        repository.setVisualizerEnabled(true)
        repository.setVisualizerBatterySaver(false)
        repository.setKeepScreenOnWhilePlaying(false)
        repository.setSkinName(AppSkin.Winamp.name)
    }

    @Test
    fun `defaults preserve original app behavior before Settings is ever opened`() = runTest {
        val prefs = repository.userPreferences.first()

        assertTrue(prefs.visualizerEnabled)
        assertFalse(prefs.visualizerBatterySaver)
        assertFalse(prefs.keepScreenOnWhilePlaying)
        assertEquals(AppSkin.Winamp.name, prefs.skinName)
    }

    @Test
    fun `setVisualizerEnabled persists and is reflected in the flow`() = runTest {
        repository.setVisualizerEnabled(false)

        assertFalse(repository.userPreferences.first().visualizerEnabled)
    }

    @Test
    fun `setVisualizerBatterySaver persists and is reflected in the flow`() = runTest {
        repository.setVisualizerBatterySaver(true)

        assertTrue(repository.userPreferences.first().visualizerBatterySaver)
    }

    @Test
    fun `setKeepScreenOnWhilePlaying persists and is reflected in the flow`() = runTest {
        repository.setKeepScreenOnWhilePlaying(true)

        assertTrue(repository.userPreferences.first().keepScreenOnWhilePlaying)
    }

    @Test
    fun `setSkinName persists and is reflected in the flow`() = runTest {
        repository.setSkinName(AppSkin.EightBit.name)

        assertEquals(AppSkin.EightBit.name, repository.userPreferences.first().skinName)
    }

    @Test
    fun `settings changed independently do not clobber each other`() = runTest {
        repository.setVisualizerEnabled(false)
        repository.setSkinName(AppSkin.EightBit.name)

        val prefs = repository.userPreferences.first()
        assertFalse(prefs.visualizerEnabled)
        assertEquals(AppSkin.EightBit.name, prefs.skinName)
    }
}
