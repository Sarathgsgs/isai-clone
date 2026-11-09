package com.isai.player.ui.nowplaying

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.isai.player.R
import com.isai.player.playback.RepeatMode
import com.isai.player.ui.theme.DialShape
import com.isai.player.ui.theme.IsaiTheme

@Composable
fun ControlDial(
    isPlaying: Boolean,
    shuffleEnabled: Boolean,
    repeatMode: RepeatMode,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = DialShape,
        color = IsaiTheme.colors.surface,
        shadowElevation = if (IsaiTheme.colors.isDark) 4.dp else 6.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Shuffle
            IconButton(
                onClick = onToggleShuffle,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = stringResource(
                        id = if (shuffleEnabled) R.string.action_shuffle_on else R.string.action_shuffle_off
                    ),
                    tint = if (shuffleEnabled) IsaiTheme.colors.accent else IsaiTheme.colors.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Previous (Honors 3s restart rule in ViewModel)
            IconButton(
                onClick = onSkipPrevious,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = stringResource(id = R.string.action_previous),
                    tint = IsaiTheme.colors.onBackground,
                    modifier = Modifier.size(36.dp)
                )
            }

            // Play / Pause Main Button with spring scale animation
            val playButtonScale by animateFloatAsState(
                targetValue = if (isPlaying) 1.0f else 0.95f,
                animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
                label = "PlayButtonScale"
            )

            Button(
                onClick = onTogglePlayPause,
                colors = ButtonDefaults.buttonColors(
                    containerColor = IsaiTheme.colors.accent,
                    contentColor = IsaiTheme.colors.onAccent
                ),
                shape = CircleShape,
                modifier = Modifier
                    .size(68.dp)
                    .scale(playButtonScale)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = stringResource(
                        id = if (isPlaying) R.string.action_pause else R.string.action_play
                    ),
                    tint = IsaiTheme.colors.onAccent,
                    modifier = Modifier.size(36.dp)
                )
            }

            // Next
            IconButton(
                onClick = onSkipNext,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = stringResource(id = R.string.action_next),
                    tint = IsaiTheme.colors.onBackground,
                    modifier = Modifier.size(36.dp)
                )
            }

            // Repeat (Cycles OFF -> ALL -> ONE)
            IconButton(
                onClick = onCycleRepeat,
                modifier = Modifier.size(48.dp)
            ) {
                val repeatIcon = when (repeatMode) {
                    RepeatMode.ONE -> Icons.Default.RepeatOne
                    else -> Icons.Default.Repeat
                }
                val repeatTint = when (repeatMode) {
                    RepeatMode.OFF -> IsaiTheme.colors.onSurfaceVariant
                    else -> IsaiTheme.colors.accent
                }
                val repeatDesc = when (repeatMode) {
                    RepeatMode.OFF -> stringResource(id = R.string.action_repeat_off)
                    RepeatMode.ALL -> stringResource(id = R.string.action_repeat_all)
                    RepeatMode.ONE -> stringResource(id = R.string.action_repeat_one)
                }

                Icon(
                    imageVector = repeatIcon,
                    contentDescription = repeatDesc,
                    tint = repeatTint,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
