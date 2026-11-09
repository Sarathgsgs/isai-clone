package com.isai.player.ui.nowplaying

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.isai.player.R
import com.isai.player.ui.theme.IsaiTheme
import com.isai.player.util.Formatters

@Composable
fun SleepTimerDialog(
    remainingMs: Long,
    isStopAtEndOfTrack: Boolean,
    onStartTimer: (durationMs: Long, stopAtEndOfTrack: Boolean) -> Unit,
    onCancelTimer: () -> Unit,
    onDismissRequest: () -> Unit
) {
    val isTimerActive = remainingMs > 0L || isStopAtEndOfTrack

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = IsaiTheme.colors.surface,
        icon = {
            Icon(
                imageVector = Icons.Default.Bedtime,
                contentDescription = null,
                tint = IsaiTheme.colors.accent,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = stringResource(id = R.string.sleep_timer_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = IsaiTheme.colors.onBackground
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (isTimerActive) {
                    val statusText = if (isStopAtEndOfTrack) {
                        stringResource(id = R.string.sleep_timer_preset_end_of_track)
                    } else {
                        stringResource(
                            id = R.string.sleep_timer_active_countdown,
                            Formatters.formatDurationMs(remainingMs)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = IsaiTheme.colors.accent,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = IsaiTheme.colors.accent
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            onCancelTimer()
                            onDismissRequest()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = IsaiTheme.colors.accent,
                            contentColor = IsaiTheme.colors.onAccent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = stringResource(id = R.string.sleep_timer_action_cancel))
                    }
                } else {
                    TimerPresetRow(
                        label = stringResource(id = R.string.sleep_timer_preset_15m),
                        onClick = {
                            onStartTimer(15 * 60 * 1000L, false)
                            onDismissRequest()
                        }
                    )
                    TimerPresetRow(
                        label = stringResource(id = R.string.sleep_timer_preset_30m),
                        onClick = {
                            onStartTimer(30 * 60 * 1000L, false)
                            onDismissRequest()
                        }
                    )
                    TimerPresetRow(
                        label = stringResource(id = R.string.sleep_timer_preset_45m),
                        onClick = {
                            onStartTimer(45 * 60 * 1000L, false)
                            onDismissRequest()
                        }
                    )
                    TimerPresetRow(
                        label = stringResource(id = R.string.sleep_timer_preset_60m),
                        onClick = {
                            onStartTimer(60 * 60 * 1000L, false)
                            onDismissRequest()
                        }
                    )
                    TimerPresetRow(
                        label = stringResource(id = R.string.sleep_timer_preset_end_of_track),
                        onClick = {
                            onStartTimer(0L, true)
                            onDismissRequest()
                        }
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(
                    text = stringResource(id = R.string.sleep_timer_action_close),
                    color = IsaiTheme.colors.onSurfaceVariant
                )
            }
        }
    )
}

@Composable
private fun TimerPresetRow(
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Timer,
            contentDescription = null,
            tint = IsaiTheme.colors.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = IsaiTheme.colors.onBackground
        )
    }
}

