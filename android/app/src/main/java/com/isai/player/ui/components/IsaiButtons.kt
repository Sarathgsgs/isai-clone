package com.isai.player.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.isai.player.data.repository.ThemeMode
import com.isai.player.ui.theme.CardShape
import com.isai.player.ui.theme.ChipShape
import com.isai.player.ui.theme.IsaiTheme

@Composable
fun IsaiPrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = CardShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = IsaiTheme.colors.accent,
            contentColor = Color.White
        ),
        modifier = modifier
    ) {
        content()
    }
}

@Composable
fun IsaiOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = CardShape,
        border = BorderStroke(1.dp, IsaiTheme.colors.accent),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = IsaiTheme.colors.accent
        ),
        modifier = modifier
    ) {
        content()
    }
}

@Composable
fun IsaiChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium
            )
        },
        leadingIcon = leadingIcon,
        shape = ChipShape,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = IsaiTheme.colors.surfaceVariant,
            labelColor = IsaiTheme.colors.onSurfaceVariant,
            selectedContainerColor = IsaiTheme.colors.accent.copy(alpha = 0.2f),
            selectedLabelColor = IsaiTheme.colors.accent
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = if (selected) IsaiTheme.colors.accent else IsaiTheme.colors.surfaceVariant
        ),
        modifier = modifier
    )
}

@Preview(name = "Buttons & Chips - Dark Theme")
@Composable
private fun ButtonsDarkPreview() {
    IsaiTheme(themeMode = ThemeMode.DARK) {
        Surface(color = IsaiTheme.colors.background) {
            IsaiPrimaryButton(
                onClick = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Play All")
            }
        }
    }
}

@Preview(name = "Buttons & Chips - Light Theme")
@Composable
private fun ButtonsLightPreview() {
    IsaiTheme(themeMode = ThemeMode.LIGHT) {
        Surface(color = IsaiTheme.colors.background) {
            IsaiChip(
                selected = true,
                onClick = {},
                label = "Songs",
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

