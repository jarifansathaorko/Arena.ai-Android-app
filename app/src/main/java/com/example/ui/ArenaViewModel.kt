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

    val isOnline: StateFlow<Boolean> = networkObserver.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), networkObserver.isConnected())

    val prompts: StateFlow<List<PromptItem>> = repository.allPrompts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val battles: StateFlow<List<BattleRecord>> = repository.allBattles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    fun setUrl(url: String) {
        val normalized = normalizeUrl(url) ?: return
        if (normalized == _currentUrl.value) return // avoid reload loops
        // State-only: ArenaWebView's update block is the single place that
        // calls loadUrl(), so quick chips, deep links and resets can't
        // trigger competing duplicate loads.
        _currentUrl.value = normalized
    }

    fun onUrlObserved(url: String) {
        val normalized = normalizeUrl(url) ?: return
        if (normalized != _currentUrl.value) {
            _currentUrl.value = normalized
        }
    }

    fun setTitle(title: String) {
        val trimmed = title.trim()
        if (trimmed.isNotEmpty() && trimmed != _pageTitle.value) {
            _pageTitle.value = trimmed
        }
    }

    fun setProgress(progress: Float) {
        // Throttle: only emit when the change is visible (>2%) or load finished.
        // The top bar progress indicator recomposes on every emission, so this
        // cuts ~100 recompositions per page load down to ~30 with no visual loss.
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
        activeWebView?.get()?.reload()
    }

    fun goBack(): Boolean {
        val webView = activeWebView?.get()
        return if (webView?.canGoBack() == true) {
            webView.goBack()
            true
        } else {
            false
        }
    }

    fun goForward() {
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
        val webView = activeWebView?.get() ?: return false
        webView.evaluateJavascript(buildInjectionScript(promptContent), null)
        return true
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
        val webView = activeWebView?.get()
        webView?.clearCache(true)
        webView?.clearHistory()
        val alreadyHome = _currentUrl.value == ARENA_HOME_URL
        _currentUrl.value = ARENA_HOME_URL
        // State change alone drives a load via the WebView update block;
        // only force-load when already home (no state change to observe).
        if (alreadyHome) {
            webView?.loadUrl(ARENA_HOME_URL)
        }
    }

    companion object {
        /**
         * Normalizes a raw URL string. Returns null for blank or
         * non-http(s) URLs (e.g. javascript:, about:).
         */
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

        /**
         * Builds the prompt-injection JavaScript. Uses JSONObject.quote()
         * so quotes, backslashes, newlines and unicode separators in the
         * prompt can never break out of the JS string literal.
         */
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
