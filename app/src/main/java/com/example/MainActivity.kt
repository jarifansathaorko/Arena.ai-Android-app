package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.*
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: ArenaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle incoming deep link intent
        handleDeepLink(intent)

        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val dynamicColor by viewModel.dynamicColor.collectAsStateWithLifecycle()

            MyApplicationTheme(themeMode = themeMode, dynamicColor = dynamicColor) {
                ArenaApp(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        val uri = intent?.data ?: return
        if (ArenaViewModel.isArenaDeepLink(uri)) {
            viewModel.setUrl(uri.toString())
            viewModel.switchTab(ArenaTab.ARENA)
        }
    }
}

@Composable
fun ArenaApp(viewModel: ArenaViewModel) {
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Navigation & Appearance State
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val selectedSubMode by viewModel.selectedSubMode.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val dynamicColor by viewModel.dynamicColor.collectAsStateWithLifecycle()

    // WebView State
    val currentUrl by viewModel.currentUrl.collectAsStateWithLifecycle()
    val pageTitle by viewModel.pageTitle.collectAsStateWithLifecycle()
    val progress by viewModel.loadingProgress.collectAsStateWithLifecycle()
    val canGoBack by viewModel.canGoBack.collectAsStateWithLifecycle()
    val canGoForward by viewModel.canGoForward.collectAsStateWithLifecycle()
    val isDesktopMode by viewModel.isDesktopMode.collectAsStateWithLifecycle()
    val textZoom by viewModel.textZoom.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val activeSheet by viewModel.activeSheet.collectAsStateWithLifecycle()

    // Prompts & Battles State
    val prompts by viewModel.prompts.collectAsStateWithLifecycle()
    val filteredPrompts by viewModel.filteredPrompts.collectAsStateWithLifecycle()
    val promptCategories by viewModel.promptCategories.collectAsStateWithLifecycle()
    val promptSearchQuery by viewModel.promptSearchQuery.collectAsStateWithLifecycle()
    val selectedPromptCategory by viewModel.promptCategory.collectAsStateWithLifecycle()

    val battles by viewModel.battles.collectAsStateWithLifecycle()
    val filteredBattles by viewModel.filteredBattles.collectAsStateWithLifecycle()
    val modelStats by viewModel.modelStats.collectAsStateWithLifecycle()
    val battleCategories by viewModel.battleCategories.collectAsStateWithLifecycle()
    val battleSearchQuery by viewModel.battleSearchQuery.collectAsStateWithLifecycle()
    val selectedBattleCategory by viewModel.battleCategory.collectAsStateWithLifecycle()
    val selectedWinnerFilter by viewModel.battleWinnerFilter.collectAsStateWithLifecycle()

    fun showMessage(message: String) {
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
    }

    // System Back Handler:
    // 1. Close modal sheets if open
    // 2. Return to Arena tab if on secondary tab
    // 3. Navigate WebView history if canGoBack
    // 4. Default back (exit)
    BackHandler(enabled = activeSheet !is ActiveSheet.None || selectedTab != ArenaTab.ARENA || canGoBack) {
        when {
            activeSheet !is ActiveSheet.None -> viewModel.setActiveSheet(ActiveSheet.None)
            selectedTab != ArenaTab.ARENA -> viewModel.switchTab(ArenaTab.ARENA)
            canGoBack -> viewModel.goBack()
        }
    }

    DisposableEffect(viewModel) {
        onDispose {
            viewModel.unregisterWebView()
        }
    }

    // Adaptive Layout: Navigation Rail for wide screens (>= 600dp), Bottom Navigation for phones
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp

        if (isWideScreen) {
            Row(modifier = Modifier.fillMaxSize()) {
                ArenaNavigationRail(
                    selectedTab = selectedTab,
                    onTabSelected = { viewModel.switchTab(it) }
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    ScreenContent(
                        selectedTab = selectedTab,
                        selectedSubMode = selectedSubMode,
                        themeMode = themeMode,
                        dynamicColor = dynamicColor,
                        currentUrl = currentUrl,
                        isDesktopMode = isDesktopMode,
                        textZoom = textZoom,
                        progress = progress,
                        isOnline = isOnline,
                        prompts = prompts,
                        filteredPrompts = filteredPrompts,
                        promptCategories = promptCategories,
                        promptSearchQuery = promptSearchQuery,
                        selectedPromptCategory = selectedPromptCategory,
                        battles = battles,
                        filteredBattles = filteredBattles,
                        modelStats = modelStats,
                        battleCategories = battleCategories,
                        battleSearchQuery = battleSearchQuery,
                        selectedBattleCategory = selectedBattleCategory,
                        selectedWinnerFilter = selectedWinnerFilter,
                        viewModel = viewModel,
                        onShowSnackbar = { showMessage(it) }
                    )
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            Scaffold(
                bottomBar = {
                    ArenaNavigationBar(
                        selectedTab = selectedTab,
                        onTabSelected = { viewModel.switchTab(it) }
                    )
                },
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    ScreenContent(
                        selectedTab = selectedTab,
                        selectedSubMode = selectedSubMode,
                        themeMode = themeMode,
                        dynamicColor = dynamicColor,
                        currentUrl = currentUrl,
                        isDesktopMode = isDesktopMode,
                        textZoom = textZoom,
                        progress = progress,
                        isOnline = isOnline,
                        prompts = prompts,
                        filteredPrompts = filteredPrompts,
                        promptCategories = promptCategories,
                        promptSearchQuery = promptSearchQuery,
                        selectedPromptCategory = selectedPromptCategory,
                        battles = battles,
                        filteredBattles = filteredBattles,
                        modelStats = modelStats,
                        battleCategories = battleCategories,
                        battleSearchQuery = battleSearchQuery,
                        selectedBattleCategory = selectedBattleCategory,
                        selectedWinnerFilter = selectedWinnerFilter,
                        viewModel = viewModel,
                        onShowSnackbar = { showMessage(it) }
                    )
                }
            }
        }
    }

    // Backward-Compatible Sheets support
    if (activeSheet is ActiveSheet.Prompts) {
        PromptLibrarySheet(
            prompts = prompts,
            onDismiss = { viewModel.setActiveSheet(ActiveSheet.None) },
            onUsePrompt = { promptText ->
                val injected = viewModel.injectPromptToArena(promptText)
                showMessage(
                    if (injected) "Prompt injected into Arena input!"
                    else "Arena page is still loading — try again in a moment."
                )
            },
            onCopyPrompt = { promptText ->
                clipboardManager.setText(AnnotatedString(promptText))
                showMessage("Copied prompt to clipboard!")
            },
            onToggleFavorite = { prompt -> viewModel.toggleFavorite(prompt) },
            onAddPrompt = { title, cat, content ->
                viewModel.addCustomPrompt(title, cat, content)
                showMessage("Custom benchmark prompt saved!")
            },
            onDeletePrompt = { prompt -> viewModel.deletePrompt(prompt) }
        )
    }

    if (activeSheet is ActiveSheet.BattleTracker) {
        BattleTrackerDialog(
            battles = battles,
            onDismiss = { viewModel.setActiveSheet(ActiveSheet.None) },
            onLogBattle = { modelA, modelB, winner, topic, category, notes ->
                viewModel.logBattle(modelA, modelB, winner, topic, category, notes)
                showMessage("Battle recorded to scorecard!")
            },
            onDeleteBattle = { battle -> viewModel.deleteBattle(battle) },
            onClearAll = { viewModel.clearBattles() }
        )
    }

    if (activeSheet is ActiveSheet.Settings) {
        ArenaSettingsDialog(
            currentUrl = currentUrl,
            isDesktopMode = isDesktopMode,
            textZoom = textZoom,
            onToggleDesktop = { viewModel.toggleDesktopMode() },
            onChangeTextZoom = { zoom -> viewModel.setTextZoom(zoom) },
            onResetSession = {
                viewModel.resetSession()
                showMessage("Session reset. Reloaded Arena.")
            },
            onDismiss = { viewModel.setActiveSheet(ActiveSheet.None) }
        )
    }
}

