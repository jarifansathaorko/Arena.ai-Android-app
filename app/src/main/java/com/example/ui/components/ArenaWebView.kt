package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.ArenaPrimary

private const val DESKTOP_USER_AGENT =
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ArenaWebView(
    currentUrl: String,
    isDesktopMode: Boolean,
    textZoom: Int,
    onProgressChange: (Float) -> Unit,
    onTitleChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onCanGoBackChange: (Boolean) -> Unit,
    onCanGoForwardChange: (Boolean) -> Unit,
    onWebViewCreated: (WebView) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    // Tracks the last URL we programmatically loaded so recompositions
    // (progress, title, rotation) never re-issue loadUrl() for it.
    var lastLoadedUrl by remember { mutableStateOf<String?>(null) }

    // File chooser launcher for multimodal vision uploads on Arena.ai
    var filePathCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val dataIntent = result.data
        val results: Array<Uri>? = if (result.resultCode == android.app.Activity.RESULT_OK) {
            when {
                dataIntent?.data != null -> arrayOf(dataIntent.data!!)
                dataIntent?.clipData != null -> {
                    val count = dataIntent.clipData!!.itemCount
                    val uris = Array(count) { i -> dataIntent.clipData!!.getItemAt(i).uri }
                    uris
                }
                else -> null
            }
        } else {
            null
        }
        filePathCallback?.onReceiveValue(results)
        filePathCallback = null
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    // Virtualized / Cloud Emulator rendernode guard:
                    // When running in headless/cloud or emulator containers lacking /dev/dri/renderD*
                    // nodes, fallback to LAYER_TYPE_SOFTWARE to prevent MESA driver rendernode errors.
                    val isVirtualOrEmulator = Build.FINGERPRINT.startsWith("generic") ||
                            Build.FINGERPRINT.startsWith("unknown") ||
                            Build.MODEL.contains("google_sdk") ||
                            Build.MODEL.contains("Emulator") ||
                            Build.MODEL.contains("Android SDK built for") ||
                            Build.HARDWARE.contains("goldfish") ||
                            Build.HARDWARE.contains("ranchu") ||
                            Build.PRODUCT.contains("sdk") ||
                            Build.PRODUCT.contains("emulator")

                    val hasHardwareGpuNode = try {
                        java.io.File("/dev/kgsl-3d0").exists() || // Qualcomm Adreno
                        java.io.File("/dev/mali0").exists() ||    // ARM Mali
                        java.io.File("/dev/nvhost-gpu").exists() || // Tegra
                        java.io.File("/dev/pvr_sync").exists() || // PowerVR
                        (java.io.File("/dev/dri").exists() && (java.io.File("/dev/dri").listFiles()?.isNotEmpty() == true))
                    } catch (e: Exception) {
                        false
                    }

                    if (isVirtualOrEmulator || !hasHardwareGpuNode) {
                        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                    } else {
                        setLayerType(View.LAYER_TYPE_HARDWARE, null)
                    }

                    // Cookie and storage configuration
                    val cookieManager = CookieManager.getInstance()
                    cookieManager.setAcceptCookie(true)
                    cookieManager.setAcceptThirdPartyCookies(this, true)

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        setSupportZoom(true)
                        builtInZoomControls = true
                        displayZoomControls = false
                        cacheMode = WebSettings.LOAD_DEFAULT
                        allowFileAccess = true
                        allowContentAccess = true
                        mediaPlaybackRequiresUserGesture = false
                        // Safe Browsing exists only on API 26+; guard for minSdk 24.
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            safeBrowsingEnabled = true
                        }

                        // Text zoom
                        this.textZoom = textZoom

                        // User agent
                        userAgentString = if (isDesktopMode) {
                            DESKTOP_USER_AGENT
                        } else {
                            WebSettings.getDefaultUserAgent(ctx)
                        }
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            super.onProgressChanged(view, newProgress)
                            onProgressChange(newProgress / 100f)
                        }

                        override fun onReceivedTitle(view: WebView?, title: String?) {
                            super.onReceivedTitle(view, title)
                            title?.let { onTitleChange(it) }
                        }

                        override fun onShowFileChooser(
                            view: WebView?,
                            callback: ValueCallback<Array<Uri>>?,
                            fileChooserParams: FileChooserParams?
                        ): Boolean {
                            filePathCallback?.onReceiveValue(null)
                            filePathCallback = callback

                            val intent = fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                                type = "*/*"
                                addCategory(Intent.CATEGORY_OPENABLE)
                            }
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                            try {
                                filePickerLauncher.launch(intent)
                            } catch (e: Exception) {
                                filePathCallback = null
                                return false
                            }
                            return true
                        }
                    }

                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            hasError = false
                            url?.let {
                                lastLoadedUrl = it
                                onUrlChange(it)
                            }
                            onCanGoBackChange(view?.canGoBack() == true)
                            onCanGoForwardChange(view?.canGoForward() == true)
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            url?.let {
                                lastLoadedUrl = it
                                onUrlChange(it)
                            }
                            onCanGoBackChange(view?.canGoBack() == true)
                            onCanGoForwardChange(view?.canGoForward() == true)
                        }

                        override fun doUpdateVisitedHistory(
                            view: WebView?,
                            url: String?,
                            isReload: Boolean
                        ) {
                            super.doUpdateVisitedHistory(view, url, isReload)
                            url?.let {
                                lastLoadedUrl = it
                                onUrlChange(it)
                            }
                            onCanGoBackChange(view?.canGoBack() == true)
                            onCanGoForwardChange(view?.canGoForward() == true)
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            super.onReceivedError(view, request, error)
                            if (request?.isForMainFrame == true) {
                                hasError = true
                                errorMessage = error?.description?.toString() ?: "Failed to connect to Arena"
                            }
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            val uri = request?.url ?: return false
                            val host = uri.host ?: ""

                            // Keep arena.ai and auth flows inside our webview
                            if (host.contains("arena.ai") ||
                                host.contains("lmarena.ai") ||
                                host.contains("lmsys.org") ||
                                host.contains("accounts.google.com") ||
                                host.contains("google.com/accounts") ||
                                host.contains("challenges.cloudflare.com")
                            ) {
                                return false
                            }

                            // Open external links (Twitter, Github, etc.) in system browser
                            return try {
                                val intent = Intent(Intent.ACTION_VIEW, uri)
                                ctx.startActivity(intent)
                                true
                            } catch (e: Exception) {
                                false
                            }
                        }

                        override fun onRenderProcessGone(
                            view: WebView?,
                            detail: RenderProcessGoneDetail?
                        ): Boolean {
                            // Returning true tells the framework we handled it,
                            // so the app survives instead of crashing.
                            if (detail?.didCrash() == true) {
                                // Genuine renderer crash: don't blindly reload
                                // (that risks a crash->reload loop). Surface the
                                // error overlay so the user retries manually.
                                hasError = true
                                errorMessage =
                                    "Arena's page renderer crashed. Your data is safe — tap retry to reload."
                                onCanGoBackChange(false)
                                onCanGoForwardChange(false)
                            } else {
                                // System killed the renderer for resources:
                                // in-place reload is safe here.
                                hasError = false
                                errorMessage = ""
                                view?.reload()
                            }
                            return true
                        }
                    }

                    loadUrl(currentUrl)
                    lastLoadedUrl = currentUrl
                    webViewRef = this
                    onWebViewCreated(this)
                }
            },
            update = { webView ->
                webViewRef = webView
                // Sync programmatic navigation (quick chips, deep links, session
                // reset). ViewModel.setUrl() only mutates state, so this is the
                // single loadUrl() path: no competing duplicate loads. In-page
                // SPA navigations already sync state via onUrlObserved, hence
                // the webView.url check.
                if (currentUrl.isNotBlank() && lastLoadedUrl != currentUrl && webView.url != currentUrl) {
                    lastLoadedUrl = currentUrl
                    webView.loadUrl(currentUrl)
                } else {
                    // Update User Agent if desktop mode changed
                    val targetUA = if (isDesktopMode) DESKTOP_USER_AGENT else null
                    if (webView.settings.userAgentString != targetUA && targetUA != null) {
                        webView.settings.userAgentString = targetUA
                        webView.reload()
                    } else if (!isDesktopMode && webView.settings.userAgentString == DESKTOP_USER_AGENT) {
                        // getDefaultUserAgent path: assigning an empty/default UA resets
                        // to the system default instead of relying on null semantics.
                        webView.settings.userAgentString = WebSettings.getDefaultUserAgent(context)
                        webView.reload()
                    }
                }

                // Update text zoom
                if (webView.settings.textZoom != textZoom) {
                    webView.settings.textZoom = textZoom
                }
            },
            onRelease = { webView ->
                webView.stopLoading()
                webView.destroy()
            },
            modifier = Modifier.fillMaxSize()
        )

        // Error / Offline State Overlay
        AnimatedVisibility(
            visible = hasError,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .testTag("webview_error_overlay"),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.WifiOff,
                        contentDescription = "Connection Error",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Unable to connect to Arena.ai",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (errorMessage.isNotBlank()) errorMessage else "Please check your internet connection and try again.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            hasError = false
                            webViewRef?.reload()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ArenaPrimary),
                        modifier = Modifier.testTag("webview_retry_button")
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Retry Connection")
                    }
                }
            }
        }
    }
}
