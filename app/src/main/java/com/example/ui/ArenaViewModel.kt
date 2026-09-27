package com.example.ui

import android.app.Application
import android.net.Uri
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
import com.example.data.repository.ModelStats
import com.example.ui.theme.ThemeMode
import com.example.util.NetworkObserver
import java.lang.ref.WeakReference
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject

sealed interface ActiveSheet {
    object None : ActiveSheet
    object Prompts : ActiveSheet
    object BattleTracker : ActiveSheet
    object Settings : ActiveSheet
}

const val ARENA_HOME_URL = "https://arena.ai/"

class ArenaViewModel(application: Application) : AndroidViewModel(application) {
    private val database = ArenaDatabase.getInstance(application)
    private val repository = ArenaRepository(database.promptDao(), database.battleDao())
    private val networkObserver = NetworkObserver(application)

    // Navigation and Viewport State
    private val _selectedTab = MutableStateFlow(ArenaTab.ARENA)
    val selectedTab: StateFlow<ArenaTab> = _selectedTab.asStateFlow()

    private val _selectedSubMode = MutableStateFlow(ArenaSubMode.BATTLE)
    val selectedSubMode: StateFlow<ArenaSubMode> = _selectedSubMode.asStateFlow()

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _dynamicColor = MutableStateFlow(false)
    val dynamicColor: StateFlow<Boolean> = _dynamicColor.asStateFlow()

    private val _currentUrl = MutableStateFlow(ARENA_HOME_URL)
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

    // Decoupled WebView Command Channel
    private val _webViewCommands = MutableSharedFlow<WebViewCommand>(extraBufferCapacity = 16)
    val webViewCommands: SharedFlow<WebViewCommand> = _webViewCommands.asSharedFlow()

