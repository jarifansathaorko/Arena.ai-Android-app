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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.ARENA_HOME_URL
import com.example.ui.ActiveSheet
import com.example.ui.ArenaViewModel
import com.example.ui.components.*
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
            MyApplicationTheme {
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
        }
    }
}

@Composable
fun ArenaApp(viewModel: ArenaViewModel) {
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val currentUrl by viewModel.currentUrl.collectAsStateWithLifecycle()
    val pageTitle by viewModel.pageTitle.collectAsStateWithLifecycle()
    val progress by viewModel.loadingProgress.collectAsStateWithLifecycle()
    val canGoBack by viewModel.canGoBack.collectAsStateWithLifecycle()
    val canGoForward by viewModel.canGoForward.collectAsStateWithLifecycle()
    val isDesktopMode by viewModel.isDesktopMode.collectAsStateWithLifecycle()
    val textZoom by viewModel.textZoom.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val activeSheet by viewModel.activeSheet.collectAsStateWithLifecycle()

    val prompts by viewModel.prompts.collectAsStateWithLifecycle()
    val battles by viewModel.battles.collectAsStateWithLifecycle()

    // System Back Handler: navigate web history first
    BackHandler(enabled = canGoBack || activeSheet !is ActiveSheet.None) {
        if (activeSheet !is ActiveSheet.None) {
            viewModel.setActiveSheet(ActiveSheet.None)
        } else {
            viewModel.goBack()
        }
    }

    DisposableEffect(viewModel) {
        onDispose {
            viewModel.unregisterWebView()
        }
    }

    fun showMessage(message: String) {
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                ArenaTopBar(
                    currentUrl = currentUrl,
                    pageTitle = pageTitle,
                    progress = progress,
                    isDesktopMode = isDesktopMode,
                    canGoBack = canGoBack,
                    canGoForward = canGoForward,
                    onNavigateBack = { viewModel.goBack() },
                    onNavigateForward = { viewModel.goForward() },
                    onReload = { viewModel.reload() },
                    onToggleDesktop = { viewModel.toggleDesktopMode() },
                    onOpenPrompts = { viewModel.setActiveSheet(ActiveSheet.Prompts) },
                    onOpenBattleTracker = { viewModel.setActiveSheet(ActiveSheet.BattleTracker) },
                    onOpenSettings = { viewModel.setActiveSheet(ActiveSheet.Settings) }
                )
                OfflineBanner(
                    isOnline = isOnline,
                    onRetry = { viewModel.reload() }
                )
            }
        },
        bottomBar = {
            ArenaBottomBar(
                currentUrl = currentUrl,
                onNavigateToUrl = { url -> viewModel.setUrl(url) },
                onOpenBattleTracker = { viewModel.setActiveSheet(ActiveSheet.BattleTracker) },
                onOpenPrompts = { viewModel.setActiveSheet(ActiveSheet.Prompts) }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            ArenaWebView(
                currentUrl = currentUrl.ifBlank { ARENA_HOME_URL },
                isDesktopMode = isDesktopMode,
                textZoom = textZoom,
                onProgressChange = { viewModel.setProgress(it) },
                onTitleChange = { viewModel.setTitle(it) },
                onUrlChange = { viewModel.onUrlObserved(it) },
                onCanGoBackChange = { viewModel.setCanGoBack(it) },
                onCanGoForwardChange = { viewModel.setCanGoForward(it) },
                onWebViewCreated = { webView -> viewModel.registerWebView(webView) }
            )
            ArenaLoadingPill(
                progress = progress,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp)
            )
        }
    }

    // Benchmark Prompts Bottom Sheet
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

    // Battle Tracker Dialog
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

    // Settings Dialog
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
