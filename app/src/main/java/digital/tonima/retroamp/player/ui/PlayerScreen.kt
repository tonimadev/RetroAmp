package digital.tonima.retroamp.player.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import digital.tonima.retroamp.core.model.Track
import digital.tonima.retroamp.core.ui.LaunchedUiEffectHandler
import digital.tonima.retroamp.player.PlayerEffect
import digital.tonima.retroamp.player.PlayerIntent
import digital.tonima.retroamp.player.PlayerUiState
import digital.tonima.retroamp.player.PlayerViewModel
import digital.tonima.retroamp.ui.components.RetroButton
import digital.tonima.retroamp.ui.components.RetroSlider
import digital.tonima.retroamp.ui.components.SegmentedDisplay
import digital.tonima.retroamp.ui.components.TrackItem
import digital.tonima.retroamp.ui.theme.RetroAmpTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import androidx.core.net.toUri

@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val effectFlow = remember(viewModel) { viewModel.uiState.map { it.effect } }
    
    val pickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.onIntent(PlayerIntent.AddTracks(uris))
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            pickerLauncher.launch(arrayOf("audio/*"))
        } else {
            viewModel.onIntent(PlayerIntent.ShowMessage("Permission denied to read storage"))
        }
    }

    // Handle Effects
    LaunchedUiEffectHandler(
        effectFlow = effectFlow,
        onConsume = { viewModel.onIntent(PlayerIntent.ConsumeEffect) },
        onEffect = { effect ->
            when (effect) {
                is PlayerEffect.Error -> {
                    scope.launch {
                        snackbarHostState.showSnackbar(effect.message)
                    }
                }
                is PlayerEffect.ShowMessage -> {
                    scope.launch {
                        snackbarHostState.showSnackbar(effect.message)
                    }
                }
            }
        }
    )

    val context = LocalContext.current
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    PlayerContent(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        onAddClick = {
            if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                pickerLauncher.launch(arrayOf("audio/*"))
            } else {
                permissionLauncher.launch(permission)
            }
        },
        snackbarHostState = snackbarHostState,
        modifier = modifier
    )
}

@Composable
fun PlayerContent(
    uiState: PlayerUiState,
    onIntent: (PlayerIntent) -> Unit,
    onAddClick: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title Bar Look
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .background(MaterialTheme.colorScheme.secondary),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = " RETRO-AMP - WINAMP.MP3",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            // Main Display Area
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val minutes = (uiState.currentPositionMs / 1000) / 60
                val seconds = (uiState.currentPositionMs / 1000) % 60
                val timeText = "%02d:%02d".format(minutes, seconds)

                SegmentedDisplay(
                    text = timeText,
                    label = "TIME",
                    modifier = Modifier.weight(1f)
                )
                
                Column(modifier = Modifier.weight(2f)) {
                    var showTitle by remember { mutableStateOf(true) }
                    LaunchedEffect(uiState.currentTrack) {
                        if (uiState.currentTrack != null) {
                            while (true) {
                                delay(3000.milliseconds)
                                showTitle = !showTitle
                            }
                        } else {
                            showTitle = true
                        }
                    }

                    val trackText = uiState.currentTrack?.let { 
                        if (showTitle) it.title else it.artist 
                    } ?: "NO TRACK LOADED"
                    
                    Text(
                        text = trackText,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1
                    )
                    Text(
                        text = "kbps: 128  khz: 44.1",
                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Progress Slider
            val progress = if (uiState.currentTrack != null && uiState.currentTrack.durationMs > 0) {
                uiState.currentPositionMs.toFloat() / uiState.currentTrack.durationMs
            } else 0f

            RetroSlider(
                value = progress.coerceIn(0f, 1f),
                onValueChange = { newValue ->
                    uiState.currentTrack?.let { track ->
                        onIntent(PlayerIntent.SeekTo((newValue * track.durationMs).toLong()))
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            // Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                RetroButton(text = "PREV", onClick = { onIntent(PlayerIntent.SkipPrevious) })
                RetroButton(text = "PLAY", onClick = { onIntent(PlayerIntent.Play) })
                RetroButton(text = "PAUSE", onClick = { onIntent(PlayerIntent.Pause) })
                RetroButton(text = "STOP", onClick = { onIntent(PlayerIntent.Stop) })
                RetroButton(text = "NEXT", onClick = { onIntent(PlayerIntent.SkipNext) })
                RetroButton(text = "ADD", onClick = onAddClick)
            }
            
            // Volume
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "VOL",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(end = 8.dp)
                )
                RetroSlider(
                    value = uiState.volume,
                    onValueChange = { newValue ->
                        onIntent(PlayerIntent.SetVolume(newValue))
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Playlist
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black)
                    .border(1.dp, MaterialTheme.colorScheme.onSurface)
            ) {
                LazyColumn {
                    items(uiState.playlist, key = { it.id }) { track ->
                        TrackItem(
                            track = track,
                            isSelected = track.id == uiState.currentTrack?.id,
                            onClick = { onIntent(PlayerIntent.SelectTrack(track.id)) }
                        )
                    }
                }
            }
            
            // Visualizer
            RetroVisualizer(
                amplitude = uiState.amplitude,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(Color.Black)
                    .border(1.dp, MaterialTheme.colorScheme.onSurface)
            )
        }
    }
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun PlayerPreview() {
    RetroAmpTheme {
        PlayerContent(
            uiState = PlayerUiState(
                currentTrack = Track(
                    id = "1",
                    title = "Winamp Intro",
                    artist = "Llama Soft",
                    durationMs = 5000L,
                    audioUrl = "https://example.com/audio.mp3".toUri(),
                    coverArtUrl = "https://picsum.photos/seed/winamp/200".toUri()
                ),
                isPlaying = true
            ),
            onIntent = {},
            onAddClick = {},
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}
