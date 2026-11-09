package com.isai.player.data.scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.isai.player.data.db.util.Fnv1a
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

@Singleton
class ArtCache @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val artDir: File by lazy {
        File(context.cacheDir, "art").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    /**
     * Downsamples and caches embedded album art bytes to disk as a 512x512 JPEG.
     *
     * @param uriString Track URI used to produce a deterministic cached filename.
     * @param pictureBytes Raw image bytes extracted from the audio file tags.
     * @return Absolute file path to the cached JPEG, or null if decoding failed.
     */
    suspend fun saveEmbeddedArt(uriString: String, pictureBytes: ByteArray): String? = withContext(Dispatchers.IO) {
        if (pictureBytes.isEmpty()) return@withContext null

        val hash = Fnv1a.hash64(uriString)
        val targetFile = File(artDir, "${hash}.jpg")

        if (targetFile.exists() && targetFile.length() > 0) {
            return@withContext targetFile.absolutePath
        }

        try {
            // First decode bounds only to calculate inSampleSize without loading full image into RAM
            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeByteArray(pictureBytes, 0, pictureBytes.size, boundsOptions)

            val width = boundsOptions.outWidth
            val height = boundsOptions.outHeight
            if (width <= 0 || height <= 0) return@withContext null

            // Target max dimension: 512px
            val targetDim = 512
            var inSampleSize = 1
            val maxDim = max(width, height)
            while ((maxDim / (inSampleSize * 2)) >= targetDim) {
                inSampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            val bitmap = BitmapFactory.decodeByteArray(pictureBytes, 0, pictureBytes.size, decodeOptions)
                ?: return@withContext null

            // Write atomically via temporary file
            val tempFile = File(artDir, "${hash}_${System.currentTimeMillis()}.tmp")
            FileOutputStream(tempFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                out.flush()
            }

            bitmap.recycle()

            if (tempFile.renameTo(targetFile)) {
                targetFile.absolutePath
            } else {
                tempFile.absolutePath
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Clears all cached album art files.
     */
    suspend fun clearCache() = withContext(Dispatchers.IO) {
        artDir.listFiles()?.forEach { file ->
            file.delete()
        }
    }
}

