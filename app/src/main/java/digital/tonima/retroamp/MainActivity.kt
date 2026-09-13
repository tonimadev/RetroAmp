package digital.tonima.retroamp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import digital.tonima.retroamp.player.PlayerViewModel
import digital.tonima.retroamp.player.ui.PlayerScreen
import digital.tonima.retroamp.player.ui.SettingsScreen
import digital.tonima.retroamp.ui.theme.RetroAmpTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            RetroAmpTheme {
                val viewModel: PlayerViewModel = hiltViewModel()
                var showSettings by rememberSaveable { mutableStateOf(false) }

                if (showSettings) {
                    SettingsScreen(
                        viewModel = viewModel,
                        onBack = { showSettings = false }
                    )
                } else {
                    PlayerScreen(
                        viewModel = viewModel,
                        onOpenSettings = { showSettings = true }
                    )
                }
            }
        }
    }
}