@Composable
private fun ScreenContent(
    selectedTab: ArenaTab,
    selectedSubMode: ArenaSubMode,
    themeMode: com.example.ui.theme.ThemeMode,
    dynamicColor: Boolean,
    currentUrl: String,
    isDesktopMode: Boolean,
    textZoom: Int,
    progress: Float,
    isOnline: Boolean,
    prompts: List<com.example.data.model.PromptItem>,
    filteredPrompts: List<com.example.data.model.PromptItem>,
    promptCategories: List<String>,
    promptSearchQuery: String,
    selectedPromptCategory: String,
    battles: List<com.example.data.model.BattleRecord>,
    filteredBattles: List<com.example.data.model.BattleRecord>,
    modelStats: List<com.example.data.repository.ModelStats>,
    battleCategories: List<String>,
    battleSearchQuery: String,
    selectedBattleCategory: String,
    selectedWinnerFilter: com.example.data.model.BattleWinner?,
    viewModel: ArenaViewModel,
    onShowSnackbar: (String) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Arena Screen is maintained in the composition tree to retain WebView session & DOM state!
        Box(
            modifier = if (selectedTab == ArenaTab.ARENA) Modifier.fillMaxSize()
            else Modifier
                .fillMaxSize()
                .zIndex(-1f)
                .alpha(0f)
        ) {
            ArenaScreen(
                currentUrl = currentUrl,
                currentSubMode = selectedSubMode,
                isDesktopMode = isDesktopMode,
                textZoom = textZoom,
                progress = progress,
                isOnline = isOnline,
                prompts = prompts,
                categories = promptCategories,
                webViewCommands = viewModel.webViewCommands,
                onSubModeSelected = { viewModel.switchSubMode(it) },
                onProgressChange = { viewModel.setProgress(it) },
                onTitleChange = { viewModel.setTitle(it) },
                onUrlChange = { viewModel.onUrlObserved(it) },
                onCanGoBackChange = { viewModel.setCanGoBack(it) },
                onCanGoForwardChange = { viewModel.setCanGoForward(it) },
                onToggleDesktop = { viewModel.toggleDesktopMode() },
                onReload = { viewModel.reload() },
                onResetSession = {
                    viewModel.resetSession()
                    onShowSnackbar("Session reset & cookies cleared")
                },
                onInjectPrompt = { promptText ->
                    viewModel.injectPromptToArena(promptText)
                },
                onShowSnackbar = onShowSnackbar,
                onWebViewCreated = { webView -> viewModel.registerWebView(webView) }
            )
        }

        // Secondary Tabs rendered conditionally
        when (selectedTab) {
            ArenaTab.ARENA -> { /* Rendered above with state preservation */ }
            ArenaTab.PROMPTS -> {
                PromptsScreen(
                    prompts = filteredPrompts,
                    categories = promptCategories,
                    selectedCategory = selectedPromptCategory,
                    searchQuery = promptSearchQuery,
                    onCategorySelected = { viewModel.setPromptCategory(it) },
                    onSearchQueryChanged = { viewModel.setPromptSearch(it) },
                    onUsePrompt = { promptText ->
                        viewModel.sendPromptToArena(promptText)
                        onShowSnackbar("Prompt sent to Arena!")
                    },
                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                    onAddPrompt = { title, cat, content ->
                        viewModel.addCustomPrompt(title, cat, content)
                    },
                    onUpdatePrompt = { id, title, cat, content ->
                        viewModel.updatePrompt(id, title, cat, content)
                    },
                    onDuplicatePrompt = { viewModel.duplicatePrompt(it) },
                    onDeletePrompt = { viewModel.deletePrompt(it) },
                    onShowSnackbar = onShowSnackbar
                )
            }
            ArenaTab.BATTLES -> {
                BattlesScreen(
                    battles = filteredBattles,
                    modelStats = modelStats,
                    categories = battleCategories,
                    searchQuery = battleSearchQuery,
                    selectedCategory = selectedBattleCategory,
                    selectedWinnerFilter = selectedWinnerFilter,
                    onSearchQueryChanged = { viewModel.setBattleSearch(it) },
                    onCategorySelected = { viewModel.setBattleCategory(it) },
                    onWinnerFilterSelected = { viewModel.setBattleWinnerFilter(it) },
                    onLogBattle = { mA, mB, win, topic, cat, notes ->
                        viewModel.logBattle(mA, mB, win, topic, cat, notes)
                    },
                    onDeleteBattle = { viewModel.deleteBattle(it) },
                    onClearAllBattles = { viewModel.clearBattles() },
                    onShowSnackbar = onShowSnackbar
                )
            }
            ArenaTab.SETTINGS -> {
                SettingsScreen(
                    themeMode = themeMode,
                    dynamicColor = dynamicColor,
                    isDesktopMode = isDesktopMode,
                    textZoom = textZoom,
                    currentUrl = currentUrl,
                    onThemeModeChanged = { viewModel.setThemeMode(it) },
                    onDynamicColorChanged = { viewModel.setDynamicColor(it) },
                    onToggleDesktop = { viewModel.toggleDesktopMode() },
                    onChangeTextZoom = { viewModel.setTextZoom(it) },
                    onResetSession = {
                        viewModel.resetSession()
                        onShowSnackbar("Session reset & cookies cleared")
                    },
                    onClearBattles = {
                        viewModel.clearBattles()
                        onShowSnackbar("Battle history cleared")
                    },
                    onShowSnackbar = onShowSnackbar
                )
            }
        }
    }
}
