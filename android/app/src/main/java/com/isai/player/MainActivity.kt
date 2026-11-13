package com.isai.player

import android.Manifest
import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.LruCache
import android.util.Size
import android.view.View
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
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import com.isai.player.data.db.dao.TrackDao
import com.isai.player.data.db.entity.TrackEntity
import com.isai.player.data.model.Track
import com.isai.player.data.model.toEntity
import com.isai.player.data.scanner.DiscoveredFile
import com.isai.player.data.scanner.MetadataExtractor
import com.isai.player.playback.PlayerController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.net.URLDecoder
import java.net.URLEncoder
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.max

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var playerController: PlayerController

    @Inject
    lateinit var metadataExtractor: MetadataExtractor

    @Inject
    lateinit var trackDao: TrackDao

    private var webView: WebView? = null
    private var isPageLoaded: Boolean = false
    private val pendingJsScripts = mutableListOf<String>()
    private var fileUploadCallback: ValueCallback<Array<Uri>>? = null
    private var pendingMediaStoreScan: Boolean = false
    private var pendingDeleteTrackId: String? = null
    private var pendingDeleteTrackUri: String? = null

    // 12 MB fast RAM cache for decoded album art thumbnails to guarantee 60fps scrolling
    private val artMemoryCache = object : LruCache<String, ByteArray>(12 * 1024 * 1024) {
        override fun sizeOf(key: String, value: ByteArray): Int = value.size
    }

    // Permission launcher for Media & Notifications
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val audioPerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        val grantedAudio = permissions[audioPerm] == true || hasAudioPermission()
        if (pendingMediaStoreScan && grantedAudio) {
            pendingMediaStoreScan = false
            lifecycleScope.launch(Dispatchers.IO) {
                scanMediaStoreAudioInternal()
            }
        } else {
            pendingMediaStoreScan = false
        }
    }

    // System MediaStore Permanent Delete Confirmation Launcher (Android 10/11+)
    private val deleteMediaLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val id = pendingDeleteTrackId
        val uriStr = pendingDeleteTrackUri
        pendingDeleteTrackId = null
        pendingDeleteTrackUri = null

        if (id != null) {
            val deleted = result.resultCode == Activity.RESULT_OK
            if (deleted && uriStr != null) {
                lifecycleScope.launch(Dispatchers.IO) {
                    removeTrackFromDb(id, uriStr)
                }
            }
            val escapedId = JSONObject.quote(id)
            dispatchJsToWebView(
                "window.__isaiOnTrackDeleted && window.__isaiOnTrackDeleted($escapedId, $deleted);"
            )
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
            try {
                contentResolver.takePersistableUriPermission(
                    treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (_: Exception) {}

            notifyScanProgress(true, "Scanning selected folder...")
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
                for (u in uris) {
                    try {
                        contentResolver.takePersistableUriPermission(u, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    } catch (_: Exception) {}
                }
                notifyScanProgress(true, "Importing selected tracks...")
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
        window.decorView.setBackgroundColor(Color.parseColor("#000000"))

        val isaiWebView = WebView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#000000"))
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        webView = isaiWebView

        // Apply Window Insets so UI sits cleanly below status bar / camera cutout
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

        // Prompt for audio & notification permissions once on first launch
        val prefs = getSharedPreferences("isai_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("asked_initial_perms_v1", false)) {
            prefs.edit().putBoolean("asked_initial_perms_v1", true).apply()
            if (!hasAudioPermission() || !hasNotificationPermission()) {
                requestStorageAndNotificationPermissions()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        webView?.onResume()
        flushPendingJs()
    }

    private fun hasAudioPermission(): Boolean {
        val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun dispatchJsToWebView(script: String) {
        runOnUiThread {
            val wv = webView
            if (wv != null && isPageLoaded) {
                wv.evaluateJavascript(script, null)
            } else {
                synchronized(pendingJsScripts) {
                    pendingJsScripts.add(script)
                }
            }
        }
    }

    private fun flushPendingJs() {
        runOnUiThread {
            val wv = webView ?: return@runOnUiThread
            if (!isPageLoaded) return@runOnUiThread
            val copy: List<String>
            synchronized(pendingJsScripts) {
                if (pendingJsScripts.isEmpty()) return@runOnUiThread
                copy = pendingJsScripts.toList()
                pendingJsScripts.clear()
            }
            for (script in copy) {
                wv.evaluateJavascript(script, null)
            }
        }
    }

    private fun notifyScanProgress(scanning: Boolean, message: String = "") {
        val escapedMsg = JSONObject.quote(message)
        dispatchJsToWebView(
            "window.__isaiOnScanProgress && window.__isaiOnScanProgress($scanning, $escapedMsg);"
        )
    }

    private fun observePlayerState(wv: WebView) {
        lifecycleScope.launch {
            playerController.playerState.collectLatest { state ->
                val isPlaying = state.isPlaying
                val currentPos = state.currentPositionMs
                val dur = state.durationMs
                if (isPageLoaded) {
                    wv.evaluateJavascript(
                        "window.__isaiOnTimeUpdate && window.__isaiOnTimeUpdate($currentPos, $dur); " +
                        "window.__isaiOnStateChange && window.__isaiOnStateChange($isPlaying);",
                        null
                    )
                }
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

                // 1. Handle Artwork Serving with RAM + Disk + On-Demand Cache
                if ((host == "isai.local" || host == "localhost") && url.path == "/artwork") {
                    val id = url.getQueryParameter("id")
                    if (!id.isNullOrBlank()) {
                        val cleanId = id.removePrefix("media-").removePrefix("tree-").removePrefix("file-")
                        val headers = mapOf(
                            "Access-Control-Allow-Origin" to "*",
                            "Cache-Control" to "public, max-age=604800"
                        )

                        // A. In-memory LRU hit (< 0.1ms)
                        artMemoryCache.get(cleanId)?.let { cachedBytes ->
                            return WebResourceResponse("image/jpeg", null, 200, "OK", headers, ByteArrayInputStream(cachedBytes))
                        }

                        // B. Disk cache hit
                        val artFile = File(cacheDir, "art/$cleanId.jpg")
                        if (artFile.exists() && artFile.length() > 0) {
                            try {
                                val bytes = artFile.readBytes()
                                artMemoryCache.put(cleanId, bytes)
                                return WebResourceResponse("image/jpeg", null, 200, "OK", headers, ByteArrayInputStream(bytes))
                            } catch (_: Exception) {}
                        }

                        // C. On-demand extraction from audio URI
                        val uriParam = url.getQueryParameter("uri")
                        if (!uriParam.isNullOrBlank()) {
                            val retriever = MediaMetadataRetriever()
                            try {
                                val parsedUri = Uri.parse(uriParam)
                                contentResolver.openFileDescriptor(parsedUri, "r")?.use { fd ->
                                    retriever.setDataSource(fd.fileDescriptor)
                                    val picBytes = retriever.embeddedPicture
                                    if (picBytes != null && picBytes.isNotEmpty()) {
                                        val downsampled = downsampleArtToJpeg(picBytes)
                                        artMemoryCache.put(cleanId, downsampled)
                                        try {
                                            val artDir = File(cacheDir, "art")
                                            artDir.mkdirs()
                                            File(artDir, "$cleanId.jpg").writeBytes(downsampled)
                                        } catch (_: Exception) {}
                                        return WebResourceResponse(
                                            "image/jpeg",
                                            null,
                                            200,
                                            "OK",
                                            headers,
                                            ByteArrayInputStream(downsampled)
                                        )
                                    }
                                }
                            } catch (_: Exception) {
                            } finally {
                                try {
                                    retriever.release()
                                } catch (_: Exception) {}
                            }
                        }

                        // D. MediaStore thumbnail on Android 10+ (API 29+)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && id.startsWith("media-")) {
                            val trackId = cleanId.toLongOrNull()
                            if (trackId != null) {
                                try {
                                    val trackUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, trackId)
                                    val bitmap = contentResolver.loadThumbnail(trackUri, Size(512, 512), null)
                                    val baos = ByteArrayOutputStream()
                                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos)
                                    bitmap.recycle()
                                    val bytes = baos.toByteArray()
                                    artMemoryCache.put(cleanId, bytes)
                                    return WebResourceResponse("image/jpeg", null, 200, "OK", headers, ByteArrayInputStream(bytes))
                                } catch (_: Exception) {}
                            }
                        }

                        // E. Legacy album art URI
                        val albumId = url.getQueryParameter("albumId")?.toLongOrNull()
                        if (albumId != null && albumId >= 0) {
                            try {
                                val sArtworkUri = Uri.parse("content://media/external/audio/albumart")
                                val albumArtUri = ContentUris.withAppendedId(sArtworkUri, albumId)
                                contentResolver.openInputStream(albumArtUri)?.use { input ->
                                    val bytes = downsampleArtToJpeg(input.readBytes())
                                    artMemoryCache.put(cleanId, bytes)
                                    return WebResourceResponse("image/jpeg", null, 200, "OK", headers, ByteArrayInputStream(bytes))
                                }
                            } catch (_: Exception) {}
                        }
                    }

                    // Return clean 404 immediately instead of falling back to SPA index.html
                    return WebResourceResponse(
                        "text/plain",
                        "UTF-8",
                        404,
                        "Not Found",
                        mapOf("Access-Control-Allow-Origin" to "*"),
                        ByteArrayInputStream(ByteArray(0))
                    )
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
                view?.setBackgroundColor(Color.parseColor("#000000"))
                isPageLoaded = true
                flushPendingJs()
            }
        }
    }

    private fun downsampleArtToJpeg(rawBytes: ByteArray, targetDim: Int = 512): ByteArray {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, bounds)
            val maxDim = max(bounds.outWidth, bounds.outHeight)
            if (maxDim <= 0) return rawBytes
            if (maxDim <= targetDim && rawBytes.size <= 160 * 1024) return rawBytes

            var sampleSize = 1
            while ((maxDim / (sampleSize * 2)) >= targetDim) {
                sampleSize *= 2
            }
            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            val bitmap = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOpts)
                ?: return rawBytes
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos)
            bitmap.recycle()
            baos.toByteArray()
        } catch (_: Exception) {
            rawBytes
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
            if (!hasAudioPermission()) {
                pendingMediaStoreScan = true
                runOnUiThread {
                    requestStorageAndNotificationPermissions()
                }
                return
            }
            notifyScanProgress(true, "Scanning device music library...")
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
        fun removeTrackFromApp(id: String, uriString: String) {
            lifecycleScope.launch(Dispatchers.IO) {
                removeTrackFromDb(id, uriString)
            }
        }

        @JavascriptInterface
        fun deleteTrackFromDevice(id: String, uriString: String) {
            lifecycleScope.launch(Dispatchers.IO) {
                deleteTrackPermanently(id, uriString)
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
            val trackId = parseTrackId(id)

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

    private fun parseTrackId(id: String): Long {
        return try {
            id.removePrefix("tree-").removePrefix("file-").removePrefix("media-").toLong()
        } catch (_: Exception) {
            id.hashCode().toLong()
        }
    }

    private suspend fun removeTrackFromDb(id: String, uriString: String) {
        try {
            trackDao.deleteTrackByIdOrUri(parseTrackId(id), uriString)
        } catch (e: Exception) {
            android.util.Log.w("IsaiAndroid", "Failed to remove track from DB: ${e.message}")
        }
    }

    private suspend fun deleteTrackPermanently(id: String, uriString: String) {
        val escapedId = JSONObject.quote(id)
        try {
            val uri = Uri.parse(uriString)

            // 1. Try deleting directly if it's a writable SAF document URI
            if (uri.scheme == "content" && DocumentsContract.isDocumentUri(this, uri)) {
                try {
                    if (DocumentsContract.deleteDocument(contentResolver, uri)) {
                        removeTrackFromDb(id, uriString)
                        dispatchJsToWebView(
                            "window.__isaiOnTrackDeleted && window.__isaiOnTrackDeleted($escapedId, true);"
                        )
                        return
                    }
                } catch (_: Exception) {}
            }

            // 2. Resolve MediaStore URI for system deletion
            val mediaUri = resolveMediaStoreUri(uri)
            if (mediaUri == null) {
                dispatchJsToWebView(
                    "window.__isaiOnTrackDeleted && window.__isaiOnTrackDeleted($escapedId, false, \"Could not locate track in device MediaStore.\");"
                )
                return
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val pendingIntent = MediaStore.createDeleteRequest(contentResolver, listOf(mediaUri))
                withContext(Dispatchers.Main) {
                    pendingDeleteTrackId = id
                    pendingDeleteTrackUri = uriString
                    deleteMediaLauncher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
                }
            } else {
                try {
                    val rows = contentResolver.delete(mediaUri, null, null)
                    if (rows > 0) {
                        removeTrackFromDb(id, uriString)
                        dispatchJsToWebView(
                            "window.__isaiOnTrackDeleted && window.__isaiOnTrackDeleted($escapedId, true);"
                        )
                    } else {
                        dispatchJsToWebView(
                            "window.__isaiOnTrackDeleted && window.__isaiOnTrackDeleted($escapedId, false);"
                        )
                    }
                } catch (e: SecurityException) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && e is RecoverableSecurityException) {
                        withContext(Dispatchers.Main) {
                            pendingDeleteTrackId = id
                            pendingDeleteTrackUri = uriString
                            deleteMediaLauncher.launch(
                                IntentSenderRequest.Builder(e.userAction.actionIntent.intentSender).build()
                            )
                        }
                    } else {
                        dispatchJsToWebView(
                            "window.__isaiOnTrackDeleted && window.__isaiOnTrackDeleted($escapedId, false);"
                        )
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("IsaiAndroid", "Permanent delete failed: ${e.message}", e)
            dispatchJsToWebView(
                "window.__isaiOnTrackDeleted && window.__isaiOnTrackDeleted($escapedId, false);"
            )
        }
    }

    private fun resolveMediaStoreUri(uri: Uri): Uri? {
        if (uri.toString().startsWith("content://media/")) {
            return uri
        }
        var displayName: String? = null
        var size: Long = -1L
        try {
            contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIdx >= 0) displayName = cursor.getString(nameIdx)
                    if (sizeIdx >= 0) size = cursor.getLong(sizeIdx)
                }
            }
        } catch (_: Exception) {}

        if (displayName.isNullOrBlank()) return null

        return try {
            val selection = "${MediaStore.Audio.Media.DISPLAY_NAME} = ?"
            val args = arrayOf(displayName)
            contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.SIZE),
                selection,
                args,
                null
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(MediaStore.Audio.Media._ID)
                val sizeIdx = cursor.getColumnIndex(MediaStore.Audio.Media.SIZE)
                var fallbackId: Long? = null
                while (cursor.moveToNext()) {
                    val rowId = cursor.getLong(idIdx)
                    if (fallbackId == null) fallbackId = rowId
                    if (size > 0 && sizeIdx >= 0 && cursor.getLong(sizeIdx) == size) {
                        return ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, rowId)
                    }
                }
                fallbackId?.let {
                    ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, it)
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    // Backend Fingerprint Key to automatically omit duplicate tracks across folders/MediaStore
    private fun buildTrackFingerprint(
        title: String,
        artist: String,
        durationMs: Long,
        sizeBytes: Long
    ): String {
        val normTitle = title.trim().lowercase().replace(Regex("\\s+"), " ")
        val normArtist = artist.trim().lowercase().replace(Regex("\\s+"), " ")
        val durBucket = if (durationMs > 0L) durationMs / 3000L else if (sizeBytes > 0L) -(sizeBytes / 4096L) else 0L
        return "$normTitle|$normArtist|$durBucket"
    }

    // Scan SAF Document Tree after user taps "USE THIS FOLDER"
    private suspend fun scanAndImportFolder(treeUri: Uri) {
        val tracks = JSONArray()
        var folderName = "Music Folder"
        val folderId = "folder-" + abs(treeUri.toString().hashCode())

        try {
            val rootDocId = if (DocumentsContract.isDocumentUri(this, treeUri)) {
                DocumentsContract.getDocumentId(treeUri)
            } else {
                DocumentsContract.getTreeDocumentId(treeUri)
            }

            val docFileName = try {
                DocumentFile.fromTreeUri(this, treeUri)?.name
            } catch (_: Exception) {
                null
            }
            if (!docFileName.isNullOrBlank()) {
                folderName = docFileName
            } else {
                val treeDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, rootDocId)
                try {
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
                } catch (_: Exception) {}
            }

            if (folderName == "Music Folder" && rootDocId.isNotBlank()) {
                val segment = rootDocId.substringAfterLast(':').substringAfterLast('/')
                if (segment.isNotBlank()) {
                    folderName = segment
                }
            }

            // 1. Collect all audio files rapidly (up to depth 6, resilient per-subfolder)
            val discovered = mutableListOf<DiscoveredFile>()
            val seenUris = HashSet<String>()
            collectSafAudioFiles(treeUri, rootDocId, folderName, discovered, seenUris, maxDepth = 6, currentDepth = 0)

            // 2. Parallel metadata extraction (no blocking bitmap encoding during scan)
            val semaphore = Semaphore(6)
            val extractedList = coroutineScope {
                discovered.map { file ->
                    async(Dispatchers.IO) {
                        semaphore.withPermit {
                            try {
                                file to metadataExtractor.extractTrack(file, extractArtworkToDisk = false)
                            } catch (_: Exception) {
                                null
                            }
                        }
                    }
                }.awaitAll().filterNotNull()
            }

            // 3. Backend silent deduplication & batch DB insert
            val seenFingerprints = HashSet<String>()
            val entitiesToInsert = mutableListOf<TrackEntity>()

            for ((file, trackEntity) in extractedList) {
                val title = if (trackEntity.title.isNotBlank()) trackEntity.title else file.displayName
                val artist = if (trackEntity.artist.isNotBlank()) trackEntity.artist else "Unknown Artist"
                val album = if (trackEntity.album.isNotBlank()) trackEntity.album else folderName
                val dur = if (trackEntity.durationMs > 0L) trackEntity.durationMs else 180000L

                val fingerprint = buildTrackFingerprint(title, artist, dur, file.sizeBytes)
                if (!seenFingerprints.add(fingerprint)) {
                    continue // Omit duplicate track automatically in backend
                }

                entitiesToInsert.add(trackEntity)
                val encodedDocUri = URLEncoder.encode(file.uriString, "UTF-8")
                val streamUrl = "https://isai.local/content_stream?uri=$encodedDocUri"
                val artUrl = "https://isai.local/artwork?id=${trackEntity.id}&uri=$encodedDocUri"

                val trk = JSONObject().apply {
                    put("id", "tree-" + file.documentId.hashCode().toString())
                    put("uri", file.uriString)
                    put("streamUrl", streamUrl)
                    put("title", title)
                    put("artist", artist)
                    put("album", album)
                    put("artPath", artUrl)
                    put("folderPath", "$folderName/${file.displayName}")
                    put("mimeType", if (file.mimeType.isNotBlank()) file.mimeType else "audio/mpeg")
                    put("sizeBytes", file.sizeBytes)
                    put("durationMs", dur)
                }
                tracks.put(trk)
            }

            if (entitiesToInsert.isNotEmpty()) {
                try {
                    trackDao.insertTracks(entitiesToInsert)
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            android.util.Log.e("IsaiAndroid", "Error scanning folder: ${e.message}", e)
        } finally {
            notifyScanProgress(false, "")
        }

        val escapedName = JSONObject.quote(folderName)
        val escapedId = JSONObject.quote(folderId)
        if (tracks.length() == 0) {
            dispatchJsToWebView(
                "window.__isaiOnScanEmpty && window.__isaiOnScanEmpty($escapedName);"
            )
        } else {
            val escapedTracksJson = JSONObject.quote(tracks.toString())
            dispatchJsToWebView(
                "window.__isaiOnFolderImported && window.__isaiOnFolderImported($escapedName, JSON.parse($escapedTracksJson), $escapedId);"
            )
        }
    }

    private fun collectSafAudioFiles(
        treeUri: Uri,
        parentDocId: String,
        folderName: String,
        outFiles: MutableList<DiscoveredFile>,
        seenUris: MutableSet<String>,
        maxDepth: Int,
        currentDepth: Int
    ) {
        if (currentDepth > maxDepth || outFiles.size >= 800) return
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)

        try {
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

                while (cursor.moveToNext() && outFiles.size < 800) {
                    try {
                        val docId = if (idIdx >= 0) cursor.getString(idIdx) else null
                        if (docId.isNullOrBlank()) continue
                        val displayName = if (nameIdx >= 0) cursor.getString(nameIdx) ?: "Audio Track" else "Audio Track"
                        val mimeType = if (mimeIdx >= 0) cursor.getString(mimeIdx) ?: "" else ""
                        val size = if (sizeIdx >= 0) cursor.getLong(sizeIdx) else 0L

                        if (displayName.startsWith(".")) continue // skip hidden trash/temp folders & files

                        if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                            collectSafAudioFiles(treeUri, docId, folderName, outFiles, seenUris, maxDepth, currentDepth + 1)
                        } else if (isAudioFile(displayName, mimeType)) {
                            val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                            val uriStr = docUri.toString()
                            if (seenUris.add(uriStr)) {
                                outFiles.add(
                                    DiscoveredFile(
                                        uriString = uriStr,
                                        documentId = docId,
                                        displayName = displayName,
                                        mimeType = mimeType,
                                        sizeBytes = size,
                                        lastModified = System.currentTimeMillis(),
                                        folderPath = folderName
                                    )
                                )
                            }
                        }
                    } catch (_: Exception) {}
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("IsaiAndroid", "Skipping unreadable SAF directory $parentDocId: ${e.message}")
        }
    }

    private suspend fun processSelectedUris(uris: List<Uri>) {
        val results = JSONArray()
        val seenFingerprints = HashSet<String>()
        val entitiesToInsert = mutableListOf<TrackEntity>()

        try {
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

                    val encodedUri = URLEncoder.encode(uri.toString(), "UTF-8")
                    val streamUrl = "https://isai.local/content_stream?uri=$encodedUri"
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
                        val trackEntity = metadataExtractor.extractTrack(discoveredFile, extractArtworkToDisk = false)
                        if (trackEntity.title.isNotBlank()) extractedTitle = trackEntity.title
                        if (trackEntity.artist.isNotBlank()) extractedArtist = trackEntity.artist
                        if (trackEntity.album.isNotBlank()) extractedAlbum = trackEntity.album
                        if (trackEntity.durationMs > 0) durationMs = trackEntity.durationMs
                        artUrl = "https://isai.local/artwork?id=${trackEntity.id}&uri=$encodedUri"
                        entitiesToInsert.add(trackEntity)
                    } catch (_: Exception) {}

                    if (artUrl == null) {
                        val fallbackId = abs(uri.toString().hashCode()).toString()
                        artUrl = "https://isai.local/artwork?id=$fallbackId&uri=$encodedUri"
                    }

                    val finalTitle = if (extractedTitle.isNotBlank()) extractedTitle else displayName
                    val fingerprint = buildTrackFingerprint(finalTitle, extractedArtist, durationMs, size)
                    if (!seenFingerprints.add(fingerprint)) continue

                    val trk = JSONObject().apply {
                        put("id", "file-" + uri.toString().hashCode().toString())
                        put("uri", uri.toString())
                        put("streamUrl", streamUrl)
                        put("title", finalTitle)
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

            if (entitiesToInsert.isNotEmpty()) {
                try {
                    trackDao.insertTracks(entitiesToInsert)
                } catch (_: Exception) {}
            }
        } finally {
            notifyScanProgress(false, "")
        }

        if (results.length() > 0) {
            val escapedTracksJson = JSONObject.quote(results.toString())
            dispatchJsToWebView(
                "window.__isaiOnTracksImported && window.__isaiOnTracksImported(JSON.parse($escapedTracksJson));"
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
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
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

        val selectionBuilder = StringBuilder("(${MediaStore.Audio.Media.IS_MUSIC} != 0 OR ${MediaStore.Audio.Media.DURATION} > 15000)")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            selectionBuilder.append(" AND ${MediaStore.Audio.Media.IS_PENDING} = 0")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            selectionBuilder.append(" AND ${MediaStore.Audio.Media.IS_TRASHED} = 0")
        }

        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"
        val entitiesToInsert = mutableListOf<TrackEntity>()
        val seenFingerprints = HashSet<String>()

        try {
            contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selectionBuilder.toString(),
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
                    val title = cursor.getString(titleCol)?.trim() ?: "Track $id"
                    val artist = cursor.getString(artistCol)?.trim() ?: "Unknown Artist"
                    val album = cursor.getString(albumCol)?.trim() ?: "Downloads & Storage"
                    val durationMs = cursor.getLong(durationCol).coerceAtLeast(0L)
                    val dataPath = if (dataCol >= 0) cursor.getString(dataCol) ?: "" else ""
                    val mime = if (mimeCol >= 0) cursor.getString(mimeCol) ?: "audio/mpeg" else "audio/mpeg"
                    val size = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0L
                    val albumId = if (albumIdCol >= 0) cursor.getLong(albumIdCol) else -1L

                    if (size <= 0L) continue
                    if (dataPath.isNotBlank() && !File(dataPath).exists()) continue

                    val resolvedDuration = if (durationMs > 0) durationMs else 180000L
                    val fingerprint = buildTrackFingerprint(title, artist, resolvedDuration, size)
                    if (!seenFingerprints.add(fingerprint)) {
                        continue // Omit duplicate track automatically in backend
                    }

                    val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                    val encodedUri = URLEncoder.encode(contentUri.toString(), "UTF-8")
                    val streamUrl = "https://isai.local/content_stream?uri=$encodedUri"
                    val artUrl = "https://isai.local/artwork?id=media-$id&albumId=$albumId&uri=$encodedUri"

                    val folderName = if (dataPath.isNotBlank() && dataPath.contains('/')) {
                        dataPath.substringBeforeLast('/').substringAfterLast('/')
                    } else {
                        album
                    }

                    val entity = TrackEntity(
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
                        durationMs = resolvedDuration,
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
                        put("durationMs", resolvedDuration)
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
        } finally {
            notifyScanProgress(false, "")
        }

        if (tracks.length() == 0) {
            dispatchJsToWebView(
                "window.__isaiOnScanEmpty && window.__isaiOnScanEmpty(\"Phone Storage & Downloads\");"
            )
        } else {
            val escapedTracksJson = JSONObject.quote(tracks.toString())
            dispatchJsToWebView(
                "window.__isaiOnFolderImported && window.__isaiOnFolderImported(\"Phone Storage & Downloads\", JSON.parse($escapedTracksJson), \"folder-mediastore-all\");"
            )
        }
    }

    override fun onDestroy() {
        webView?.destroy()
        webView = null
        super.onDestroy()
    }
}
