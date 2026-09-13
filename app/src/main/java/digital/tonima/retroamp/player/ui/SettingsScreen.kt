package digital.tonima.retroamp.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import digital.tonima.retroamp.player.PlayerIntent
import digital.tonima.retroamp.player.PlayerUiState
import digital.tonima.retroamp.player.PlayerViewModel
import digital.tonima.retroamp.ui.theme.AppSkin
import digital.tonima.retroamp.ui.theme.RetroAmpTheme

@Composable
fun SettingsScreen(
    viewModel: PlayerViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    SettingsContent(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        onBack = onBack,
        modifier = modifier
    )
}

@Composable
fun SettingsContent(
    uiState: PlayerUiState,
    onIntent: (PlayerIntent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val skin = uiState.currentSkin

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = skin.backgroundColor
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            // Title Bar Look, matching PlayerScreen
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .background(skin.secondaryColor),
                contentAlignment = Alignment.CenterStart
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.CenterStart).size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = if (skin.forceAllCaps) "BACK" else "Back",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = if (skin.forceAllCaps) "SETTINGS" else "Settings",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontFamily = skin.fontFamily,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                SettingsSection(
                    title = if (skin.forceAllCaps) "VISUALIZER" else "Visualizer",
                    skin = skin
                ) {
                    SettingSwitchRow(
                        title = if (skin.forceAllCaps) "PSYCHEDELIC VISUALIZER" else "Psychedelic visualizer",
                        description = "Turns off the animated shader effects. The biggest single battery/GPU saver on this screen.",
                        checked = uiState.visualizerEnabled,
                        onCheckedChange = { onIntent(PlayerIntent.SetVisualizerEnabled(it)) },
                        skin = skin
                    )
                    SettingSwitchRow(
                        title = if (skin.forceAllCaps) "BATTERY SAVER MODE" else "Battery saver mode",
                        description = "Swaps the shader for a simple bar visualizer. Good on older devices or if the phone gets warm.",
                        checked = uiState.visualizerBatterySaver,
                        onCheckedChange = { onIntent(PlayerIntent.SetVisualizerBatterySaver(it)) },
                        enabled = uiState.visualizerEnabled,
                        skin = skin
                    )
                }

                SettingsSection(
                    title = if (skin.forceAllCaps) "PLAYBACK" else "Playback",
                    skin = skin
                ) {
                    SettingSwitchRow(
                        title = if (skin.forceAllCaps) "KEEP SCREEN ON" else "Keep screen on while playing",
                        description = "Stops the display from sleeping during playback. Uses more battery while the screen stays lit.",
                        checked = uiState.keepScreenOnWhilePlaying,
                        onCheckedChange = { onIntent(PlayerIntent.SetKeepScreenOnWhilePlaying(it)) },
                        skin = skin
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    skin: AppSkin,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, skin.textColor)
            .background(skin.surfaceColor.copy(alpha = 0.3f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = title,
            color = skin.accentColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = skin.fontFamily
        )
        content()
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    skin: AppSkin,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            Text(
                text = title,
                color = if (enabled) skin.textColor else skin.textColor.copy(alpha = 0.4f),
                fontSize = 13.sp,
                fontFamily = skin.fontFamily,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                color = if (enabled) skin.textColor.copy(alpha = 0.7f) else skin.textColor.copy(alpha = 0.3f),
                fontSize = 11.sp,
                fontFamily = skin.fontFamily
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = skin.primaryColor,
                checkedTrackColor = skin.secondaryColor,
                uncheckedThumbColor = skin.textColor,
                uncheckedTrackColor = skin.surfaceColor
            )
        )
    }
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
private fun SettingsPreview() {
    RetroAmpTheme {
        SettingsContent(
            uiState = PlayerUiState(currentSkin = AppSkin.EightBit, visualizerBatterySaver = true),
            onIntent = {},
            onBack = {}
        )
    }
}
