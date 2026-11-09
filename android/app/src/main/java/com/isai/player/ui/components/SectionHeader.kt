package com.isai.player.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.isai.player.data.repository.ThemeMode
import com.isai.player.ui.theme.IsaiTheme

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = IsaiTheme.colors.onBackground
        )

        if (actionLabel != null && onActionClick != null) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = IsaiTheme.colors.accent,
                modifier = Modifier.clickable(onClick = onActionClick)
            )
        }
    }
}

@Preview(name = "SectionHeader - Dark Theme")
@Composable
private fun SectionHeaderDarkPreview() {
    IsaiTheme(themeMode = ThemeMode.DARK) {
        Surface(color = IsaiTheme.colors.background) {
            SectionHeader(
                title = "Played Recently",
                actionLabel = "View More",
                onActionClick = {}
            )
        }
    }
}

@Preview(name = "SectionHeader - Light Theme")
@Composable
private fun SectionHeaderLightPreview() {
    IsaiTheme(themeMode = ThemeMode.LIGHT) {
        Surface(color = IsaiTheme.colors.background) {
            SectionHeader(
                title = "Songs",
                actionLabel = "Shuffle",
                onActionClick = {}
            )
        }
    }
}

