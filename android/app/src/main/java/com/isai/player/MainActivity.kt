package com.isai.player

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.media.MediaMetadataRetriever
import androidx.documentfile.provider.DocumentFile
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
import com.isai.player.data.model.Track
import com.isai.player.data.scanner.DiscoveredFile
import com.isai.player.data.scanner.MetadataExtractor
import com.isai.player.playback.PlayerController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import android.content.ContentUris
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import com.isai.player.data.db.dao.TrackDao
import com.isai.player.data.model.toEntity
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.net.URLDecoder
import java.net.URLEncoder
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var playerController: PlayerController

    @Inject
    lateinit var metadataExtractor: MetadataExtractor

    @Inject
    lateinit var trackDao: TrackDao

    private var webView: WebView? = null
    private var fileUploadCallback: ValueCallback<Array<Uri>>? = null

    // Permission launcher for Media & Notifications
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.any { it.value }
        if (granted) {
            lifecycleScope.launch(Dispatchers.IO) {
                scanMediaStoreAudioInternal()
            }
        }
    }

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
        observePlayerState(isaiWebView)

        setContentView(isaiWebView)
        isaiWebView.loadUrl("https://isai.local/index.html")
    }

    private fun observePlayerState(wv: WebView) {
        lifecycleScope.launch {
            playerController.playerState.collectLatest { state ->
                val isPlaying = state.isPlaying
                val currentPos = state.currentPositionMs
                val dur = state.durationMs
                wv.evaluateJavascript(
                    "window.__isaiOnTimeUpdate && window.__isaiOnTimeUpdate($currentPos, $dur); " +
                    "window.__isaiOnStateChange && window.__isaiOnStateChange($isPlaying);",
                    null
                )
            }
        }
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

        // Bridge for Native SAF Folder Picking, Audio Scanning & Native Media Playback
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

                // 1. Handle Cached Artwork Serving: https://isai.local/artwork?id=...
                if ((host == "isai.local" || host == "localhost") && url.path == "/artwork") {
                    val id = url.getQueryParameter("id")
                    if (!id.isNullOrBlank()) {
                        val cleanId = id.removePrefix("media-").removePrefix("tree-").removePrefix("file-")
                        val artFile = File(cacheDir, "art/$cleanId.jpg")
                        if (artFile.exists() && artFile.length() > 0) {
                            try {
                                val headers = mapOf(
                                    "Access-Control-Allow-Origin" to "*",
                                    "Cache-Control" to "public, max-age=86400"
                                )
                                return WebResourceResponse("image/jpeg", null, 200, "OK", headers, FileInputStream(artFile))
                            } catch (_: Exception) {}
                        }

                        // Try extracting directly from URI on demand
                        val uriParam = url.getQueryParameter("uri")
                        if (!uriParam.isNullOrBlank()) {
                            try {
                                val parsedUri = Uri.parse(uriParam)
                                val retriever = MediaMetadataRetriever()
                                val pfd = contentResolver.openFileDescriptor(parsedUri, "r")
                                if (pfd != null) {
                                    pfd.use { fd ->
                                        retriever.setDataSource(fd.fileDescriptor)
                                        val picBytes = retriever.embeddedPicture
                                        if (picBytes != null && picBytes.isNotEmpty()) {
                                            try {
                                                val artDir = File(cacheDir, "art")
                                                artDir.mkdirs()
                                                File(artDir, "$cleanId.jpg").writeBytes(picBytes)
                                            } catch (_: Exception) {}

                                            val headers = mapOf(
                                                "Access-Control-Allow-Origin" to "*",
                                                "Cache-Control" to "public, max-age=86400"
                                            )
                                            return WebResourceResponse(
                                                "image/jpeg",
                                                null,
                                                200,
                                                "OK",
                                                headers,
                                                ByteArrayInputStream(picBytes)
                                            )
                                        }
                                    }
                                }
                            } catch (_: Exception) {}
                        }

                        // Try loading MediaStore thumbnail if on Android 10+ (API 29+)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && id.startsWith("media-")) {
                            val trackId = cleanId.toLongOrNull()
                            if (trackId != null) {
                                try {
                                    val trackUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, trackId)
                                    val bitmap = contentResolver.loadThumbnail(trackUri, Size(512, 512), null)
                                    val baos = ByteArrayOutputStream()
                                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, baos)
                                    val headers = mapOf(
                                        "Access-Control-Allow-Origin" to "*",
                                        "Cache-Control" to "public, max-age=86400"
                                    )
                                    return WebResourceResponse("image/jpeg", null, 200, "OK", headers, ByteArrayInputStream(baos.toByteArray()))
                                } catch (_: Exception) {}
                            }
                        }

                        // Try loading album art URI
                        val albumId = url.getQueryParameter("albumId")?.toLongOrNull()
                        if (albumId != null && albumId >= 0) {
                            try {
                                val sArtworkUri = Uri.parse("content://media/external/audio/albumart")
                                val albumArtUri = ContentUris.withAppendedId(sArtworkUri, albumId)
                                val inputStream = contentResolver.openInputStream(albumArtUri)
                                if (inputStream != null) {
                                    val headers = mapOf(
                                        "Access-Control-Allow-Origin" to "*",
                                        "Cache-Control" to "public, max-age=86400"
                                    )
                                    return WebResourceResponse("image/jpeg", null, 200, "OK", headers, inputStream)
                                }
                            } catch (_: Exception) {}
                        }
                    }
                }

                // 2. Handle Audio Streaming with HTTP 206 Partial Content Range Requests
                if ((host == "isai.local" || host == "localhost") && url.path == "/content_stream") {
                    val encodedUri = url.getQueryParameter("uri")
                    if (!encodedUri.isNullOrBlank()) {
                        try {
                            val decodedUri = Uri.parse(URLDecoder.decode(encodedUri, "UTF-8"))
                            val mimeType = contentResolver.getType(decodedUri) ?: "audio/mpeg"
                            val afd = contentResolver.openAssetFileDescriptor(decodedUri, "r")
                            if (afd != null) {
                                val totalLength = afd.length
                                val rangeHeader = request.requestHeaders?.get("Range")

                                if (rangeHeader != null && rangeHeader.startsWith("bytes=") && totalLength > 0) {
                                    val rangeValues = rangeHeader.substring(6).split("-")
                                    val start = rangeValues[0].toLongOrNull() ?: 0L
                                    val end = if (rangeValues.size > 1 && rangeValues[1].isNotBlank()) {
                                        rangeValues[1].toLongOrNull() ?: (totalLength - 1)
                                    } else {
                                        totalLength - 1
                                    }
                                    val contentLength = (end - start + 1).coerceAtLeast(0L)
                                    val inputStream = afd.createInputStream()
                                    inputStream.skip(start)

                                    val headers = mapOf(
                                        "Access-Control-Allow-Origin" to "*",
                                        "Accept-Ranges" to "bytes",
                                        "Content-Range" to "bytes $start-$end/$totalLength",
                                        "Content-Length" to contentLength.toString(),
                                        "Access-Control-Expose-Headers" to "Content-Range, Content-Length, Accept-Ranges"
                                    )
                                    return WebResourceResponse(mimeType, "UTF-8", 206, "Partial Content", headers, inputStream)
                                } else {
                                    val headers = mutableMapOf(
                                        "Access-Control-Allow-Origin" to "*",
                                        "Accept-Ranges" to "bytes"
                                    )
                                    if (totalLength > 0) headers["Content-Length"] = totalLength.toString()
                                    return WebResourceResponse(mimeType, "UTF-8", 200, "OK", headers, afd.createInputStream())
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("IsaiWeb", "Error streaming content URI: $encodedUri", e)
                        }
                    }
                }

                // 3. Handle Bundled App Assets
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

        @JavascriptInterface
        fun scanAllDeviceAudio() {
            lifecycleScope.launch(Dispatchers.IO) {
                scanMediaStoreAudioInternal()
            }
        }

        @JavascriptInterface
        fun requestPermissions() {
            runOnUiThread {
                requestStorageAndNotificationPermissions()
            }
        }

        @JavascriptInterface
        fun playTrack(
            id: String,
            uri: String,
            title: String,
            artist: String,
            album: String,
            artPath: String,
            durationMs: Long,
            positionMs: Long
        ) {
            val trackId = try {
                id.removePrefix("tree-").removePrefix("file-").removePrefix("media-").toLong()
            } catch (_: Exception) {
                id.hashCode().toLong()
            }

            val track = Track(
                id = trackId,
                uri = uri,
                title = title.ifBlank { "Unknown Title" },
                titleSort = title.lowercase(),
                artist = artist.ifBlank { "Unknown Artist" },
                albumArtist = artist.ifBlank { "Unknown Artist" },
                album = album.ifBlank { "Unknown Album" },
                year = 2024,
                trackNo = 1,
                discNo = 1,
                durationMs = durationMs,
                mimeType = "audio/mpeg",
                sizeBytes = 0L,
                lastModified = 0L,
                artPath = if (artPath.isNotBlank()) artPath else null,
                folderPath = album,
                dateAdded = System.currentTimeMillis(),
                isMissing = false,
                relativePath = null,
                lyricsUri = null,
                hasLyrics = false
            )

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    trackDao.insertTrack(track.toEntity())
                } catch (e: Exception) {
                    android.util.Log.w("IsaiAndroid", "Error inserting track into DB: ${e.message}")
                }
            }

            runOnUiThread {
                try {
                    playerController.playTrack(track, listOf(track))
                    if (positionMs > 0) {
                        playerController.seekTo(positionMs)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("IsaiAndroid", "Error playing track: ${e.message}", e)
                }
            }
        }

        @JavascriptInterface
        fun pauseTrack() {
            runOnUiThread {
                try {
                    playerController.pause()
                } catch (_: Exception) {}
            }
        }

        @JavascriptInterface
        fun resumeTrack() {
            runOnUiThread {
                try {
                    playerController.play()
                } catch (_: Exception) {}
            }
        }

        @JavascriptInterface
        fun seekTo(positionMs: Long) {
            runOnUiThread {
                playerController.seekTo(positionMs)
            }
        }

        @JavascriptInterface
        fun setVolume(vol: Float) {
            runOnUiThread {
                playerController.setVolume(vol)
            }
        }

        @JavascriptInterface
        fun skipNext() {
            runOnUiThread {
                playerController.skipToNext()
            }
        }

        @JavascriptInterface
        fun skipPrevious() {
            runOnUiThread {
                playerController.skipToPrevious()
            }
        }
    }

    // Scan SAF Document Tree after user taps "USE THIS FOLDER"
    private suspend fun scanAndImportFolder(treeUri: Uri) {
        val tracks = JSONArray()
        var folderName = "Music Folder"
        val folderId = "folder-" + Math.abs(treeUri.toString().hashCode())

        try {
            val rootDocId = if (DocumentsContract.isDocumentUri(this, treeUri)) {
                DocumentsContract.getDocumentId(treeUri)
            } else {
                DocumentsContract.getTreeDocumentId(treeUri)
            }

            val docFile = DocumentFile.fromTreeUri(this, treeUri)
            val docFileName = docFile?.name
            if (!docFileName.isNullOrBlank()) {
                folderName = docFileName
            } else {
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
            }

            if (folderName == "Music Folder" && rootDocId.isNotBlank()) {
                val segment = rootDocId.substringAfterLast(':').substringAfterLast('/')
                if (segment.isNotBlank()) {
                    folderName = segment
                }
            }

            scanDirectoryDocuments(treeUri, rootDocId, folderName, tracks, maxDepth = 2, currentDepth = 0)
        } catch (e: Exception) {
            android.util.Log.e("IsaiAndroid", "Error scanning folder: ${e.message}", e)
        }

        runOnUiThread {
            val escapedName = JSONObject.quote(folderName)
            val escapedId = JSONObject.quote(folderId)
            webView?.evaluateJavascript(
                "window.__isaiOnFolderImported && window.__isaiOnFolderImported($escapedName, $tracks, $escapedId);",
                null
            )
        }
    }

    private suspend fun scanDirectoryDocuments(
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

                    // Extract actual ID3 tags: Title, Artist, Album, Duration & Embedded Artwork
                    val discoveredFile = DiscoveredFile(
                        uriString = docUri.toString(),
                        documentId = docId,
                        displayName = displayName,
                        mimeType = mimeType,
                        sizeBytes = size,
                        lastModified = System.currentTimeMillis(),
                        folderPath = folderName
                    )

                    var extractedTitle = displayName.substringBeforeLast(".").replace(Regex("^[0-9\\s._-]+"), "")
                    var extractedArtist = "Unknown Artist"
                    var extractedAlbum = folderName
                    var durationMs = 180000L
                    var artUrl: String? = null

                    try {
                        val trackEntity = metadataExtractor.extractTrack(discoveredFile)
                        if (trackEntity.title.isNotBlank()) extractedTitle = trackEntity.title
                        if (trackEntity.artist.isNotBlank()) extractedArtist = trackEntity.artist
                        if (trackEntity.album.isNotBlank()) extractedAlbum = trackEntity.album
                        if (trackEntity.durationMs > 0) durationMs = trackEntity.durationMs
                        val trackId = trackEntity.id.toString()
                        val encodedDocUri = URLEncoder.encode(docUri.toString(), "UTF-8")
                        artUrl = "https://isai.local/artwork?id=$trackId&uri=$encodedDocUri"
                        try {
                            trackDao.insertTrack(trackEntity)
                        } catch (_: Exception) {}
                    } catch (e: Exception) {
                        android.util.Log.w("IsaiMetadata", "Error reading metadata: ${e.message}")
                    }

                    if (artUrl == null) {
                        val fallbackId = Math.abs(docId.hashCode()).toString()
                        val encodedDocUri = URLEncoder.encode(docUri.toString(), "UTF-8")
                        artUrl = "https://isai.local/artwork?id=$fallbackId&uri=$encodedDocUri"
                    }

                    val trk = JSONObject().apply {
                        put("id", "tree-" + docId.hashCode().toString())
                        put("uri", docUri.toString())
                        put("streamUrl", streamUrl)
                        put("title", if (extractedTitle.isNotBlank()) extractedTitle else displayName)
                        put("artist", extractedArtist)
                        put("album", extractedAlbum)
                        put("artPath", artUrl)
                        put("folderPath", "$folderName/$displayName")
                        put("mimeType", if (mimeType.isNotBlank()) mimeType else "audio/mpeg")
                        put("sizeBytes", size)
                        put("durationMs", durationMs)
                    }
                    results.put(trk)
                }
            }
        }
    }

    private suspend fun processSelectedUris(uris: List<Uri>) {
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
                val discoveredFile = DiscoveredFile(
                    uriString = uri.toString(),
                    documentId = uri.toString().hashCode().toString(),
                    displayName = displayName,
                    mimeType = mimeType,
                    sizeBytes = size,
                    lastModified = System.currentTimeMillis(),
                    folderPath = "Imported Tracks"
                )

                var extractedTitle = displayName.substringBeforeLast(".").replace(Regex("^[0-9\\s._-]+"), "")
                var extractedArtist = "Unknown Artist"
                var extractedAlbum = "Imported Tracks"
                var durationMs = 180000L
                var artUrl: String? = null

                try {
                    val trackEntity = metadataExtractor.extractTrack(discoveredFile)
                    if (trackEntity.title.isNotBlank()) extractedTitle = trackEntity.title
                    if (trackEntity.artist.isNotBlank()) extractedArtist = trackEntity.artist
                    if (trackEntity.album.isNotBlank()) extractedAlbum = trackEntity.album
                    if (trackEntity.durationMs > 0) durationMs = trackEntity.durationMs
                    val trackId = trackEntity.id.toString()
                    val encodedUri = URLEncoder.encode(uri.toString(), "UTF-8")
                    artUrl = "https://isai.local/artwork?id=$trackId&uri=$encodedUri"
                    try {
                        trackDao.insertTrack(trackEntity)
                    } catch (_: Exception) {}
                } catch (_: Exception) {}

                if (artUrl == null) {
                    val fallbackId = Math.abs(uri.toString().hashCode()).toString()
                    val encodedUri = URLEncoder.encode(uri.toString(), "UTF-8")
                    artUrl = "https://isai.local/artwork?id=$fallbackId&uri=$encodedUri"
                }

                val trk = JSONObject().apply {
                    put("id", "file-" + uri.toString().hashCode().toString())
                    put("uri", uri.toString())
                    put("streamUrl", streamUrl)
                    put("title", if (extractedTitle.isNotBlank()) extractedTitle else displayName)
                    put("artist", extractedArtist)
                    put("album", extractedAlbum)
                    put("artPath", artUrl)
                    put("folderPath", "Imported Tracks/$displayName")
                    put("mimeType", mimeType)
                    put("sizeBytes", size)
                    put("durationMs", durationMs)
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

    fun requestStorageAndNotificationPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(android.Manifest.permission.READ_MEDIA_AUDIO)
            permissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
        } else {
            permissions.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    private suspend fun scanMediaStoreAudioInternal() {
        val tracks = JSONArray()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.ALBUM_ID
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 OR ${MediaStore.Audio.Media.DURATION} > 15000"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"
        val entitiesToInsert = mutableListOf<com.isai.player.data.db.entity.TrackEntity>()

        try {
            contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                val mimeCol = cursor.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE)
                val sizeCol = cursor.getColumnIndex(MediaStore.Audio.Media.SIZE)
                val albumIdCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)

                while (cursor.moveToNext() && tracks.length() < 1000) {
                    val id = cursor.getLong(idCol)
                    val title = cursor.getString(titleCol) ?: "Track $id"
                    val artist = cursor.getString(artistCol) ?: "Unknown Artist"
                    val album = cursor.getString(albumCol) ?: "Downloads & Storage"
                    val durationMs = cursor.getLong(durationCol).coerceAtLeast(0L)
                    val dataPath = if (dataCol >= 0) cursor.getString(dataCol) ?: "" else ""
                    val mime = if (mimeCol >= 0) cursor.getString(mimeCol) ?: "audio/mpeg" else "audio/mpeg"
                    val size = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0L
                    val albumId = if (albumIdCol >= 0) cursor.getLong(albumIdCol) else -1L

                    val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                    val streamUrl = "https://isai.local/content_stream?uri=" + URLEncoder.encode(contentUri.toString(), "UTF-8")
                    val artUrl = "https://isai.local/artwork?id=media-$id&albumId=$albumId"

                    val folderName = if (dataPath.isNotBlank() && dataPath.contains('/')) {
                        dataPath.substringBeforeLast('/').substringAfterLast('/')
                    } else {
                        album
                    }

                    val entity = com.isai.player.data.db.entity.TrackEntity(
                        id = id,
                        uri = contentUri.toString(),
                        title = title,
                        titleSort = title.lowercase(),
                        artist = artist,
                        albumArtist = artist,
                        album = album,
                        year = 2024,
                        trackNo = 1,
                        discNo = 1,
                        durationMs = if (durationMs > 0) durationMs else 180000L,
                        mimeType = mime,
                        sizeBytes = size,
                        lastModified = System.currentTimeMillis(),
                        artPath = artUrl,
                        folderPath = folderName,
                        dateAdded = System.currentTimeMillis(),
                        isMissing = false,
                        relativePath = null,
                        lyricsUri = null,
                        hasLyrics = false
                    )
                    entitiesToInsert.add(entity)

                    val trk = JSONObject().apply {
                        put("id", id.toString())
                        put("uri", contentUri.toString())
                        put("streamUrl", streamUrl)
                        put("title", title)
                        put("artist", artist)
                        put("album", album)
                        put("artPath", artUrl)
                        put("folderPath", "$folderName/$title")
                        put("mimeType", mime)
                        put("sizeBytes", size)
                        put("durationMs", entity.durationMs)
                    }
                    tracks.put(trk)
                }
            }

            if (entitiesToInsert.isNotEmpty()) {
                try {
                    trackDao.insertTracks(entitiesToInsert)
                } catch (e: Exception) {
                    android.util.Log.w("IsaiAndroid", "Failed to batch insert MediaStore tracks: ${e.message}")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("IsaiAndroid", "Error querying MediaStore: ${e.message}", e)
        }

        runOnUiThread {
            webView?.evaluateJavascript(
                "window.__isaiOnFolderImported && window.__isaiOnFolderImported(\"Phone Storage & Downloads\", $tracks);",
                null
            )
        }
    }

    override fun onDestroy() {
        webView?.destroy()
        webView = null
        super.onDestroy()
    }
}
