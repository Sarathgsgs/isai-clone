package com.isai.player.ui.nowplaying

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.isai.player.R
import com.isai.player.ui.theme.IsaiTheme
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsOffsetDialog(
    currentOffsetMs: Int,
    onOffsetChange: (Int) -> Unit,
    onDismissRequest: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = IsaiTheme.colors.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(id = R.string.lyrics_offset_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = IsaiTheme.colors.onBackground
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = stringResource(id = R.string.lyrics_offset_desc),
                style = MaterialTheme.typography.bodySmall,
                color = IsaiTheme.colors.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Offset display
            val seconds = currentOffsetMs / 1000f
            val sign = if (seconds > 0) "+" else ""
            Text(
                text = String.format(Locale.US, "%s%.1fs", sign, seconds),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color = if (currentOffsetMs != 0) IsaiTheme.colors.accent else IsaiTheme.colors.onBackground
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Coarse adjustment buttons: -0.5s / +0.5s
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { onOffsetChange(currentOffsetMs - 500) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("-0.5s", fontWeight = FontWeight.Bold, color = IsaiTheme.colors.onBackground)
                }

                Button(
                    onClick = { onOffsetChange(0) },
                    enabled = currentOffsetMs != 0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = IsaiTheme.colors.surfaceVariant,
                        contentColor = IsaiTheme.colors.onBackground
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(id = R.string.lyrics_offset_reset), fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = { onOffsetChange(currentOffsetMs + 500) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("+0.5s", fontWeight = FontWeight.Bold, color = IsaiTheme.colors.onBackground)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Fine adjustment buttons: -0.1s / +0.1s
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(
                    onClick = { onOffsetChange(currentOffsetMs - 100) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("-0.1s", color = IsaiTheme.colors.onSurfaceVariant)
                }

                Spacer(modifier = Modifier.weight(1f))

                TextButton(
                    onClick = { onOffsetChange(currentOffsetMs + 100) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("+0.1s", color = IsaiTheme.colors.onSurfaceVariant)
                }
            }
        }
    }
}

