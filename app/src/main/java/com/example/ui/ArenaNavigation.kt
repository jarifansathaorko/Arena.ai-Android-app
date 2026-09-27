package com.example.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsMma
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SportsMma
import androidx.compose.ui.graphics.vector.ImageVector

enum class ArenaTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    ARENA("Arena", Icons.Default.SportsMma, Icons.Outlined.SportsMma),
    PROMPTS("Prompts", Icons.Default.Lightbulb, Icons.Outlined.Lightbulb),
    BATTLES("Battles", Icons.Default.EmojiEvents, Icons.Outlined.EmojiEvents),
    SETTINGS("Settings", Icons.Default.Settings, Icons.Outlined.Settings)
}

enum class ArenaSubMode(
    val title: String,
    val url: String
) {
    BATTLE("Battle", "https://arena.ai/"),
    LEADERBOARD("Leaderboard", "https://arena.ai/leaderboard"),
    HISTORY("History", "https://arena.ai/history/search")
}

sealed interface WebViewCommand {
    data object Reload : WebViewCommand
    data object GoBack : WebViewCommand
    data object GoForward : WebViewCommand
    data class InjectPrompt(val script: String) : WebViewCommand
    data object ClearSession : WebViewCommand
}
