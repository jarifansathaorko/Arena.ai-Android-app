package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ArenaPrimary
import com.example.ui.theme.ArenaSecondary
import com.example.ui.theme.ArenaSuccess

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
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                // Back & Forward navigation
                IconButton(
                    onClick = onNavigateBack,
                    enabled = canGoBack,
                    modifier = Modifier
                        .size(40.dp)
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
                        .size(40.dp)
                        .testTag("nav_forward_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Forward",
                        tint = if (canGoForward) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Brand Pill + Active Path info
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp)
                    ) {
                        // SSL Lock
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Secure HTTPS",
                            tint = ArenaSuccess,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "arena.ai",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = ArenaPrimary,
                            maxLines = 1
                        )
                        Text(
                            text = if (currentUrl.contains("/leaderboard")) " / leaderboard"
                            else if (currentUrl.contains("/history")) " / history"
                            else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Reload
                IconButton(
                    onClick = onReload,
                    modifier = Modifier
                        .size(40.dp)
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
                        .size(40.dp)
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
                        .size(40.dp)
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
                        .size(40.dp)
                        .testTag("settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "App Settings & Tools",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Quick Navigation Chips
            val scrollState = rememberScrollState()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val isBattle = currentUrl == "https://arena.ai" || currentUrl == "https://arena.ai/" || currentUrl.startsWith("https://arena.ai/?")
                NavChip(
                    label = "⚔️ Arena Battle",
                    isSelected = isBattle,
                    tag = "battle_nav_chip",
                    onClick = { onNavigateToUrl("https://arena.ai/") }
                )

                val isLeaderboard = currentUrl.contains("/leaderboard")
                NavChip(
                    label = "🏆 Leaderboard",
                    isSelected = isLeaderboard,
                    tag = "leaderboard_nav_chip",
                    onClick = { onNavigateToUrl("https://arena.ai/leaderboard") }
                )

                val isHistory = currentUrl.contains("/history")
                NavChip(
                    label = "📜 History",
                    isSelected = isHistory,
                    tag = "history_nav_chip",
                    onClick = { onNavigateToUrl("https://arena.ai/history/search") }
                )

                NavChip(
                    label = "📝 Battle Log",
                    isSelected = false,
                    isSpecial = true,
                    tag = "battle_log_nav_chip",
                    onClick = onOpenBattleTracker
                )

                NavChip(
                    label = "💡 Prompts Library",
                    isSelected = false,
                    isSpecial = true,
                    tag = "prompts_lib_nav_chip",
                    onClick = onOpenPrompts
                )
            }

            // Progress bar
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
            .height(32.dp)
            .then(if (tag.isNotEmpty()) Modifier.testTag(tag) else Modifier)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 10.dp)
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (isSelected || isSpecial) FontWeight.SemiBold else FontWeight.Normal,
                color = contentColor
            )
        }
    }
}
