package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ArenaSubMode
import com.example.ui.theme.ArenaPrimary
import com.example.ui.theme.ArenaSecondary
import com.example.ui.theme.ArenaSuccess
import com.example.ui.theme.ArenaWarning
import com.example.ui.theme.Dimensions

/**
 * Modern native header for the Arena screen.
 * Replaces the browser URL-bar chrome with a sleek, native app header.
 */
@Composable
fun ArenaHeader(
    currentSubMode: ArenaSubMode,
    onSubModeSelected: (ArenaSubMode) -> Unit,
    progress: Float,
    isDesktopMode: Boolean,
    isOnline: Boolean,
    onReload: () -> Unit,
    onToggleDesktop: () -> Unit,
    onResetSession: () -> Unit,
    currentUrl: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top branding & action row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .padding(horizontal = Dimensions.spaceStandard, vertical = Dimensions.spaceXs)
            ) {
                // Brand + Live Status
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = ArenaPrimary.copy(alpha = 0.15f),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.SportsMma,
                                contentDescription = null,
                                tint = ArenaPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(Dimensions.spaceSmall))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Arena.ai",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(Dimensions.spaceSmall))
                            // Online/Offline status dot
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isOnline) ArenaSuccess else ArenaWarning)
                            )
                        }
                        Text(
                            text = if (isOnline) "LMSYS Model Benchmark" else "Offline Mode",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Header Actions: Desktop toggle, Reload, More Menu
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Desktop toggle chip button
                    IconButton(
                        onClick = onToggleDesktop,
                        modifier = Modifier
                            .size(Dimensions.minTouchTarget)
                            .testTag("header_desktop_toggle")
                    ) {
                        Icon(
                            imageVector = if (isDesktopMode) Icons.Default.DesktopWindows else Icons.Default.Smartphone,
                            contentDescription = if (isDesktopMode) "Desktop View Active" else "Mobile View Active",
                            tint = if (isDesktopMode) ArenaSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Reload button
                    IconButton(
                        onClick = onReload,
                        modifier = Modifier
                            .size(Dimensions.minTouchTarget)
                            .testTag("header_reload_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reload Arena page",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Overflow Menu
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier
                                .size(Dimensions.minTouchTarget)
                                .testTag("header_menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Reset Session (Fresh Cookies)") },
                                leadingIcon = {
                                    Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = ArenaWarning)
                                },
                                onClick = {
                                    menuExpanded = false
                                    onResetSession()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Copy Page Link") },
                                leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    clipboardManager.setText(AnnotatedString(currentUrl))
                                    Toast.makeText(context, "Link copied to clipboard", Toast.LENGTH_SHORT).show()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Open in External Browser") },
                                leadingIcon = { Icon(Icons.Default.OpenInBrowser, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    try {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl)))
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "No browser found", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Sub-mode pill selector row (Battle, Leaderboard, History)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimensions.spaceStandard)
                    .padding(bottom = Dimensions.spaceSmall),
                horizontalArrangement = Arrangement.spacedBy(Dimensions.spaceSmall)
            ) {
                ArenaSubMode.entries.forEach { mode ->
                    val isSelected = currentSubMode == mode
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSubModeSelected(mode) },
                        label = {
                            Text(
                                text = mode.title,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        },
                        leadingIcon = {
                            when (mode) {
                                ArenaSubMode.BATTLE -> Icon(
                                    Icons.Default.SportsMma,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isSelected) ArenaPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                ArenaSubMode.LEADERBOARD -> Icon(
                                    Icons.Default.Leaderboard,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isSelected) ArenaPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                ArenaSubMode.HISTORY -> Icon(
                                    Icons.Default.History,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isSelected) ArenaPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ArenaPrimary.copy(alpha = 0.15f),
                            selectedLabelColor = ArenaPrimary
                        ),
                        shape = RoundedCornerShape(Dimensions.radiusPill),
                        modifier = Modifier.testTag("submode_${mode.name.lowercase()}")
                    )
                }
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
