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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
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
import digital.tonima.retroamp.ui.theme.AppSkin
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
            skin = uiState.currentSkin,
            modifier = Modifier.fillMaxSize().background(uiState.currentSkin.backgroundColor)
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
    val skin = uiState.currentSkin
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize(),
        containerColor = skin.backgroundColor
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
                    .background(skin.secondaryColor),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = if (skin.forceAllCaps) " RETRO-AMP - ${skin.name}.MP3" else " RETRO-AMP - ${skin.name}.mp3",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontFamily = skin.fontFamily,
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
                    skin = skin,
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

                    val rawTrackText = uiState.currentTrack?.let { 
                        if (showTitle) it.title else it.artist 
                    } ?: "NO TRACK LOADED"
                    
                    val trackText = if (skin.forceAllCaps) rawTrackText.uppercase() else rawTrackText
                    
                    Text(
                        text = trackText,
                        color = skin.accentColor,
                        fontSize = 14.sp,
                        fontFamily = skin.fontFamily,
                        maxLines = 1
                    )
                    Text(
                        text = if (skin.forceAllCaps) "KBPS: 128  KHZ: 44.1" else "kbps: 128  khz: 44.1",
                        color = skin.accentColor.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        fontFamily = skin.fontFamily
                    )
                }
            }

            // Progress Slider
            PlaybackProgressSlider(
                positionProvider = positionProvider,
                durationMs = uiState.currentTrack?.durationMs ?: 0L,
                onSeek = { position -> onIntent(PlayerIntent.SeekTo(position)) },
                skin = skin,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                RetroButton(
                    text = "PREV",
                    icon = Icons.Filled.SkipPrevious,
                    onClick = { onIntent(PlayerIntent.SkipPrevious) },
                    skin = skin,
                    modifier = Modifier.weight(1f)
                )
                RetroButton(
                    text = "PLAY",
                    icon = Icons.Filled.PlayArrow,
                    onClick = { onIntent(PlayerIntent.Play) },
                    skin = skin,
                    modifier = Modifier.weight(1f)
                )
                RetroButton(
                    text = "PAUSE",
                    icon = Icons.Filled.Pause,
                    onClick = { onIntent(PlayerIntent.Pause) },
                    skin = skin,
                    modifier = Modifier.weight(1f)
                )
                RetroButton(
                    text = "STOP",
                    icon = Icons.Filled.Stop,
                    onClick = { onIntent(PlayerIntent.Stop) },
                    skin = skin,
                    modifier = Modifier.weight(1f)
                )
                RetroButton(
                    text = "NEXT",
                    icon = Icons.Filled.SkipNext,
                    onClick = { onIntent(PlayerIntent.SkipNext) },
                    skin = skin,
                    modifier = Modifier.weight(1f)
                )
                RetroButton(
                    text = "ADD",
                    icon = Icons.Filled.Add,
                    onClick = onAddClick,
                    skin = skin,
                    modifier = Modifier.weight(1f)
                )
                RetroButton(
                    text = "SKIN",
                    icon = Icons.Filled.Palette,
                    onClick = {
                        val nextSkin = if (skin == AppSkin.Winamp) AppSkin.EightBit else AppSkin.Winamp
                        onIntent(PlayerIntent.SwitchSkin(nextSkin))
                    },
                    skin = skin,
                    modifier = Modifier.weight(1.2f)
                )
            }
            
            // Volume
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (skin.forceAllCaps) "VOL" else "Vol",
                    color = skin.textColor,
                    fontSize = 12.sp,
                    fontFamily = skin.fontFamily,
                    modifier = Modifier.padding(end = 8.dp)
                )
                RetroSlider(
                    value = uiState.volume,
                    onValueChange = { newValue ->
                        onIntent(PlayerIntent.SetVolume(newValue))
                    },
                    skin = skin,
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Playlist
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(skin.backgroundColor)
                    .border(1.dp, skin.textColor)
            ) {
                LazyColumn {
                    items(uiState.playlist, key = { it.id }) { track ->
                        TrackItem(
                            track = track,
                            isSelected = track.id == uiState.currentTrack?.id,
                            onClick = { onIntent(PlayerIntent.SelectTrack(track.id)) },
                            skin = skin
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
                skin = skin,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(skin.backgroundColor)
                    .border(1.dp, skin.textColor)
            )
        }
    }
}

@Composable
private fun PlaybackTimeDisplay(
    positionProvider: () -> Long,
    skin: AppSkin,
    modifier: Modifier = Modifier
) {
    val position = positionProvider()
    val minutes = (position / 1000) / 60
    val seconds = (position / 1000) % 60
    val timeText = "%02d:%02d".format(minutes, seconds)

    SegmentedDisplay(
        text = timeText,
        label = "TIME",
        skin = skin,
        modifier = modifier
    )
}

@Composable
private fun PlaybackProgressSlider(
    positionProvider: () -> Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    skin: AppSkin,
    modifier: Modifier = Modifier
) {
    val position = positionProvider()
    val progress = if (durationMs > 0) position.toFloat() / durationMs else 0f

    RetroSlider(
        value = progress.coerceIn(0f, 1f),
        onValueChange = { newValue ->
            onSeek((newValue * durationMs).toLong())
        },
        skin = skin,
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
                isPlaying = true,
                currentSkin = AppSkin.EightBit
            ),
            amplitudeProvider = { 0.5f },
            positionProvider = { 2500L },
            onIntent = {},
            onAddClick = {},
            snackbarHostState = remember { SnackbarHostState() }
        )
    }
}
