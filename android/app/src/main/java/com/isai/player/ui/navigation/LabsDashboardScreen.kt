package com.isai.player.ui.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.isai.player.R
import com.isai.player.data.repository.ThemeMode
import com.isai.player.ui.theme.CardShape
import com.isai.player.ui.theme.IsaiTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabsDashboardScreen(
    onBackClick: () -> Unit,
    onNavigateToLab: (LabsRoute) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.nav_screen_labs)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.global_action_cancel),
                            tint = IsaiTheme.colors.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = IsaiTheme.colors.background,
                    titleContentColor = IsaiTheme.colors.onBackground
                )
            )
        },
        containerColor = IsaiTheme.colors.background,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            LabItemCard(
                title = "Prompt A & B: Tonearm Lab",
                subtitle = "Canvas tonearm rendering, geometry math & gesture seeking",
                icon = Icons.Default.Timeline,
                onClick = { onNavigateToLab(LabsRoute.TonearmLab) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            LabItemCard(
                title = "Prompt C: Vinyl Disc Lab",
                subtitle = "Grooves, sheen, 33⅓ RPM spin physics & fallback gradients",
                icon = Icons.Default.Album,
                onClick = { onNavigateToLab(LabsRoute.VinylDiscLab) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            LabItemCard(
                title = "Prompt D: Vinyl Stage Lab",
                subtitle = "Disc + tonearm assembly, background blur & track transitions",
                icon = Icons.Default.Layers,
                onClick = { onNavigateToLab(LabsRoute.VinylStageLab) }
            )
        }
    }
}

@Composable
private fun LabItemCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = IsaiTheme.colors.surface),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = IsaiTheme.colors.accent
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = IsaiTheme.colors.onBackground
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = IsaiTheme.colors.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = IsaiTheme.colors.onSurfaceVariant
            )
        }
    }
}

@Preview(name = "LabsDashboard - Dark Theme")
@Composable
private fun LabsDashboardDarkPreview() {
    IsaiTheme(themeMode = ThemeMode.DARK) {
        Surface(color = IsaiTheme.colors.background) {
            LabsDashboardScreen(onBackClick = {}, onNavigateToLab = {})
        }
    }
}

@Preview(name = "LabsDashboard - Light Theme")
@Composable
private fun LabsDashboardLightPreview() {
    IsaiTheme(themeMode = ThemeMode.LIGHT) {
        Surface(color = IsaiTheme.colors.background) {
            LabsDashboardScreen(onBackClick = {}, onNavigateToLab = {})
        }
    }
}

