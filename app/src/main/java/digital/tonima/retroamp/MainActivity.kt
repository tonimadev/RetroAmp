package digital.tonima.retroamp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.room.Room
import digital.tonima.retroamp.core.database.RetroAmpDatabase
import digital.tonima.retroamp.core.repository.PlaylistRepository
import digital.tonima.retroamp.player.PlayerManager
import digital.tonima.retroamp.player.PlayerViewModel
import digital.tonima.retroamp.player.ui.PlayerScreen
import digital.tonima.retroamp.ui.theme.RetroAmpTheme

class MainActivity : ComponentActivity() {
    
    private lateinit var playerManager: PlayerManager
    private lateinit var viewModel: PlayerViewModel
    private lateinit var database: RetroAmpDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        database = Room.databaseBuilder(
            applicationContext,
            RetroAmpDatabase::class.java,
            "retroamp.db"
        ).build()

        val repository = PlaylistRepository(database.trackDao())
        
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val permissionsToRequest = permissions.filter {
            ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), 0)
        }

        playerManager = PlayerManager(this)
        viewModel = PlayerViewModel(playerManager, repository)

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
