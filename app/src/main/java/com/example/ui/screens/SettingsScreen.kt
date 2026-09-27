package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ArenaDanger
import com.example.ui.theme.ArenaPrimary
import com.example.ui.theme.Dimensions
import com.example.ui.theme.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    dynamicColor: Boolean,
    isDesktopMode: Boolean,
    textZoom: Int,
    currentUrl: String,
    onThemeModeChanged: (ThemeMode) -> Unit,
    onDynamicColorChanged: (Boolean) -> Unit,
    onToggleDesktop: () -> Unit,
    onChangeTextZoom: (Int) -> Unit,
    onResetSession: () -> Unit,
    onClearBattles: () -> Unit,
    onShowSnackbar: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var showConfirmReset by remember { mutableStateOf(false) }
    var showConfirmClearBattles by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimensions.spaceStandard, vertical = Dimensions.spaceMedium)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = ArenaPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(Dimensions.spaceSmall))
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(Dimensions.spaceStandard),
            verticalArrangement = Arrangement.spacedBy(Dimensions.spaceStandard)
        ) {
            // Section 1: Appearance & Theme
            SettingsSectionHeader(title = "Appearance & Theme", icon = Icons.Outlined.Palette)
            Card(
                shape = RoundedCornerShape(Dimensions.radiusCard),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(Dimensions.spaceStandard)) {
                    Text(
                        text = "Color Theme",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(Dimensions.spaceSmall))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Dimensions.spaceSmall),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ThemeMode.entries.forEach { mode ->
                            val isSelected = themeMode == mode
                            FilterChip(
                                selected = isSelected,
                                onClick = { onThemeModeChanged(mode) },
                                label = {
                                    Text(
                                        when (mode) {
                                            ThemeMode.SYSTEM -> "System"
                                            ThemeMode.DARK -> "Dark"
                                            ThemeMode.LIGHT -> "Light"
                                        }
                                    )
                                },
                                leadingIcon = {
                                    when (mode) {
                                        ThemeMode.SYSTEM -> Icon(Icons.Default.BrightnessAuto, contentDescription = null, modifier = Modifier.size(16.dp))
                                        ThemeMode.DARK -> Icon(Icons.Default.DarkMode, contentDescription = null, modifier = Modifier.size(16.dp))
                                        ThemeMode.LIGHT -> Icon(Icons.Default.LightMode, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ArenaPrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = Dimensions.spaceMedium),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Dynamic Color (Material You)", fontWeight = FontWeight.SemiBold)
                                Text(
                                    "Adapt color palette from device wallpaper",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = dynamicColor,
                                onCheckedChange = onDynamicColorChanged
                            )
                        }
                    }
                }
            }

            // Section 2: Arena Viewport & Text
            SettingsSectionHeader(title = "Arena Viewport & Scaling", icon = Icons.Outlined.Tune)
            Card(
                shape = RoundedCornerShape(Dimensions.radiusCard),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(Dimensions.spaceStandard)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Desktop Viewport Mode", fontWeight = FontWeight.SemiBold)
                            Text(
                                if (isDesktopMode) "Shows side-by-side desktop comparison view"
                                else "Mobile responsive single-column layout",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isDesktopMode,
                            onCheckedChange = { onToggleDesktop() },
                            modifier = Modifier.testTag("desktop_mode_switch")
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = Dimensions.spaceMedium),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )

                    Text(
                        text = "Text Zoom Scale: $textZoom%",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(Dimensions.spaceSmall))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Dimensions.spaceSmall),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(85, 100, 115, 130).forEach { zoom ->
                            FilterChip(
                                selected = textZoom == zoom,
                                onClick = { onChangeTextZoom(zoom) },
                                label = { Text("$zoom%", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ArenaPrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            // Section 3: Session & Cache
            SettingsSectionHeader(title = "Session & Data Management", icon = Icons.Outlined.Security)
            Card(
                shape = RoundedCornerShape(Dimensions.radiusCard),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(Dimensions.spaceStandard)) {
                    SettingsRowItem(
                        icon = Icons.Outlined.DeleteSweep,
                        title = "Reset Anonymous Voting Session",
                        subtitle = "Clears cookies and web storage to restart fresh voting environment",
                        iconTint = ArenaDanger,
                        onClick = { showConfirmReset = true }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = Dimensions.spaceSmall),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                    )
                    SettingsRowItem(
                        icon = Icons.Outlined.Share,
                        title = "Copy Current Arena Link",
                        subtitle = currentUrl,
                        onClick = {
                            clipboardManager.setText(AnnotatedString(currentUrl))
                            onShowSnackbar("Link copied to clipboard")
                        }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = Dimensions.spaceSmall),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                    )
                    SettingsRowItem(
                        icon = Icons.Outlined.OpenInBrowser,
                        title = "Open in External Browser",
                        subtitle = "Open Arena in system browser",
                        onClick = {
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl)))
                            } catch (_: Exception) {
                                Toast.makeText(context, "No browser found", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = Dimensions.spaceSmall),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                    )
                    SettingsRowItem(
                        icon = Icons.Outlined.DeleteForever,
                        title = "Clear Battle History",
                        subtitle = "Deletes all locally recorded battles and resets scorecard",
                        iconTint = ArenaDanger,
                        onClick = { showConfirmClearBattles = true }
                    )
                }
            }

            // Section 4: About Platform & Mission
            SettingsSectionHeader(title = "About LMSYS Chatbot Arena", icon = Icons.Outlined.Info)
            Card(
                shape = RoundedCornerShape(Dimensions.radiusCard),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(Dimensions.spaceStandard),
                    verticalArrangement = Arrangement.spacedBy(Dimensions.spaceSmall)
                ) {
                    Text(
                        text = "Crowdsourced Open LLM Evaluation",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ArenaPrimary
                    )
                    Text(
                        text = "Chatbot Arena is an open research platform run by LMSYS Org (Large Model Systems Organization) and UC Berkeley. It benchmarks large language models through blind pairwise comparisons, calculating accurate Bradley-Terry Elo ratings based on over 2 million human votes.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(Dimensions.spaceSmall))
                    Text(
                        text = "Arena AI Companion • Production Grade v1.0",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimensions.space2xl))
        }
    }

    // Reset Session Confirmation Dialog
    if (showConfirmReset) {
        AlertDialog(
            onDismissRequest = { showConfirmReset = false },
            title = { Text("Reset Session?") },
            text = { Text("This will clear cached data, session cookies, and reload Arena to provide a fresh blind evaluation state.") },
            confirmButton = {
                Button(
                    onClick = {
                        onResetSession()
                        showConfirmReset = false
                        onShowSnackbar("Session reset & cookies cleared")
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

    // Clear Battles Confirmation Dialog
    if (showConfirmClearBattles) {
        AlertDialog(
            onDismissRequest = { showConfirmClearBattles = false },
            title = { Text("Clear All Battles?") },
            text = { Text("This permanently deletes all your recorded battles and resets the model scorecard. This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearBattles()
                        showConfirmClearBattles = false
                        onShowSnackbar("Battle history cleared")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ArenaDanger)
                ) {
                    Text("Delete All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmClearBattles = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionHeader(
    title: String,
    icon: ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = ArenaPrimary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(Dimensions.spaceSmall))
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingsRowItem(
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
            modifier = Modifier.padding(vertical = Dimensions.spaceSmall)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(Dimensions.spaceMedium))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
