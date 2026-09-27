package com.example

import android.app.Application
import android.net.Uri
import android.webkit.ValueCallback
import android.webkit.WebView
import androidx.test.core.app.ApplicationProvider
import com.example.ui.ARENA_HOME_URL
import com.example.ui.ActiveSheet
import com.example.ui.ArenaViewModel
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ArenaViewModelTest {

    private fun viewModel(): ArenaViewModel {
        val app = ApplicationProvider.getApplicationContext<Application>()
        return ArenaViewModel(app)
    }

    @Test
    fun defaults_areSane() {
        val vm = viewModel()
        assertEquals(ARENA_HOME_URL, vm.currentUrl.value)
        assertEquals("Arena AI", vm.pageTitle.value)
        assertEquals(0f, vm.loadingProgress.value)
        assertFalse(vm.canGoBack.value)
        assertFalse(vm.canGoForward.value)
        assertFalse(vm.isDesktopMode.value)
        assertEquals(100, vm.textZoom.value)
        assertTrue(vm.activeSheet.value is ActiveSheet.None)
    }

    @Test
    fun toggleDesktopMode_flips() {
        val vm = viewModel()
        vm.toggleDesktopMode()
        assertTrue(vm.isDesktopMode.value)
        vm.toggleDesktopMode()
        assertFalse(vm.isDesktopMode.value)
    }

    @Test
    fun setTextZoom_clampsToSaneRange() {
        val vm = viewModel()
        vm.setTextZoom(130)
        assertEquals(130, vm.textZoom.value)
        vm.setTextZoom(30)
        assertEquals(50, vm.textZoom.value)
        vm.setTextZoom(500)
        assertEquals(200, vm.textZoom.value)
    }

    @Test
    fun setProgress_throttlesTinyUpdates() {
        val vm = viewModel()
        vm.setProgress(0.5f)
        assertEquals(0.5f, vm.loadingProgress.value)
        // 1% delta is below the 2% throttle threshold: ignored.
        vm.setProgress(0.51f)
        assertEquals(0.5f, vm.loadingProgress.value)
        // Completion always propagates so the bar hides.
        vm.setProgress(1f)
        assertEquals(1f, vm.loadingProgress.value)
    }

    @Test
    fun normalizeUrl_rejectsBlankAndNonHttp() {
        assertNull(ArenaViewModel.normalizeUrl("   "))
        assertNull(ArenaViewModel.normalizeUrl("javascript:alert(1)"))
        assertNull(ArenaViewModel.normalizeUrl("about:blank"))
        assertEquals("https://arena.ai/leaderboard", ArenaViewModel.normalizeUrl("https://arena.ai/leaderboard"))
        assertEquals("https://arena.ai/", ArenaViewModel.normalizeUrl("  https://arena.ai/  "))
    }

    @Test
    fun setUrl_ignoresInvalidAndDuplicateUrls() {
        val vm = viewModel()
        val before = vm.currentUrl.value
        vm.setUrl("not a url")
        assertEquals(before, vm.currentUrl.value)
        vm.setUrl("https://arena.ai/leaderboard")
        assertEquals("https://arena.ai/leaderboard", vm.currentUrl.value)
        // Duplicate must not trigger a reload loop.
        vm.setUrl("https://arena.ai/leaderboard")
        assertEquals("https://arena.ai/leaderboard", vm.currentUrl.value)
    }

    @Test
    fun onUrlObserved_tracksSpaNavigations() {
        val vm = viewModel()
        vm.onUrlObserved("https://arena.ai/history/search")
        assertEquals("https://arena.ai/history/search", vm.currentUrl.value)
        vm.onUrlObserved("")
        assertEquals("https://arena.ai/history/search", vm.currentUrl.value)
    }

    @Test
    fun isArenaDeepLink_matchesArenaHostsOnly() {
        assertTrue(ArenaViewModel.isArenaDeepLink(Uri.parse("https://arena.ai/")))
        assertTrue(ArenaViewModel.isArenaDeepLink(Uri.parse("https://www.arena.ai/leaderboard")))
        assertTrue(ArenaViewModel.isArenaDeepLink(Uri.parse("https://lmarena.ai/")))
        assertFalse(ArenaViewModel.isArenaDeepLink(Uri.parse("https://google.com/")))
        assertFalse(ArenaViewModel.isArenaDeepLink(Uri.parse("mailto:foo@bar.com")))
        // Lookalike hosts must not pass.
        assertFalse(ArenaViewModel.isArenaDeepLink(Uri.parse("https://arena.ai.evil.com/")))
        assertFalse(ArenaViewModel.isArenaDeepLink(Uri.parse("https://notarena.ai/")))
        assertTrue(ArenaViewModel.isArenaDeepLink(Uri.parse("https://sub.arena.ai/")))
    }

    @Test
    fun buildInjectionScript_safelyEscapesSpecialChars() {
        val tricky = "Say \"hi\"\nNew line \\ backslash \u2028 separator"
        val script = ArenaViewModel.buildInjectionScript(tricky)
        // JSONObject.quote escapes quotes, newlines and unicode separators.
        assertTrue(script.contains("\\\"hi\\\""))
        assertTrue(script.contains("\\n"))
        assertTrue(script.contains("\\\\"))
        assertTrue(script.contains("document.querySelectorAll"))
        assertTrue(script.contains("bubbles"))
    }

    @Test
    fun injectPrompt_failsGracefullyWithoutWebView() {
        val vm = viewModel()
        assertFalse(vm.injectPromptToArena("hello"))
        assertFalse(vm.injectPromptToArena("   "))
    }

    @Test
    fun injectPrompt_succeedsWithRegisteredWebView() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = viewModel()
        var evaluated: String? = null
        // Subclass to intercept JS evaluation: ShadowWebView does not
        // implement evaluateJavascript, so a plain WebView would crash here.
        val webView = object : WebView(app) {
            override fun evaluateJavascript(script: String?, resultCallback: ValueCallback<String>?) {
                evaluated = script
            }
        }
        try {
            vm.registerWebView(webView)
            assertTrue(vm.injectPromptToArena("hello arena"))
            assertNotNull(evaluated)
            assertTrue(evaluated!!.contains("hello arena"))
        } finally {
            vm.unregisterWebView()
            webView.destroy()
        }
    }
}
