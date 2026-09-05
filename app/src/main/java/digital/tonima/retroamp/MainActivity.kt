package digital.tonima.retroamp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import digital.tonima.retroamp.player.PlayerViewModel
import digital.tonima.retroamp.player.ui.PlayerScreen
import digital.tonima.retroamp.ui.theme.RetroAmpTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        setContent {
            RetroAmpTheme {
                val viewModel: PlayerViewModel = hiltViewModel()
                PlayerScreen(viewModel = viewModel)
            }
        }
    }
}
