package digital.tonima.retroamp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.activity.viewModels
import digital.tonima.retroamp.core.database.RetroAmpDatabase
import digital.tonima.retroamp.core.repository.PlaylistRepository
import digital.tonima.retroamp.player.PlayerManager
import digital.tonima.retroamp.player.PlayerIntent
import digital.tonima.retroamp.player.PlayerViewModel
import digital.tonima.retroamp.player.PlayerViewModelFactory
import digital.tonima.retroamp.player.ui.PlayerScreen
import digital.tonima.retroamp.ui.theme.RetroAmpTheme

class MainActivity : ComponentActivity() {
    
    private val viewModel: PlayerViewModel by viewModels {
        val database = RetroAmpDatabase.getDatabase(applicationContext)
        val repository = PlaylistRepository(database.trackDao())
        val playerManager = PlayerManager(applicationContext)
        PlayerViewModelFactory(playerManager, repository)
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.RECORD_AUDIO] == true) {
            viewModel.onIntent(PlayerIntent.RefreshVisualizer)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }

        enableEdgeToEdge()
        setContent {
            RetroAmpTheme {
                PlayerScreen(viewModel = viewModel)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // ViewModel.onCleared will handle playerManager.release()
    }
}
