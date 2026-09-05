package digital.tonima.retroamp.player.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.core.net.toUri
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
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val amplitudeState = viewModel.amplitude.collectAsState()
    val positionState = viewModel.currentPositionMs.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val effectFlow = remember(viewModel) { viewModel.uiState.map { it.effect } }
    
    val context = LocalContext.current
    
    val visualizerPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.onIntent(PlayerIntent.RefreshVisualizer)
        }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            visualizerPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

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

    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    PlayerContent(
        uiState = uiState,
        amplitudeProvider = { amplitudeState.value },
        positionProvider = { positionState.value },
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

    // Full Screen Visualizer Overlay
    AnimatedVisibility(
        visible = uiState.isVisualizerFullScreen,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        RetroVisualizer(
            amplitudeProvider = { amplitudeState.value },
            mode = uiState.visualizerMode,
            onToggleMode = { viewModel.onIntent(PlayerIntent.ToggleVisualizer) },
            onToggleFullScreen = { viewModel.onIntent(PlayerIntent.ToggleVisualizerFullScreen) },
            modifier = Modifier.fillMaxSize().background(Color.Black)
        )
    }
}

@Composable
fun PlayerContent(
    uiState: PlayerUiState,
    amplitudeProvider: () -> Float,
    positionProvider: () -> Long,
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
                PlaybackTimeDisplay(
                    positionProvider = positionProvider,
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
            PlaybackProgressSlider(
                positionProvider = positionProvider,
                durationMs = uiState.currentTrack?.durationMs ?: 0L,
                onSeek = { position -> onIntent(PlayerIntent.SeekTo(position)) },
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
                amplitudeProvider = amplitudeProvider,
                mode = uiState.visualizerMode,
                onToggleMode = { onIntent(PlayerIntent.ToggleVisualizer) },
                onToggleFullScreen = { onIntent(PlayerIntent.ToggleVisualizerFullScreen) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(Color.Black)
                    .border(1.dp, MaterialTheme.colorScheme.onSurface)
            )
        }
    }
}

@Composable
private fun PlaybackTimeDisplay(
    positionProvider: () -> Long,
    modifier: Modifier = Modifier
) {
    val position = positionProvider()
    val minutes = (position / 1000) / 60
    val seconds = (position / 1000) % 60
    val timeText = "%02d:%02d".format(minutes, seconds)

    SegmentedDisplay(
        text = timeText,
        label = "TIME",
        modifier = modifier
    )
}

@Composable
private fun PlaybackProgressSlider(
    positionProvider: () -> Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val position = positionProvider()
    val progress = if (durationMs > 0) position.toFloat() / durationMs else 0f

    RetroSlider(
        value = progress.coerceIn(0f, 1f),
        onValueChange = { newValue ->
            onSeek((newValue * durationMs).toLong())
        },
        modifier = modifier
    )
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
            amplitudeProvider = { 0.5f },
            positionProvider = { 2500L },
            onIntent = {},
            onAddClick = {},
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}
