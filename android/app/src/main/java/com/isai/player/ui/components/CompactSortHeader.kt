package com.isai.player.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class SortBy { DATE_ADDED, TITLE, ARTIST }
enum class SortOrder { ASCENDING, DESCENDING }

/**
 * Compact sort control matching Samsung Music One UI:
 * - Clicking the arrow toggles between Ascending and Descending.
 * - Clicking the text label cycles through Date Added -> Name -> Artist.
 */
@Composable
fun CompactSortHeader(
    sortBy: SortBy,
    sortOrder: SortOrder,
    onToggleOrder: () -> Unit,
    onCycleSortBy: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Arrow Button: Toggles Ascending / Descending
        IconButton(
            onClick = onToggleOrder,
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
        ) {
            Text(
                text = if (sortOrder == SortOrder.ASCENDING) "↑" else "↓",
                color = Color(0xFFFF8A3D),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Text Label: Clicking cycles Date Added -> Name -> Artist
        Text(
            text = when (sortBy) {
                SortBy.DATE_ADDED -> "Date added"
                SortBy.TITLE -> "Name"
                SortBy.ARTIST -> "Artist"
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            modifier = Modifier
                .clip(CircleShape)
                .clickable { onCycleSortBy() }
                .padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}
