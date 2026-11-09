package com.isai.player.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.isai.player.R
import com.isai.player.data.repository.ThemeMode
import com.isai.player.ui.theme.IsaiTheme

enum class BottomBarTab(val titleResId: Int, val icon: ImageVector) {
    HOME(R.string.nav_tab_home, Icons.Default.Home),
    LIBRARY(R.string.nav_tab_library, Icons.Default.LibraryMusic),
    FAVORITES(R.string.nav_tab_favorites, Icons.Default.Favorite),
    SEARCH(R.string.nav_tab_search, Icons.Default.Search)
}

@Composable
fun IsaiBottomBar(
    currentTab: BottomBarTab,
    onTabSelected: (BottomBarTab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        containerColor = IsaiTheme.colors.surface,
        tonalElevation = 8.dp
    ) {
        BottomBarTab.entries.forEach { tab ->
            val selected = tab == currentTab
            NavigationBarItem(
                selected = selected,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = stringResource(id = tab.titleResId)
                    )
                },
                label = {
                    Text(text = stringResource(id = tab.titleResId))
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = IsaiTheme.colors.accent,
                    selectedTextColor = IsaiTheme.colors.accent,
                    indicatorColor = IsaiTheme.colors.accent.copy(alpha = 0.15f),
                    unselectedIconColor = IsaiTheme.colors.onSurfaceVariant,
                    unselectedTextColor = IsaiTheme.colors.onSurfaceVariant
                )
            )
        }
    }
}

@Preview(name = "IsaiBottomBar - Dark Theme")
@Composable
private fun IsaiBottomBarDarkPreview() {
    IsaiTheme(themeMode = ThemeMode.DARK) {
        Surface {
            IsaiBottomBar(
                currentTab = BottomBarTab.HOME,
                onTabSelected = {}
            )
        }
    }
}

@Preview(name = "IsaiBottomBar - Light Theme")
@Composable
private fun IsaiBottomBarLightPreview() {
    IsaiTheme(themeMode = ThemeMode.LIGHT) {
        Surface {
            IsaiBottomBar(
                currentTab = BottomBarTab.LIBRARY,
                onTabSelected = {}
            )
        }
    }
}

