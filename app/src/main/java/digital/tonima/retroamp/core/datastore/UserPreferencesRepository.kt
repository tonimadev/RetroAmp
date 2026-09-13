package digital.tonima.retroamp.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import digital.tonima.retroamp.ui.theme.AppSkin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

/**
 * User-configurable app settings, persisted across launches.
 *
 * Defaults are chosen to preserve the app's original behavior (visualizer on,
 * full shader quality, screen-on management left to the system) so existing
 * users see no change until they open Settings.
 */
data class UserPreferences(
    val visualizerEnabled: Boolean = true,
    val visualizerBatterySaver: Boolean = false,
    val keepScreenOnWhilePlaying: Boolean = false,
    val skinName: String = AppSkin.Winamp.name
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val VISUALIZER_ENABLED = booleanPreferencesKey("visualizer_enabled")
        val VISUALIZER_BATTERY_SAVER = booleanPreferencesKey("visualizer_battery_saver")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on_while_playing")
        val SKIN_NAME = stringPreferencesKey("skin_name")
    }

    val userPreferences: Flow<UserPreferences> = context.settingsDataStore.data.map { prefs ->
        UserPreferences(
            visualizerEnabled = prefs[Keys.VISUALIZER_ENABLED] ?: true,
            visualizerBatterySaver = prefs[Keys.VISUALIZER_BATTERY_SAVER] ?: false,
            keepScreenOnWhilePlaying = prefs[Keys.KEEP_SCREEN_ON] ?: false,
            skinName = prefs[Keys.SKIN_NAME] ?: AppSkin.Winamp.name
        )
    }

    suspend fun setVisualizerEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.VISUALIZER_ENABLED] = enabled }
    }

    suspend fun setVisualizerBatterySaver(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.VISUALIZER_BATTERY_SAVER] = enabled }
    }

    suspend fun setKeepScreenOnWhilePlaying(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.KEEP_SCREEN_ON] = enabled }
    }

    suspend fun setSkinName(name: String) {
        context.settingsDataStore.edit { it[Keys.SKIN_NAME] = name }
    }
}
