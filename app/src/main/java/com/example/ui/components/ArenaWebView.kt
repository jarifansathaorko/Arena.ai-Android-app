package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.WebViewCommand
import com.example.ui.theme.ArenaPrimary
import com.example.ui.theme.Dimensions
import kotlinx.coroutines.flow.SharedFlow

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
    modifier: Modifier = Modifier,
    webViewCommands: SharedFlow<WebViewCommand>? = null
) {
    val context = LocalContext.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var lastLoadedUrl by remember { mutableStateOf<String?>(null) }
    var isFirstLoad by remember { mutableStateOf(true) }
    var currentProgress by remember { mutableStateOf(0f) }

    // Retained bundle for state preservation across recreations
    val webViewStateBundle = rememberSaveable { Bundle() }

    // Listen to reactive commands from ViewModel
    if (webViewCommands != null) {
        LaunchedEffect(webViewCommands) {
            webViewCommands.collect { command ->
                when (command) {
                    is WebViewCommand.Reload -> {
                        hasError = false
                        webViewRef?.reload()
                    }
                    is WebViewCommand.GoBack -> {
                        if (webViewRef?.canGoBack() == true) webViewRef?.goBack()
                    }
                    is WebViewCommand.GoForward -> {
                        if (webViewRef?.canGoForward() == true) webViewRef?.goForward()
                    }
                    is WebViewCommand.InjectPrompt -> {
                        webViewRef?.evaluateJavascript(command.script, null)
                    }
                    is WebViewCommand.ClearSession -> {
                        webViewRef?.clearCache(true)
                        webViewRef?.clearHistory()
                    }
                }
            }
        }
    }

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
                    Array(count) { i -> dataIntent.clipData!!.getItemAt(i).uri }
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

                    // Hardware vs Software rendering:
                    // Check for standard emulator indicators safely without inspecting low-level /dev nodes
                    val isEmulator = Build.FINGERPRINT.startsWith("generic") ||
                            Build.FINGERPRINT.startsWith("unknown") ||
                            Build.MODEL.contains("google_sdk") ||
                            Build.MODEL.contains("Emulator") ||
                            Build.HARDWARE.contains("goldfish") ||
                            Build.HARDWARE.contains("ranchu")

                    if (isEmulator) {
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
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        setSupportZoom(true)
                        builtInZoomControls = true
                        displayZoomControls = false
                        cacheMode = WebSettings.LOAD_DEFAULT
                        // Strict security: disable local file system access via file://
                        allowFileAccess = false
                        allowContentAccess = true
                        mediaPlaybackRequiresUserGesture = false

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            safeBrowsingEnabled = true
                        }

                        this.textZoom = textZoom

                        userAgentString = if (isDesktopMode) {
                            DESKTOP_USER_AGENT
                        } else {
                            WebSettings.getDefaultUserAgent(ctx)
                        }
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            super.onProgressChanged(view, newProgress)
                            val normalized = newProgress / 100f
                            currentProgress = normalized
                            onProgressChange(normalized)
                            if (newProgress >= 80) {
                                isFirstLoad = false
                            }
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

                            return try {
                                filePickerLauncher.launch(intent)
                                true
                            } catch (e: Exception) {
                                filePathCallback = null
                                false
                            }
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
                            isFirstLoad = false
                            url?.let {
                                lastLoadedUrl = it
                                onUrlChange(it)
                            }
                            onCanGoBackChange(view?.canGoBack() == true)
                            onCanGoForwardChange(view?.canGoForward() == true)
                            // Save state for recreation
                            view?.saveState(webViewStateBundle)
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
                                isFirstLoad = false
                                errorMessage = error?.description?.toString() ?: "Failed to connect to Arena"
                                onProgressChange(1f)
                            }
                        }

                        override fun onReceivedSslError(
                            view: WebView?,
                            handler: SslErrorHandler?,
                            error: SslError?
                        ) {
                            // Enforce SSL security: never proceed on invalid SSL certificates
                            handler?.cancel()
                            hasError = true
                            isFirstLoad = false
                            errorMessage = "Secure connection to Arena failed (SSL error code: ${error?.primaryError}). Connection halted for your security."
                            onProgressChange(1f)
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            val uri = request?.url ?: return false
                            val scheme = uri.scheme?.lowercase() ?: ""
                            val host = uri.host?.lowercase() ?: ""

                            // Keep arena.ai and authentication flows inside this webview
                            if (host.contains("arena.ai") ||
                                host.contains("lmarena.ai") ||
                                host.contains("lmsys.org") ||
                                host.contains("accounts.google.com") ||
                                host.contains("google.com/accounts") ||
                                host.contains("challenges.cloudflare.com")
                            ) {
                                return false
                            }

                            // Handle mailto:, tel: and external URLs cleanly
                            return try {
                                val intent = when (scheme) {
                                    "mailto" -> Intent(Intent.ACTION_SENDTO, uri)
                                    "tel" -> Intent(Intent.ACTION_DIAL, uri)
                                    else -> Intent(Intent.ACTION_VIEW, uri)
                                }
                                ctx.startActivity(intent)
                                true
                            } catch (_: Exception) {
                                false
                            }
                        }

                        override fun onRenderProcessGone(
                            view: WebView?,
                            detail: RenderProcessGoneDetail?
                        ): Boolean {
                            if (detail?.didCrash() == true) {
                                hasError = true
                                isFirstLoad = false
                                errorMessage = "Arena's render process crashed. Your state is preserved — tap retry to reload."
                                onCanGoBackChange(false)
                                onCanGoForwardChange(false)
                                onProgressChange(1f)
                            } else {
                                hasError = false
                                errorMessage = ""
                                view?.reload()
                            }
                            return true
                        }
                    }

                    // Restore state if bundle exists, else load initial URL
                    if (!webViewStateBundle.isEmpty) {
                        restoreState(webViewStateBundle)
                    } else {
                        loadUrl(currentUrl)
                    }
                    lastLoadedUrl = currentUrl
                    webViewRef = this
                    onWebViewCreated(this)
                }
            },
            update = { webView ->
                webViewRef = webView
                if (currentUrl.isNotBlank() && lastLoadedUrl != currentUrl && webView.url != currentUrl) {
                    lastLoadedUrl = currentUrl
                    webView.loadUrl(currentUrl)
                } else {
                    val targetUA = if (isDesktopMode) DESKTOP_USER_AGENT else null
                    if (webView.settings.userAgentString != targetUA && targetUA != null) {
                        webView.settings.userAgentString = targetUA
                        webView.reload()
                    } else if (!isDesktopMode && webView.settings.userAgentString == DESKTOP_USER_AGENT) {
                        webView.settings.userAgentString = WebSettings.getDefaultUserAgent(context)
                        webView.reload()
                    }
                }

                if (webView.settings.textZoom != textZoom) {
                    webView.settings.textZoom = textZoom
                }
            },
            onRelease = { webView ->
                webView.stopLoading()
                webView.saveState(webViewStateBundle)
                webView.destroy()
            },
            modifier = Modifier.fillMaxSize()
        )

        // Native Loading Skeleton (replaces white flash on startup)
        AnimatedVisibility(
            visible = isFirstLoad && !hasError && currentProgress < 0.85f,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            ArenaSkeletonPlaceholder()
        }

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
                Card(
                    shape = RoundedCornerShape(Dimensions.radiusSheet),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    ),
                    modifier = Modifier
                        .padding(Dimensions.space2xl)
                        .widthIn(max = 380.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(Dimensions.space2xl)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                            modifier = Modifier.size(80.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.WifiOff,
                                    contentDescription = "Connection Error",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(Dimensions.spaceStandard))
                        Text(
                            text = "Connection Issue",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(Dimensions.spaceSmall))
                        Text(
                            text = if (errorMessage.isNotBlank()) errorMessage else "Unable to load Arena.ai. Please check your connection and retry.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(Dimensions.space2xl))
                        Button(
                            onClick = {
                                hasError = false
                                webViewRef?.reload()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ArenaPrimary),
                            shape = RoundedCornerShape(Dimensions.radiusSmall),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(Dimensions.minTouchTarget)
                                .testTag("webview_retry_button")
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(Dimensions.spaceSmall))
                            Text("Retry Connection")
                        }
                        Spacer(modifier = Modifier.height(Dimensions.spaceSmall))
                        OutlinedButton(
                            onClick = {
                                try {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl))
                                    )
                                } catch (_: Exception) { }
                            },
                            shape = RoundedCornerShape(Dimensions.radiusSmall),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(Dimensions.minTouchTarget)
                                .testTag("webview_open_browser_button")
                        ) {
                            Icon(imageVector = Icons.Default.OpenInBrowser, contentDescription = null)
                            Spacer(modifier = Modifier.width(Dimensions.spaceSmall))
                            Text("Open in System Browser")
                        }
                    }
                }
            }
        }
    }
}
