package com.isai.player.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.isai.player.R
import com.isai.player.data.repository.ThemeMode
import com.isai.player.ui.theme.IsaiTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IsaiTopBar(
    modifier: Modifier = Modifier,
    title: String = stringResource(id = R.string.app_name),
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            if (title.equals("Isai", ignoreCase = true)) {
                Text(
                    text = androidx.compose.ui.text.buildAnnotatedString {
                        append("Isa")
                        androidx.compose.ui.text.withStyle(
                            androidx.compose.ui.text.SpanStyle(color = androidx.compose.ui.graphics.Color(0xFFFF8A3D))
                        ) {
                            append("i")
                        }
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = IsaiTheme.colors.onBackground
                )
            } else {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = IsaiTheme.colors.onBackground
                )
            }
        },
        navigationIcon = navigationIcon,
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = IsaiTheme.colors.background,
            titleContentColor = IsaiTheme.colors.onBackground,
            actionIconContentColor = IsaiTheme.colors.onBackground
        ),
        modifier = modifier
    )
}

@Preview(name = "IsaiTopBar - Dark Theme")
@Composable
private fun IsaiTopBarDarkPreview() {
    IsaiTheme(themeMode = ThemeMode.DARK) {
        Surface(color = IsaiTheme.colors.background) {
            IsaiTopBar(
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Search, contentDescription = null)
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Settings, contentDescription = null)
                    }
                }
            )
        }
    }
}

@Preview(name = "IsaiTopBar - Light Theme")
@Composable
private fun IsaiTopBarLightPreview() {
    IsaiTheme(themeMode = ThemeMode.LIGHT) {
        Surface(color = IsaiTheme.colors.background) {
            IsaiTopBar(
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Search, contentDescription = null)
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Settings, contentDescription = null)
                    }
                }
            )
        }
    }
}

