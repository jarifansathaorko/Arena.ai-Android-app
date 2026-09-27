package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.ArenaTab
import com.example.ui.theme.ArenaPrimary

/**
 * Modern Material 3 Navigation Bar for compact (phone portrait) layouts.
 */
@Composable
fun ArenaNavigationBar(
    selectedTab: ArenaTab,
    onTabSelected: (ArenaTab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier.testTag("arena_navigation_bar"),
        tonalElevation = 3.dp
    ) {
        ArenaTab.entries.forEach { tab ->
            val isSelected = selectedTab == tab
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = tab.title,
                        tint = if (isSelected) ArenaPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                label = { Text(tab.title) },
                alwaysShowLabel = true,
                modifier = Modifier.testTag("tab_${tab.name.lowercase()}")
            )
        }
    }
}

/**
 * Modern Material 3 Navigation Rail for expanded (tablet / landscape) layouts.
 */
@Composable
fun ArenaNavigationRail(
    selectedTab: ArenaTab,
    onTabSelected: (ArenaTab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationRail(
        modifier = modifier.testTag("arena_navigation_rail")
    ) {
        Spacer(modifier = Modifier.weight(1f))
        ArenaTab.entries.forEach { tab ->
            val isSelected = selectedTab == tab
            NavigationRailItem(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = tab.title,
                        tint = if (isSelected) ArenaPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                label = { Text(tab.title) },
                alwaysShowLabel = true,
                modifier = Modifier.testTag("rail_tab_${tab.name.lowercase()}")
            )
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}
