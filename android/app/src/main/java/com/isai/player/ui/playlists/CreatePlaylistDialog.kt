package com.isai.player.ui.playlists

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.isai.player.R
import com.isai.player.ui.theme.IsaiTheme

@Composable
fun CreatePlaylistDialog(
    initialName: String = "",
    isRename: Boolean = false,
    onConfirm: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    var playlistName by remember { mutableStateOf(initialName) }
    val isConfirmEnabled = playlistName.trim().isNotBlank()

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = IsaiTheme.colors.surface,
        title = {
            Text(
                text = stringResource(
                    id = if (isRename) {
                        R.string.playlist_dialog_rename_title
                    } else {
                        R.string.playlist_dialog_create_title
                    }
                ),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = IsaiTheme.colors.onBackground
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = playlistName,
                    onValueChange = { playlistName = it },
                    placeholder = {
                        Text(
                            text = stringResource(id = R.string.playlist_dialog_name_hint),
                            color = IsaiTheme.colors.onSurfaceVariant
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = IsaiTheme.colors.background,
                        unfocusedContainerColor = IsaiTheme.colors.background,
                        focusedBorderColor = IsaiTheme.colors.accent,
                        unfocusedBorderColor = IsaiTheme.colors.surfaceVariant,
                        focusedTextColor = IsaiTheme.colors.onBackground,
                        unfocusedTextColor = IsaiTheme.colors.onBackground,
                        cursorColor = IsaiTheme.colors.accent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (isConfirmEnabled) {
                        onConfirm(playlistName.trim())
                    }
                },
                enabled = isConfirmEnabled
            ) {
                Text(
                    text = stringResource(
                        id = if (isRename) {
                            R.string.playlist_dialog_rename_btn
                        } else {
                            R.string.playlist_dialog_create_btn
                        }
                    ),
                    color = if (isConfirmEnabled) IsaiTheme.colors.accent else IsaiTheme.colors.onSurfaceVariant
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(
                    text = stringResource(id = R.string.playlist_dialog_cancel_btn),
                    color = IsaiTheme.colors.onSurfaceVariant
                )
            }
        }
    )
}

