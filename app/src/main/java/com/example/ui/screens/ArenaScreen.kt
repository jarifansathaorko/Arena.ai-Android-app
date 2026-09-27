package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PromptItem
import com.example.ui.ArenaSubMode
import com.example.ui.WebViewCommand
import com.example.ui.components.*
import com.example.ui.theme.ArenaPrimary
import com.example.ui.theme.Dimensions
import com.example.ui.theme.getCategoryColor
import kotlinx.coroutines.flow.SharedFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArenaScreen(
    currentUrl: String,
    currentSubMode: ArenaSubMode,
    isDesktopMode: Boolean,
    textZoom: Int,
    progress: Float,
    isOnline: Boolean,
    prompts: List<PromptItem>,
    categories: List<String>,
    webViewCommands: SharedFlow<WebViewCommand>,
    onSubModeSelected: (ArenaSubMode) -> Unit,
    onProgressChange: (Float) -> Unit,
    onTitleChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onCanGoBackChange: (Boolean) -> Unit,
    onCanGoForwardChange: (Boolean) -> Unit,
    onToggleDesktop: () -> Unit,
    onReload: () -> Unit,
    onResetSession: () -> Unit,
    onInjectPrompt: (String) -> Boolean,
    onShowSnackbar: (String) -> Unit,
    onWebViewCreated: (android.webkit.WebView) -> Unit,
    modifier: Modifier = Modifier
) {
    var showQuickPromptsSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                ArenaHeader(
                    currentSubMode = currentSubMode,
                    onSubModeSelected = onSubModeSelected,
                    progress = progress,
                    isDesktopMode = isDesktopMode,
                    isOnline = isOnline,
                    onReload = onReload,
                    onToggleDesktop = onToggleDesktop,
                    onResetSession = onResetSession,
                    currentUrl = currentUrl
                )
                OfflineBanner(
                    isOnline = isOnline,
                    onRetry = onReload
                )
            }
        },
        floatingActionButton = {
            // Quick Prompts injector button
            ExtendedFloatingActionButton(
                onClick = { showQuickPromptsSheet = true },
                containerColor = ArenaPrimary,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Bolt, contentDescription = null) },
                text = { Text("Quick Prompts", fontWeight = FontWeight.SemiBold) },
                shape = RoundedCornerShape(Dimensions.radiusPill),
                modifier = Modifier.testTag("quick_prompts_fab")
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            ArenaWebView(
                currentUrl = currentUrl,
                isDesktopMode = isDesktopMode,
                textZoom = textZoom,
                onProgressChange = onProgressChange,
                onTitleChange = onTitleChange,
                onUrlChange = onUrlChange,
                onCanGoBackChange = onCanGoBackChange,
                onCanGoForwardChange = onCanGoForwardChange,
                onWebViewCreated = onWebViewCreated,
                webViewCommands = webViewCommands
            )

            // Loading pill pinned at the top
            ArenaLoadingPill(
                progress = progress,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = Dimensions.spaceSmall)
            )
        }
    }

    // Quick Prompts Bottom Sheet
    if (showQuickPromptsSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var quickCategory by remember { mutableStateOf("All") }
        val quickCategories = remember(categories) { listOf("All", "⭐ Favorites") + categories }
        val filteredQuickPrompts = remember(prompts, quickCategory) {
            when (quickCategory) {
                "All" -> prompts
                "⭐ Favorites" -> prompts.filter { it.isFavorite }
                else -> prompts.filter { it.category.equals(quickCategory, ignoreCase = true) }
            }
        }

        ModalBottomSheet(
            onDismissRequest = { showQuickPromptsSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            modifier = Modifier
                .fillMaxHeight(0.75f)
                .testTag("quick_prompts_sheet")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .padding(horizontal = Dimensions.spaceStandard)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = ArenaPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(Dimensions.spaceSmall))
                        Text(
                            text = "Inject Benchmark Prompt",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = { showQuickPromptsSheet = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Text(
                    text = "Tap any prompt below to inject it instantly into Arena's chat input.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = Dimensions.spaceSmall)
                )

                // Quick Category Carousel
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(Dimensions.spaceSmall),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = Dimensions.spaceSmall)
                ) {
                    items(quickCategories) { cat ->
                        FilterChip(
                            selected = quickCategory == cat,
                            onClick = { quickCategory = cat },
                            label = { Text(cat, fontSize = 11.sp) },
                            shape = RoundedCornerShape(Dimensions.radiusPill)
                        )
                    }
                }

                // Prompts list
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(Dimensions.spaceSmall),
                    contentPadding = PaddingValues(bottom = Dimensions.space2xl),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(filteredQuickPrompts, key = { it.id }) { prompt ->
                        val catColor = getCategoryColor(prompt.category)
                        Surface(
                            shape = RoundedCornerShape(Dimensions.radiusMedium),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            onClick = {
                                val success = onInjectPrompt(prompt.content)
                                showQuickPromptsSheet = false
                                onShowSnackbar(
                                    if (success) "Prompt injected into Arena!"
                                    else "Arena is still loading — try again in a moment."
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.padding(Dimensions.spaceMedium)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            color = catColor.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(4.dp),
                                            modifier = Modifier.padding(end = Dimensions.spaceSmall)
                                        ) {
                                            Text(
                                                text = prompt.category,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = catColor,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(
                                            text = prompt.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = prompt.content,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.width(Dimensions.spaceSmall))
                                FilledIconButton(
                                    onClick = {
                                        val success = onInjectPrompt(prompt.content)
                                        showQuickPromptsSheet = false
                                        onShowSnackbar(
                                            if (success) "Prompt injected into Arena!"
                                            else "Arena is still loading — try again in a moment."
                                        )
                                    },
                                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = ArenaPrimary),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Inject prompt",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
