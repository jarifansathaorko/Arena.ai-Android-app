package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ArenaDanger
import com.example.ui.theme.ArenaPrimary
import com.example.ui.theme.ArenaSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArenaSettingsDialog(
    currentUrl: String,
    isDesktopMode: Boolean,
    textZoom: Int,
    onToggleDesktop: () -> Unit,
    onChangeTextZoom: (Int) -> Unit,
    onResetSession: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var showConfirmReset by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 24.dp)
        ) {
            // Title
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = ArenaPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Arena Settings & Tools",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Display Mode
            Text("Display & Viewport", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isDesktopMode) Icons.Default.DesktopWindows else Icons.Default.Smartphone,
                                contentDescription = null,
                                tint = ArenaPrimary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Desktop Viewport", fontWeight = FontWeight.SemiBold)
                                Text(
                                    if (isDesktopMode) "Shows side-by-side desktop layout" else "Standard mobile responsive layout",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = isDesktopMode,
                            onCheckedChange = { onToggleDesktop() }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    Text("Text Zoom Scale: $textZoom%", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(85, 100, 115, 130).forEach { zoom ->
                            FilterChip(
                                selected = textZoom == zoom,
                                onClick = { onChangeTextZoom(zoom) },
                                label = { Text("$zoom%", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ArenaPrimary, selectedLabelColor = Color.White)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Web Actions
            Text("Session & Web Tools", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                    SettingActionItem(
                        icon = Icons.Default.Share,
                        title = "Copy Current Page Link",
                        subtitle = currentUrl,
                        onClick = {
                            clipboardManager.setText(AnnotatedString(currentUrl))
                            onDismiss()
                        }
                    )

                    SettingActionItem(
                        icon = Icons.Default.OpenInBrowser,
                        title = "Open in External Browser",
                        subtitle = "Open in Chrome / Default Web Browser",
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl))
                            context.startActivity(intent)
                            onDismiss()
                        }
                    )

                    SettingActionItem(
                        icon = Icons.Default.DeleteSweep,
                        title = "Reset Anonymous Session",
                        subtitle = "Clears cookies & local cache to restart fresh voting",
                        iconTint = ArenaDanger,
                        onClick = { showConfirmReset = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // About Arena & LMSYS
            Text("About Platform", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "LMSYS Chatbot Arena",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = ArenaPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Crowdsourced open platform evaluating Large Language Models (LLMs) via blind human preference and Bradley-Terry Elo ratings. Developed by LMSYS Org / UC Berkeley.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showConfirmReset) {
        AlertDialog(
            onDismissRequest = { showConfirmReset = false },
            title = { Text("Reset Session?") },
            text = { Text("This will clear cached data, session cookies, and reload the Arena website for a fresh voting environment.") },
            confirmButton = {
                Button(
                    onClick = {
                        onResetSession()
                        showConfirmReset = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ArenaDanger)
                ) {
                    Text("Clear & Reload")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmReset = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingActionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconTint: Color = ArenaPrimary,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(
                    subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
