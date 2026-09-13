package digital.tonima.retroamp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import digital.tonima.retroamp.R
import digital.tonima.retroamp.core.model.Track
import digital.tonima.retroamp.ui.theme.AppSkin

@Composable
fun RetroButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    skin: AppSkin = AppSkin.Winamp
) {
    val buttonText = if (skin.forceAllCaps) text.uppercase() else text
    val isEightBit = skin == AppSkin.EightBit
    
    Box(
        modifier = modifier
            .padding(if (isEightBit) 2.dp else 0.dp) // Space for shadow
            .background(
                if (isEightBit) skin.textColor.copy(alpha = 0.5f) else Color.Transparent,
                skin.buttonShape
            ) // Blocky shadow for 8-bit
            .padding(bottom = if (isEightBit) 2.dp else 0.dp, end = if (isEightBit) 2.dp else 0.dp)
            .size(width = 60.dp, height = 30.dp)
            .clip(skin.buttonShape)
            .border(if (isEightBit) 3.dp else 2.dp, skin.textColor, skin.buttonShape)
            .background(if (enabled) skin.primaryColor else Color.Gray, skin.buttonShape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = skin.backgroundColor,
                modifier = Modifier.size(if (isEightBit) 18.dp else 20.dp)
            )
        } else {
            Text(
                text = buttonText,
                color = skin.backgroundColor,
                fontSize = if (isEightBit) 10.sp else 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = skin.fontFamily
            )
        }
    }
}

@Composable
fun SegmentedDisplay(
    text: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    skin: AppSkin = AppSkin.Winamp
) {
    val isEightBit = skin == AppSkin.EightBit
    val displayText = if (skin.forceAllCaps) text.uppercase() else text
    val displayLabel = if (skin.forceAllCaps) label?.uppercase() else label

    Box(
        modifier = modifier
            .background(Color.Black, skin.buttonShape)
            .border(1.dp, skin.textColor, skin.buttonShape)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            if (displayLabel != null) {
                Text(
                    text = displayLabel,
                    color = skin.accentColor.copy(alpha = 0.7f),
                    fontSize = if (isEightBit) 7.sp else 10.sp,
                    fontFamily = skin.fontFamily,
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
            Text(
                text = displayText,
                color = skin.accentColor,
                fontSize = if (isEightBit) 14.sp else 20.sp,
                fontFamily = skin.fontFamily,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun RetroSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    skin: AppSkin = AppSkin.Winamp
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        colors = SliderDefaults.colors(
            thumbColor = skin.primaryColor,
            activeTrackColor = skin.secondaryColor,
            inactiveTrackColor = skin.surfaceColor
        )
    )
}

@Composable
fun TrackItem(
    track: Track,
    isSelected: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    skin: AppSkin = AppSkin.Winamp
) {
    val isEightBit = skin == AppSkin.EightBit
    val titleText = if (skin.forceAllCaps) "${track.title} - ${track.artist}".uppercase() else "${track.title} - ${track.artist}"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(if (isSelected) skin.secondaryColor.copy(alpha = 0.3f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = track.coverArtData ?: track.coverArtUrl,
            contentDescription = null,
            placeholder = painterResource(R.drawable.placeholder),
            error = painterResource(R.drawable.placeholder),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(40.dp)
                .border(1.dp, skin.textColor)
        )
        
        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = titleText,
            color = if (isSelected) skin.accentColor else skin.textColor,
            fontSize = if (isEightBit) 9.sp else 12.sp,
            fontFamily = skin.fontFamily,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = formatDuration(track.durationMs),
            color = if (isSelected) skin.accentColor else skin.textColor,
            fontSize = if (isEightBit) 9.sp else 12.sp,
            fontFamily = skin.fontFamily,
        )

        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = if (skin.forceAllCaps) "REMOVE" else "Remove from playlist",
                tint = if (isSelected) skin.accentColor else skin.textColor,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
