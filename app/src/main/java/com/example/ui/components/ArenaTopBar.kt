package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ArenaPrimary
import com.example.ui.theme.ArenaSecondary
import com.example.ui.theme.ArenaSuccess
import com.example.ui.theme.ArenaWarning

/**
 * Slim modern top bar: back/forward, URL pill, and a single overflow menu.
 * Secondary actions live in the menu so the bar never crowds small screens.
 */
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var menuExpanded by remember { mutableStateOf(false) }

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
        shadowElevation = 4.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    enabled = canGoBack,
                    modifier = Modifier
                        .size(48.dp)
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
                        .size(48.dp)
                        .testTag("nav_forward_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Forward",
                        tint = if (canGoForward) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .semantics {
                            contentDescription = "Current page: $pageTitle at $host$pathSuffix"
                        }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp)
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

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("overflow_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More actions",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Reload page") },
                            leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onReload()
                            },
                            modifier = Modifier.testTag("reload_button")
                        )
                        DropdownMenuItem(
                            text = { Text(if (isDesktopMode) "Switch to mobile view" else "Switch to desktop view") },
                            leadingIcon = {
                                Icon(
                                    if (isDesktopMode) Icons.Default.DesktopWindows else Icons.Default.Smartphone,
                                    contentDescription = null,
                                    tint = if (isDesktopMode) ArenaSecondary else LocalContentColor.current
                                )
                            },
                            trailingIcon = {
                                if (isDesktopMode) {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                }
                            },
                            onClick = {
                                menuExpanded = false
                                onToggleDesktop()
                            },
                            modifier = Modifier.testTag("desktop_toggle_button")
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Benchmark prompts") },
                            leadingIcon = { Icon(Icons.Default.Lightbulb, contentDescription = null, tint = ArenaPrimary) },
                            onClick = {
                                menuExpanded = false
                                onOpenPrompts()
                            },
                            modifier = Modifier.testTag("prompts_button")
                        )
                        DropdownMenuItem(
                            text = { Text("Battle log") },
                            leadingIcon = { Icon(Icons.Default.EditNote, contentDescription = null, tint = ArenaPrimary) },
                            onClick = {
                                menuExpanded = false
                                onOpenBattleTracker()
                            },
                            modifier = Modifier.testTag("battle_log_menu_item")
                        )
                        DropdownMenuItem(
                            text = { Text("Settings & tools") },
                            leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onOpenSettings()
                            },
                            modifier = Modifier.testTag("settings_button")
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Copy page link") },
                            leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                clipboardManager.setText(AnnotatedString(currentUrl))
                                Toast.makeText(context, "Link copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.testTag("copy_link_menu_item")
                        )
                        DropdownMenuItem(
                            text = { Text("Open in browser") },
                            leadingIcon = { Icon(Icons.Default.OpenInBrowser, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                try {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl)))
                                } catch (_: Exception) {
                                    Toast.makeText(context, "No browser app found", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.testTag("open_browser_menu_item")
                        )
                    }
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
