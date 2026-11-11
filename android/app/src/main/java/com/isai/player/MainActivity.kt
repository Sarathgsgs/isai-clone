package com.isai.player

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLDecoder
import java.net.URLEncoder

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var webView: WebView? = null
    private var fileUploadCallback: ValueCallback<Array<Uri>>? = null

    // Fallback file chooser launcher for <input type="file">
    private val webChromeFileChooserLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val clipData = data?.clipData
            val uris = when {
                clipData != null -> {
                    (0 until clipData.itemCount).map { clipData.getItemAt(it).uri }.toTypedArray()
                }
                data?.data != null -> arrayOf(data.data!!)
                else -> null
            }
            fileUploadCallback?.onReceiveValue(uris)
        } else {
            fileUploadCallback?.onReceiveValue(null)
        }
        fileUploadCallback = null
    }

    // SAF Document Tree Folder Picker: Displays "USE THIS FOLDER" button on Android
    private val folderPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { treeUri: Uri? ->
        if (treeUri != null) {
            try {
                contentResolver.takePersistableUriPermission(
                    treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}

            lifecycleScope.launch(Dispatchers.IO) {
                scanAndImportFolder(treeUri)
            }
        }
    }

    // Multiple Audio Files Picker
    private val multiFilePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val clipData = data?.clipData
            val uris = when {
                clipData != null -> (0 until clipData.itemCount).map { clipData.getItemAt(it).uri }
                data?.data != null -> listOf(data.data!!)
                else -> emptyList()
            }
            if (uris.isNotEmpty()) {
                lifecycleScope.launch(Dispatchers.IO) {
                    processSelectedUris(uris)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        volumeControlStream = AudioManager.STREAM_MUSIC
        window.decorView.setBackgroundColor(Color.parseColor("#121212"))

        val isaiWebView = WebView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#121212"))
        }
        webView = isaiWebView

        // Apply Window Insets so UI is padded smoothly below phone status bar, clock, notch & network icons
        ViewCompat.setOnApplyWindowInsetsListener(isaiWebView) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars())
            view.setPadding(0, insets.top, 0, 0)
            windowInsets
        }

        setupWebView(isaiWebView)
        setupBackNavigation(isaiWebView)

        setContentView(isaiWebView)
        isaiWebView.loadUrl("https://isai.local/index.html")
    }

    private fun setupWebView(wv: WebView) {
        wv.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false
            allowFileAccess = true
            allowContentAccess = true
            allowFileAccessFromFileURLs = true
            allowUniversalAccessFromFileURLs = true
            useWideViewPort = true
            loadWithOverviewMode = true
            cacheMode = WebSettings.LOAD_DEFAULT
            setSupportZoom(false)
            displayZoomControls = false
        }

        // Bridge for Native SAF Folder Picking and Audio Scanning
        wv.addJavascriptInterface(IsaiAndroidBridge(), "IsaiAndroid")

        wv.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                view: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                fileUploadCallback?.onReceiveValue(null)
                fileUploadCallback = filePathCallback

                val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "audio/*"
                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                }
                webChromeFileChooserLauncher.launch(Intent.createChooser(intent, "Select Audio Tracks"))
                return true
            }

            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                android.util.Log.d(
                    "IsaiWeb",
                    "${consoleMessage?.message()} -- line ${consoleMessage?.lineNumber()} of ${consoleMessage?.sourceId()}"
                )
                return true
            }
        }

        wv.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                val url = request?.url ?: return null
                val host = url.host

                // Handle Audio Streaming for Native Content URIs
                if ((host == "isai.local" || host == "localhost") && url.path == "/content_stream") {
                    val encodedUri = url.getQueryParameter("uri")
                    if (!encodedUri.isNullOrBlank()) {
                        try {
                            val decodedUri = Uri.parse(URLDecoder.decode(encodedUri, "UTF-8"))
                            val inputStream = contentResolver.openInputStream(decodedUri)
                            val mimeType = contentResolver.getType(decodedUri) ?: "audio/mpeg"
                            val headers = mapOf(
                                "Access-Control-Allow-Origin" to "*",
                                "Access-Control-Allow-Methods" to "GET, OPTIONS",
                                "Access-Control-Allow-Headers" to "*",
                                "Accept-Ranges" to "bytes"
                            )
                            return WebResourceResponse(mimeType, "UTF-8", 200, "OK", headers, inputStream)
                        } catch (e: Exception) {
                            android.util.Log.e("IsaiWeb", "Error streaming content URI: $encodedUri", e)
                        }
                    }
                }

                // Handle Bundled App Assets
                if (host == "isai.local" || host == "localhost" || host == "appassets.androidplatform.net") {
                    var path = url.path?.removePrefix("/") ?: "index.html"
                    if (path.isEmpty() || path == "/") path = "index.html"

                    val assetPath = if (path.startsWith("dist/")) path else "dist/$path"
                    val cleanAssetPath = assetPath.substringBefore("?")

                    try {
                        val inputStream = assets.open(cleanAssetPath)
                        val mimeType = getMimeType(cleanAssetPath)
                        val headers = mapOf(
                            "Access-Control-Allow-Origin" to "*",
                            "Access-Control-Allow-Methods" to "GET, OPTIONS",
                            "Access-Control-Allow-Headers" to "*"
                        )
                        return WebResourceResponse(mimeType, "UTF-8", 200, "OK", headers, inputStream)
                    } catch (e: Exception) {
                        try {
                            // Fallback to index.html for SPA routing
                            val fallbackStream = assets.open("dist/index.html")
                            val headers = mapOf("Access-Control-Allow-Origin" to "*")
                            return WebResourceResponse("text/html", "UTF-8", 200, "OK", headers, fallbackStream)
                        } catch (_: Exception) {}
                    }
                }
                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                view?.setBackgroundColor(Color.parseColor("#121212"))
            }
        }
    }

    private fun setupBackNavigation(wv: WebView) {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                wv.evaluateJavascript("window.__isaiGoBack ? window.__isaiGoBack() : false") { result ->
                    val handled = result == "true"
                    if (!handled) {
                        if (wv.canGoBack()) {
                            wv.goBack()
                        } else {
                            isEnabled = false
                            onBackPressedDispatcher.onBackPressed()
                            isEnabled = true
                        }
                    }
                }
            }
        })
    }

    // JavaScript Interface for React Web App
    inner class IsaiAndroidBridge {
        @JavascriptInterface
        fun isAndroid(): Boolean = true

        @JavascriptInterface
        fun openFolderPicker() {
            runOnUiThread {
                try {
                    folderPickerLauncher.launch(null)
                } catch (e: Exception) {
                    android.util.Log.e("IsaiAndroid", "Failed to launch folder picker", e)
                }
            }
        }

        @JavascriptInterface
        fun openFilePicker() {
            runOnUiThread {
                try {
                    val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "audio/*"
                        putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                    }
                    multiFilePickerLauncher.launch(Intent.createChooser(intent, "Select Audio Tracks"))
                } catch (e: Exception) {
                    android.util.Log.e("IsaiAndroid", "Failed to launch multi-file picker", e)
                }
            }
        }
    }

    // Scan SAF Document Tree after user taps "USE THIS FOLDER"
    private fun scanAndImportFolder(treeUri: Uri) {
        val tracks = JSONArray()
        var folderName = "Music Folder"

        try {
            val rootDocId = if (DocumentsContract.isDocumentUri(this, treeUri)) {
                DocumentsContract.getDocumentId(treeUri)
            } else {
                DocumentsContract.getTreeDocumentId(treeUri)
            }

            val treeDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, rootDocId)
            contentResolver.query(
                treeDocUri,
                arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        val name = cursor.getString(nameIndex)
                        if (!name.isNullOrBlank()) folderName = name
                    }
                }
            }

            scanDirectoryDocuments(treeUri, rootDocId, folderName, tracks, maxDepth = 2, currentDepth = 0)
        } catch (e: Exception) {
            android.util.Log.e("IsaiAndroid", "Error scanning folder: ${e.message}", e)
        }

        runOnUiThread {
            val escapedName = JSONObject.quote(folderName)
            webView?.evaluateJavascript(
                "window.__isaiOnFolderImported && window.__isaiOnFolderImported($escapedName, $tracks);",
                null
            )
        }
    }

    private fun scanDirectoryDocuments(
        treeUri: Uri,
        parentDocId: String,
        folderName: String,
        results: JSONArray,
        maxDepth: Int,
        currentDepth: Int
    ) {
        if (currentDepth > maxDepth || results.length() >= 500) return
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)

        contentResolver.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE
            ),
            null,
            null,
            null
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
            val sizeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)

            while (cursor.moveToNext()) {
                val docId = if (idIdx >= 0) cursor.getString(idIdx) else null ?: continue
                val displayName = if (nameIdx >= 0) cursor.getString(nameIdx) else "Audio Track"
                val mimeType = if (mimeIdx >= 0) cursor.getString(mimeIdx) else ""
                val size = if (sizeIdx >= 0) cursor.getLong(sizeIdx) else 0L

                if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                    scanDirectoryDocuments(treeUri, docId, folderName, results, maxDepth, currentDepth + 1)
                } else if (isAudioFile(displayName, mimeType)) {
                    val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                    val streamUrl = "https://isai.local/content_stream?uri=" + URLEncoder.encode(docUri.toString(), "UTF-8")
                    val cleanTitle = displayName.substringBeforeLast(".").replace(Regex("^[0-9\\s._-]+"), "")

                    val trk = JSONObject().apply {
                        put("id", "tree-" + docId.hashCode().toString())
                        put("uri", docUri.toString())
                        put("streamUrl", streamUrl)
                        put("title", if (cleanTitle.isNotBlank()) cleanTitle else displayName)
                        put("artist", "Local Artist")
                        put("album", folderName)
                        put("folderPath", "$folderName/$displayName")
                        put("mimeType", if (mimeType.isNotBlank()) mimeType else "audio/mpeg")
                        put("sizeBytes", size)
                        put("durationMs", 180000L)
                    }
                    results.put(trk)
                }
            }
        }
    }

    private fun processSelectedUris(uris: List<Uri>) {
        val results = JSONArray()
        for (uri in uris) {
            try {
                var displayName = "Track"
                var size = 0L
                val mimeType = contentResolver.getType(uri) ?: "audio/mpeg"

                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (nameIdx >= 0) displayName = cursor.getString(nameIdx)
                        if (sizeIdx >= 0) size = cursor.getLong(sizeIdx)
                    }
                }

                val streamUrl = "https://isai.local/content_stream?uri=" + URLEncoder.encode(uri.toString(), "UTF-8")
                val cleanTitle = displayName.substringBeforeLast(".").replace(Regex("^[0-9\\s._-]+"), "")

                val trk = JSONObject().apply {
                    put("id", "file-" + uri.toString().hashCode().toString())
                    put("uri", uri.toString())
                    put("streamUrl", streamUrl)
                    put("title", if (cleanTitle.isNotBlank()) cleanTitle else displayName)
                    put("artist", "Local Artist")
                    put("album", "Imported Tracks")
                    put("folderPath", "Imported Tracks/$displayName")
                    put("mimeType", mimeType)
                    put("sizeBytes", size)
                    put("durationMs", 180000L)
                }
                results.put(trk)
            } catch (e: Exception) {
                android.util.Log.e("IsaiWeb", "Error reading URI: $uri", e)
            }
        }

        runOnUiThread {
            webView?.evaluateJavascript(
                "window.__isaiOnTracksImported && window.__isaiOnTracksImported($results);",
                null
            )
        }
    }

    private fun isAudioFile(name: String, mime: String): Boolean {
        if (mime.startsWith("audio/")) return true
        val lower = name.lowercase()
        return lower.endsWith(".mp3") || lower.endsWith(".m4a") || lower.endsWith(".flac") ||
               lower.endsWith(".wav") || lower.endsWith(".ogg") || lower.endsWith(".aac") ||
               lower.endsWith(".opus") || lower.endsWith(".amr")
    }

    private fun getMimeType(path: String): String {
        val lower = path.lowercase()
        return when {
            lower.endsWith(".html") -> "text/html"
            lower.endsWith(".js") || lower.endsWith(".mjs") -> "application/javascript"
            lower.endsWith(".css") -> "text/css"
            lower.endsWith(".png") -> "image/png"
            lower.endsWith(".jpg") || lower.endsWith(".jpeg") -> "image/jpeg"
            lower.endsWith(".webp") -> "image/webp"
            lower.endsWith(".svg") -> "image/svg+xml"
            lower.endsWith(".json") -> "application/json"
            lower.endsWith(".woff2") -> "font/woff2"
            lower.endsWith(".woff") -> "font/woff"
            lower.endsWith(".ttf") -> "font/ttf"
            lower.endsWith(".mp3") -> "audio/mpeg"
            lower.endsWith(".wav") -> "audio/wav"
            lower.endsWith(".ogg") -> "audio/ogg"
            else -> "application/octet-stream"
        }
    }

    override fun onDestroy() {
        webView?.destroy()
        webView = null
        super.onDestroy()
    }
}
