package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

/**
 * Modern bottom navigation: the three Arena web destinations plus the two
 * native tools (battle log, prompts library). Replaces the cramped top chips.
 */
@Composable
fun ArenaBottomBar(
    currentUrl: String,
    onNavigateToUrl: (String) -> Unit,
    onOpenBattleTracker: () -> Unit,
    onOpenPrompts: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isBattle = remember(currentUrl) {
        currentUrl == "https://arena.ai" ||
                currentUrl == "https://arena.ai/" ||
                currentUrl.startsWith("https://arena.ai/?")
    }
    val isLeaderboard = remember(currentUrl) { currentUrl.contains("/leaderboard") }
    val isHistory = remember(currentUrl) { currentUrl.contains("/history") }

    NavigationBar(modifier = modifier) {
        NavigationBarItem(
            selected = isBattle,
            onClick = { onNavigateToUrl("https://arena.ai/") },
            icon = { Icon(Icons.Default.SportsMma, contentDescription = null) },
            label = { Text("Battle") },
            alwaysShowLabel = true,
            modifier = Modifier.testTag("battle_nav_chip")
        )
        NavigationBarItem(
            selected = isLeaderboard,
            onClick = { onNavigateToUrl("https://arena.ai/leaderboard") },
            icon = { Icon(Icons.Default.EmojiEvents, contentDescription = null) },
            label = { Text("Ranks") },
            alwaysShowLabel = true,
            modifier = Modifier.testTag("leaderboard_nav_chip")
        )
        NavigationBarItem(
            selected = isHistory,
            onClick = { onNavigateToUrl("https://arena.ai/history/search") },
            icon = { Icon(Icons.Default.History, contentDescription = null) },
            label = { Text("History") },
            alwaysShowLabel = true,
            modifier = Modifier.testTag("history_nav_chip")
        )
        NavigationBarItem(
            selected = false,
            onClick = onOpenBattleTracker,
            icon = { Icon(Icons.Default.EditNote, contentDescription = null) },
            label = { Text("Log") },
            alwaysShowLabel = true,
            modifier = Modifier.testTag("battle_log_nav_chip")
        )
        NavigationBarItem(
            selected = false,
            onClick = onOpenPrompts,
            icon = { Icon(Icons.Default.Lightbulb, contentDescription = null) },
            label = { Text("Prompts") },
            alwaysShowLabel = true,
            modifier = Modifier.testTag("prompts_lib_nav_chip")
        )
    }
}
