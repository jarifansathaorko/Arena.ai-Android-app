package com.example.ui.components

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ArenaPrimary
import com.example.ui.theme.ArenaSecondary
import com.example.ui.theme.ArenaSuccess
import com.example.ui.theme.ArenaWarning

private data class QuickNav(
    val label: String,
    val icon: ImageVector,
    val tag: String,
    val isSelected: Boolean,
    val isSpecial: Boolean = false,
    val onClick: () -> Unit
)

@Composable
fun ArenaTopBar(
    currentUrl: String,
    pageTitle: String,
    progress: Float,
    isDesktopMode: Boolean,
    canGoBack: Boolean,
    canGoForward: Boolean,
    onNavigateBack: () -> Unit,
    onNavigateForward: () -> Unit,
    onReload: () -> Unit,
    onToggleDesktop: () -> Unit,
    onOpenPrompts: () -> Unit,
    onOpenBattleTracker: () -> Unit,
    onOpenSettings: () -> Unit,
    onNavigateToUrl: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uri = remember(currentUrl) {
        runCatching { Uri.parse(currentUrl) }.getOrNull()
    }
    val host = uri?.host?.removePrefix("www.") ?: "arena.ai"
    val pathSuffix = remember(currentUrl) {
        when {
            currentUrl.contains("/leaderboard") -> " / leaderboard"
            currentUrl.contains("/history") -> " / history"
            else -> ""
        }
    }
    val isSecure = currentUrl.startsWith("https://")

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top Primary Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            ) {
                // Back & Forward navigation
                IconButton(
                    onClick = onNavigateBack,
                    enabled = canGoBack,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("nav_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = if (canGoBack) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                    )
                }

                IconButton(
                    onClick = onNavigateForward,
                    enabled = canGoForward,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("nav_forward_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Forward",
                        tint = if (canGoForward) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                    )
                }

                // Brand Pill + Active Path info
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 40.dp)
                        .semantics {
                            contentDescription = "Current page: $pageTitle at $host$pathSuffix"
                        }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp)
                    ) {
                        Icon(
                            imageVector = if (isSecure) Icons.Default.Lock else Icons.Default.Warning,
                            contentDescription = if (isSecure) "Secure HTTPS connection" else "Not a secure connection",
                            tint = if (isSecure) ArenaSuccess else ArenaWarning,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = host,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = ArenaPrimary,
                            maxLines = 1
                        )
                        Text(
                            text = pathSuffix,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Reload
                IconButton(
                    onClick = onReload,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("reload_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reload Page",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Desktop / Mobile Mode Toggle
                IconButton(
                    onClick = onToggleDesktop,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("desktop_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isDesktopMode) Icons.Default.DesktopWindows else Icons.Default.Smartphone,
                        contentDescription = if (isDesktopMode) "Switch to Mobile View" else "Switch to Desktop View",
                        tint = if (isDesktopMode) ArenaSecondary else MaterialTheme.colorScheme.onSurface
                    )
                }

                // Benchmark Prompts Sheet Action
                IconButton(
                    onClick = onOpenPrompts,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("prompts_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = "Benchmark Prompts",
                        tint = ArenaPrimary
                    )
                }

                // Settings & Tools
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "App Settings & Tools",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Quick Navigation Chips (lazy for cheap scroll + item recycling)
            val navItems = remember(currentUrl) {
                val isBattle = currentUrl == "https://arena.ai" ||
                        currentUrl == "https://arena.ai/" ||
                        currentUrl.startsWith("https://arena.ai/?")
                val isLeaderboard = currentUrl.contains("/leaderboard")
                val isHistory = currentUrl.contains("/history")
                listOf(
                    QuickNav("Arena Battle", Icons.Default.SportsMma, "battle_nav_chip", isBattle) {
                        onNavigateToUrl("https://arena.ai/")
                    },
                    QuickNav("Leaderboard", Icons.Default.EmojiEvents, "leaderboard_nav_chip", isLeaderboard) {
                        onNavigateToUrl("https://arena.ai/leaderboard")
                    },
                    QuickNav("History", Icons.Default.History, "history_nav_chip", isHistory) {
                        onNavigateToUrl("https://arena.ai/history/search")
                    },
                    QuickNav("Battle Log", Icons.Default.EditNote, "battle_log_nav_chip", false, true, onOpenBattleTracker),
                    QuickNav("Prompts Library", Icons.Default.Lightbulb, "prompts_lib_nav_chip", false, true, onOpenPrompts)
                )
            }
            LazyRow(
                verticalAlignment = Alignment.CenterVertically,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(navItems, key = { it.tag }) { item ->
                    NavChip(
                        label = item.label,
                        icon = item.icon,
                        isSelected = item.isSelected,
                        isSpecial = item.isSpecial,
                        tag = item.tag,
                        onClick = item.onClick
                    )
                }
            }

            // Progress bar (throttled upstream in ViewModel.setProgress)
            AnimatedVisibility(visible = progress in 0.01f..0.99f) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.5.dp),
                    color = ArenaPrimary,
                    trackColor = Color.Transparent
                )
            }
        }
    }
}

@Composable
private fun NavChip(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    isSpecial: Boolean = false,
    tag: String = "",
    onClick: () -> Unit
) {
    val containerColor by animateColorAsState(
        targetValue = when {
            isSelected -> ArenaPrimary
            isSpecial -> MaterialTheme.colorScheme.surfaceVariant
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        },
        label = "chipColor"
    )

    val contentColor = when {
        isSelected -> Color.White
        isSpecial -> ArenaPrimary
        else -> MaterialTheme.colorScheme.onSurface
    }

    Surface(
        onClick = onClick,
        color = containerColor,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .height(34.dp)
            .clip(RoundedCornerShape(16.dp))
            .semantics { role = Role.Tab }
            .then(if (tag.isNotEmpty()) Modifier.testTag(tag) else Modifier)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (isSelected || isSpecial) FontWeight.SemiBold else FontWeight.Normal,
                color = contentColor
            )
        }
    }
}
