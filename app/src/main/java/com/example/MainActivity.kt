package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.SportsKabaddi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.ActiveSheet
import com.example.ui.ArenaViewModel
import com.example.ui.components.*
import com.example.ui.theme.ArenaPrimary
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: ArenaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle incoming deep link intent
        intent?.data?.let { uri ->
            if (uri.host?.contains("arena.ai") == true || uri.host?.contains("lmarena.ai") == true) {
                viewModel.setUrl(uri.toString())
            }
        }

        setContent {
            MyApplicationTheme {
                ArenaApp(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.data?.let { uri ->
            if (uri.host?.contains("arena.ai") == true || uri.host?.contains("lmarena.ai") == true) {
                viewModel.setUrl(uri.toString())
            }
        }
    }
}

@Composable
fun ArenaApp(viewModel: ArenaViewModel) {
    val context = LocalContext.current
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

    Scaffold(
        topBar = {
            Column {
                Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
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
                    onOpenSettings = { viewModel.setActiveSheet(ActiveSheet.Settings) },
                    onNavigateToUrl = { url -> viewModel.setUrl(url) }
                )
                OfflineBanner(
                    isOnline = isOnline,
                    onRetry = { viewModel.reload() }
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        contentWindowInsets = WindowInsets(0.dp)
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
                onProgressChange = { viewModel.setProgress(it) },
                onTitleChange = { viewModel.setTitle(it) },
                onUrlChange = { viewModel.onUrlObserved(it) },
                onCanGoBackChange = { viewModel.setCanGoBack(it) },
                onCanGoForwardChange = { viewModel.setCanGoForward(it) },
                onWebViewCreated = { webView -> viewModel.registerWebView(webView) }
            )
        }
    }

    // Benchmark Prompts Bottom Sheet
    if (activeSheet is ActiveSheet.Prompts) {
        PromptLibrarySheet(
            prompts = prompts,
            onDismiss = { viewModel.setActiveSheet(ActiveSheet.None) },
            onUsePrompt = { promptText ->
                viewModel.injectPromptToArena(promptText)
                scope.launch {
                    snackbarHostState.showSnackbar("Prompt injected into Arena input!")
                }
            },
            onCopyPrompt = { promptText ->
                clipboardManager.setText(AnnotatedString(promptText))
                scope.launch {
                    snackbarHostState.showSnackbar("Copied prompt to clipboard!")
                }
            },
            onToggleFavorite = { prompt -> viewModel.toggleFavorite(prompt) },
            onAddPrompt = { title, cat, content ->
                viewModel.addCustomPrompt(title, cat, content)
                scope.launch {
                    snackbarHostState.showSnackbar("Custom benchmark prompt saved!")
                }
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
                scope.launch {
                    snackbarHostState.showSnackbar("Battle recorded to scorecard!")
                }
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
                scope.launch {
                    snackbarHostState.showSnackbar("Session reset. Reloaded Arena.")
                }
            },
            onDismiss = { viewModel.setActiveSheet(ActiveSheet.None) }
        )
    }
}
