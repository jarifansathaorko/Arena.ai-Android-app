package com.example.ui

import android.app.Application
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ArenaDatabase
import com.example.data.model.BattleRecord
import com.example.data.model.BattleWinner
import com.example.data.model.PromptItem
import com.example.data.repository.ArenaRepository
import com.example.util.NetworkObserver
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface ActiveSheet {
    object None : ActiveSheet
    object Prompts : ActiveSheet
    object BattleTracker : ActiveSheet
    object Settings : ActiveSheet
}

class ArenaViewModel(application: Application) : AndroidViewModel(application) {
    private val database = ArenaDatabase.getInstance(application)
    private val repository = ArenaRepository(database.promptDao(), database.battleDao())
    private val networkObserver = NetworkObserver(application)

    private val _currentUrl = MutableStateFlow("https://arena.ai/")
    val currentUrl: StateFlow<String> = _currentUrl.asStateFlow()

    private val _pageTitle = MutableStateFlow("Arena AI")
    val pageTitle: StateFlow<String> = _pageTitle.asStateFlow()

    private val _loadingProgress = MutableStateFlow(0f)
    val loadingProgress: StateFlow<Float> = _loadingProgress.asStateFlow()

    private val _canGoBack = MutableStateFlow(false)
    val canGoBack: StateFlow<Boolean> = _canGoBack.asStateFlow()

    private val _canGoForward = MutableStateFlow(false)
    val canGoForward: StateFlow<Boolean> = _canGoForward.asStateFlow()

    private val _isDesktopMode = MutableStateFlow(false)
    val isDesktopMode: StateFlow<Boolean> = _isDesktopMode.asStateFlow()

    private val _textZoom = MutableStateFlow(100)
    val textZoom: StateFlow<Int> = _textZoom.asStateFlow()

    private val _activeSheet = MutableStateFlow<ActiveSheet>(ActiveSheet.None)
    val activeSheet: StateFlow<ActiveSheet> = _activeSheet.asStateFlow()

    val isOnline: StateFlow<Boolean> = networkObserver.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), networkObserver.isConnected())

    val prompts: StateFlow<List<PromptItem>> = repository.allPrompts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val battles: StateFlow<List<BattleRecord>> = repository.allBattles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var activeWebView: WebView? = null

    init {
        viewModelScope.launch {
            repository.initializeDefaultPromptsIfNeeded()
        }
    }

    fun registerWebView(webView: WebView) {
        activeWebView = webView
    }

    fun setUrl(url: String) {
        _currentUrl.value = url
        activeWebView?.loadUrl(url)
    }

    fun onUrlObserved(url: String) {
        _currentUrl.value = url
    }

    fun setTitle(title: String) {
        _pageTitle.value = title
    }

    fun setProgress(progress: Float) {
        _loadingProgress.value = progress
    }

    fun setCanGoBack(canGoBack: Boolean) {
        _canGoBack.value = canGoBack
    }

    fun setCanGoForward(canGoForward: Boolean) {
        _canGoForward.value = canGoForward
    }

    fun toggleDesktopMode() {
        _isDesktopMode.value = !_isDesktopMode.value
    }

    fun setTextZoom(zoom: Int) {
        _textZoom.value = zoom
    }

    fun setActiveSheet(sheet: ActiveSheet) {
        _activeSheet.value = sheet
    }

    fun reload() {
        activeWebView?.reload()
    }

    fun goBack(): Boolean {
        return if (activeWebView?.canGoBack() == true) {
            activeWebView?.goBack()
            true
        } else {
            false
        }
    }

    fun goForward() {
        if (activeWebView?.canGoForward() == true) {
            activeWebView?.goForward()
        }
    }

    fun injectPromptToArena(promptContent: String) {
        // Escaped string for JS evaluation
        val sanitized = promptContent
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "")

        val script = """
            (function() {
                var input = document.querySelector('textarea') || 
                            document.querySelector('input[type="text"]') ||
                            document.querySelector('[contenteditable="true"]');
                if (input) {
                    if (input.tagName.toLowerCase() === 'textarea' || input.tagName.toLowerCase() === 'input') {
                        input.value = "$sanitized";
                        input.dispatchEvent(new Event('input', { bubbles: true }));
                        input.dispatchEvent(new Event('change', { bubbles: true }));
                        input.focus();
                    } else {
                        input.innerText = "$sanitized";
                        input.dispatchEvent(new Event('input', { bubbles: true }));
                        input.focus();
                    }
                }
            })();
        """.trimIndent()

        activeWebView?.evaluateJavascript(script, null)
    }

    fun addCustomPrompt(title: String, category: String, content: String) {
        viewModelScope.launch {
            repository.addPrompt(title, category, content)
        }
    }

    fun toggleFavorite(prompt: PromptItem) {
        viewModelScope.launch {
            repository.toggleFavorite(prompt.id, prompt.isFavorite)
        }
    }

    fun deletePrompt(prompt: PromptItem) {
        viewModelScope.launch {
            repository.deletePrompt(prompt)
        }
    }

    fun logBattle(
        modelA: String,
        modelB: String,
        winner: BattleWinner,
        topic: String,
        category: String,
        notes: String
    ) {
        viewModelScope.launch {
            repository.logBattle(modelA, modelB, winner, topic, category, notes)
        }
    }

    fun deleteBattle(battle: BattleRecord) {
        viewModelScope.launch {
            repository.deleteBattle(battle)
        }
    }

    fun clearBattles() {
        viewModelScope.launch {
            repository.clearBattles()
        }
    }

    fun resetSession() {
        val cookieManager = CookieManager.getInstance()
        cookieManager.removeAllCookies(null)
        cookieManager.flush()
        WebStorage.getInstance().deleteAllData()
        activeWebView?.clearCache(true)
        activeWebView?.clearHistory()
        activeWebView?.loadUrl("https://arena.ai/")
    }
}