    // Network Status
    val isOnline: StateFlow<Boolean> = networkObserver.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), networkObserver.isConnected())

    // Database Flows
    val prompts: StateFlow<List<PromptItem>> = repository.allPrompts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val battles: StateFlow<List<BattleRecord>> = repository.allBattles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Prompts Screen State & Filtered Flow
    private val _promptSearchQuery = MutableStateFlow("")
    val promptSearchQuery: StateFlow<String> = _promptSearchQuery.asStateFlow()

    private val _promptCategory = MutableStateFlow("All")
    val promptCategory: StateFlow<String> = _promptCategory.asStateFlow()

    val promptCategories: StateFlow<List<String>> = prompts
        .map { ArenaRepository.promptCategories(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredPrompts: StateFlow<List<PromptItem>> = combine(
        prompts,
        _promptCategory,
        _promptSearchQuery
    ) { all, category, query ->
        ArenaRepository.filterPrompts(all, category, query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Battles Screen State & Filtered Flow
    private val _battleSearchQuery = MutableStateFlow("")
    val battleSearchQuery: StateFlow<String> = _battleSearchQuery.asStateFlow()

    private val _battleCategory = MutableStateFlow("All")
    val battleCategory: StateFlow<String> = _battleCategory.asStateFlow()

    private val _battleWinnerFilter = MutableStateFlow<BattleWinner?>(null)
    val battleWinnerFilter: StateFlow<BattleWinner?> = _battleWinnerFilter.asStateFlow()

    val battleCategories: StateFlow<List<String>> = battles
        .map { ArenaRepository.battleCategories(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredBattles: StateFlow<List<BattleRecord>> = combine(
        battles,
        _battleSearchQuery,
        _battleCategory,
        _battleWinnerFilter
    ) { all, query, category, winner ->
        ArenaRepository.filterBattles(all, query, category, winner)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val modelStats: StateFlow<List<ModelStats>> = battles
        .map { ArenaRepository.computeModelStats(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Backward-compatible reference for tests
    private var activeWebView: WeakReference<WebView>? = null

    init {
        viewModelScope.launch {
            repository.initializeDefaultPromptsIfNeeded()
        }
    }

    override fun onCleared() {
        super.onCleared()
        unregisterWebView()
    }

    fun registerWebView(webView: WebView) {
        activeWebView = WeakReference(webView)
    }

    fun unregisterWebView() {
        activeWebView?.clear()
        activeWebView = null
    }

    // Navigation Actions
    fun switchTab(tab: ArenaTab) {
        if (_selectedTab.value != tab) {
            _selectedTab.value = tab
        }
    }

    fun switchSubMode(mode: ArenaSubMode) {
        _selectedSubMode.value = mode
        setUrl(mode.url)
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
    }

    fun setDynamicColor(enabled: Boolean) {
        _dynamicColor.value = enabled
    }

    fun setPromptSearch(query: String) {
        _promptSearchQuery.value = query
    }

    fun setPromptCategory(category: String) {
        _promptCategory.value = category
    }

    fun setBattleSearch(query: String) {
        _battleSearchQuery.value = query
    }

    fun setBattleCategory(category: String) {
        _battleCategory.value = category
    }

    fun setBattleWinnerFilter(winner: BattleWinner?) {
        _battleWinnerFilter.value = winner
    }

    fun setUrl(url: String) {
        val normalized = normalizeUrl(url) ?: return
        if (normalized == _currentUrl.value) return
        _currentUrl.value = normalized

        // Update selectedSubMode if URL matches a known mode
        when {
            normalized.contains("/leaderboard") -> _selectedSubMode.value = ArenaSubMode.LEADERBOARD
            normalized.contains("/history") -> _selectedSubMode.value = ArenaSubMode.HISTORY
            normalized == "https://arena.ai" || normalized == "https://arena.ai/" -> _selectedSubMode.value = ArenaSubMode.BATTLE
        }
    }

    fun onUrlObserved(url: String) {
        val normalized = normalizeUrl(url) ?: return
        if (normalized != _currentUrl.value) {
            _currentUrl.value = normalized
            when {
                normalized.contains("/leaderboard") -> _selectedSubMode.value = ArenaSubMode.LEADERBOARD
                normalized.contains("/history") -> _selectedSubMode.value = ArenaSubMode.HISTORY
                normalized == "https://arena.ai" || normalized == "https://arena.ai/" -> _selectedSubMode.value = ArenaSubMode.BATTLE
            }
        }
    }

    fun setTitle(title: String) {
        val trimmed = title.trim()
        if (trimmed.isNotEmpty() && trimmed != _pageTitle.value) {
            _pageTitle.value = trimmed
        }
    }

    fun setProgress(progress: Float) {
        val clamped = progress.coerceIn(0f, 1f)
        val current = _loadingProgress.value
        if (clamped == 1f || clamped == 0f || kotlin.math.abs(clamped - current) >= 0.02f) {
            _loadingProgress.value = clamped
        }
    }

    fun setCanGoBack(canGoBack: Boolean) {
        if (canGoBack != _canGoBack.value) _canGoBack.value = canGoBack
    }

    fun setCanGoForward(canGoForward: Boolean) {
        if (canGoForward != _canGoForward.value) _canGoForward.value = canGoForward
    }

    fun toggleDesktopMode() {
        _isDesktopMode.value = !_isDesktopMode.value
    }

    fun setTextZoom(zoom: Int) {
        _textZoom.value = zoom.coerceIn(50, 200)
    }

    fun setActiveSheet(sheet: ActiveSheet) {
        _activeSheet.value = sheet
    }

    fun reload() {
        _webViewCommands.tryEmit(WebViewCommand.Reload)
        activeWebView?.get()?.reload()
    }

    fun goBack(): Boolean {
        _webViewCommands.tryEmit(WebViewCommand.GoBack)
        val webView = activeWebView?.get()
        return if (webView?.canGoBack() == true) {
            webView.goBack()
            true
        } else {
            _canGoBack.value
        }
    }

    fun goForward() {
        _webViewCommands.tryEmit(WebViewCommand.GoForward)
        val webView = activeWebView?.get()
        if (webView?.canGoForward() == true) {
            webView.goForward()
        }
    }

    /**
     * Injects [promptContent] into Arena's chat input.
     * @return true if a WebView was available to receive the script.
     */
    fun injectPromptToArena(promptContent: String): Boolean {
        if (promptContent.isBlank()) return false
        val script = buildInjectionScript(promptContent)
        _webViewCommands.tryEmit(WebViewCommand.InjectPrompt(script))
        val webView = activeWebView?.get()
        if (webView != null) {
            webView.evaluateJavascript(script, null)
            return true
        }
        return false
    }

    /**
     * Injects prompt and automatically switches the active tab to Arena!
     */
    fun sendPromptToArena(promptContent: String): Boolean {
        switchTab(ArenaTab.ARENA)
        return injectPromptToArena(promptContent)
    }

    fun addCustomPrompt(title: String, category: String, content: String) {
        if (title.isBlank() || content.isBlank()) return
        viewModelScope.launch {
            try {
                repository.addPrompt(title, category, content)
            } catch (_: IllegalArgumentException) {
                // Invalid input already guarded above; ignore.
            }
        }
    }

    fun updatePrompt(id: Long, title: String, category: String, content: String) {
        if (title.isBlank() || content.isBlank()) return
        viewModelScope.launch {
            try {
                repository.updatePrompt(id, title, category, content)
            } catch (_: IllegalArgumentException) {
                // Guarded
            }
        }
    }

    fun duplicatePrompt(prompt: PromptItem) {
        viewModelScope.launch {
            repository.duplicatePrompt(prompt)
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
        if (modelA.isBlank() || modelB.isBlank()) return
        if (modelA.trim().equals(modelB.trim(), ignoreCase = true)) return
        viewModelScope.launch {
            try {
                repository.logBattle(modelA, modelB, winner, topic, category, notes)
            } catch (_: IllegalArgumentException) {
                // Guarded above; ignore.
            }
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
        _loadingProgress.value = 0f
        _pageTitle.value = "Arena AI"
        _webViewCommands.tryEmit(WebViewCommand.ClearSession)
        val webView = activeWebView?.get()
        webView?.clearCache(true)
        webView?.clearHistory()
        val alreadyHome = _currentUrl.value == ARENA_HOME_URL
        _currentUrl.value = ARENA_HOME_URL
        if (alreadyHome) {
            webView?.loadUrl(ARENA_HOME_URL)
        }
    }

    companion object {
        fun normalizeUrl(raw: String): String? {
            val trimmed = raw.trim()
            if (trimmed.isBlank()) return null
            if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                return trimmed
            }
            return null
        }

        fun isArenaDeepLink(uri: Uri): Boolean {
            val host = (uri.host ?: return false).lowercase()
            return host == "arena.ai" || host.endsWith(".arena.ai") ||
                    host == "lmarena.ai" || host.endsWith(".lmarena.ai")
        }

        fun buildInjectionScript(promptContent: String): String {
            val quoted = JSONObject.quote(promptContent)
            return """
            (function() {
                var selectors = ['textarea', 'input[type="text"]', '[contenteditable="true"]'];
                var input = null;
                for (var s = 0; s < selectors.length; s++) {
                    var candidates = document.querySelectorAll(selectors[s]);
                    for (var c = 0; c < candidates.length; c++) {
                        var el = candidates[c];
                        if (el && el.offsetParent !== null) { input = el; break; }
                    }
                    if (input) break;
                }
                if (!input) return 'no-input';
                var value = $quoted;
                input.focus();
                try {
                    var tag = input.tagName.toLowerCase();
                    if (tag === 'textarea') {
                        var taDesc = Object.getOwnPropertyDescriptor(window.HTMLTextAreaElement.prototype, 'value');
                        if (taDesc && taDesc.set) { taDesc.set.call(input, value); }
                        else { input.value = value; }
                    } else if (tag === 'input') {
                        var inDesc = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value');
                        if (inDesc && inDesc.set) { inDesc.set.call(input, value); }
                        else { input.value = value; }
                    } else {
                        input.textContent = value;
                    }
                    input.dispatchEvent(new Event('input', { bubbles: true }));
                    input.dispatchEvent(new Event('change', { bubbles: true }));
                    return 'ok';
                } catch (e) {
                    return 'error:' + e;
                }
            })();
            """.trimIndent()
        }
    }
}
