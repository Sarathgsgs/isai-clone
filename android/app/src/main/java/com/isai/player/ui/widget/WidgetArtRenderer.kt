package com.isai.player.ui.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.cos
import kotlin.math.sin

/**
 * Pre-renders a photorealistic turntable bitmap (vinyl disc + tonearm) for Jetpack Glance AppWidgets.
 *
 * Runs off the main thread and caches rendered bitmaps in memory using an LruCache
 * keyed by "${trackId}_${isPlaying}" to ensure high performance and zero IPC overhead.
 */
object WidgetArtRenderer {

    private const val BITMAP_SIZE = 300
    private val memoryCache = LruCache<String, Bitmap>(16)

    fun getCacheKey(trackId: Long, isPlaying: Boolean): String = "${trackId}_$isPlaying"

    suspend fun renderTurntable(
        context: Context,
        trackId: Long,
        artPath: String?,
        isPlaying: Boolean
    ): Bitmap = withContext(Dispatchers.Default) {
        val cacheKey = getCacheKey(trackId, isPlaying)
        val cached = memoryCache.get(cacheKey)
        if (cached != null && !cached.isRecycled) {
            return@withContext cached
        }

        val bitmap = Bitmap.createBitmap(BITMAP_SIZE, BITMAP_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val discCenter = BITMAP_SIZE * 0.44f
        val discRadius = BITMAP_SIZE * 0.40f

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 1. Vinyl Record Base (Dark vinyl with subtle radial sheen)
        paint.shader = RadialGradient(
            discCenter, discCenter, discRadius,
            intArrayOf(Color.rgb(32, 32, 38), Color.rgb(18, 18, 22), Color.rgb(8, 8, 10)),
            floatArrayOf(0.0f, 0.75f, 1.0f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(discCenter, discRadius, paint)
        paint.shader = null

        // 2. Vinyl Grooves (Subtle concentric rings)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.0f
        paint.color = Color.argb(30, 255, 255, 255)
        for (i in 1..12) {
            val r = discRadius * (0.50f + i * 0.038f)
            canvas.drawCircle(discCenter, discCenter, r, paint)
        }
        paint.style = Paint.Style.FILL

        // 3. Album Artwork / Center Label (Radius ~45% of disc)
        val artRadius = discRadius * 0.45f
        var artBitmap: Bitmap? = null
        if (!artPath.isNullOrEmpty()) {
            val file = File(artPath)
            if (file.exists()) {
                artBitmap = runCatching { BitmapFactory.decodeFile(file.absolutePath) }.getOrNull()
            }
        }

        if (artBitmap != null) {
            val scaledArt = Bitmap.createScaledBitmap(artBitmap, (artRadius * 2).toInt(), (artRadius * 2).toInt(), true)
            // Clip artwork to circular center label
            val artPathClip = Path().apply {
                addCircle(discCenter, discCenter, artRadius, Path.Direction.CW)
            }
            canvas.save()
            canvas.clipPath(artPathClip)
            canvas.drawBitmap(scaledArt, discCenter - artRadius, discCenter - artRadius, null)
            canvas.restore()
        } else {
            // Procedural Gradient Fallback Label
            paint.shader = LinearGradient(
                discCenter - artRadius, discCenter - artRadius,
                discCenter + artRadius, discCenter + artRadius,
                Color.rgb(255, 138, 61), Color.rgb(124, 77, 255),
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(discCenter, discCenter, artRadius, paint)
            paint.shader = null
        }

        // Center Spindle Hole
        paint.color = Color.rgb(240, 240, 245)
        canvas.drawCircle(discCenter, discCenter, discRadius * 0.06f, paint)
        paint.color = Color.rgb(15, 15, 18)
        canvas.drawCircle(discCenter, discCenter, discRadius * 0.025f, paint)

        // 4. Tonearm (Pivot at top right, stylus resting on groove if playing, parked if paused)
        val pivotX = BITMAP_SIZE * 0.88f
        val pivotY = BITMAP_SIZE * 0.16f

        // Stylus target: on outer groove if playing, parked right if paused
        val angleDeg = if (isPlaying) 100.0 else 64.0
        val angleRad = Math.toRadians(angleDeg)
        val armLength = BITMAP_SIZE * 0.58f

        val stylusX = pivotX - (cos(angleRad) * armLength).toFloat()
        val stylusY = pivotY + (sin(angleRad) * armLength).toFloat()

        // Pivot Base
        paint.color = Color.rgb(74, 78, 87)
        canvas.drawCircle(pivotX, pivotY, 14f, paint)
        paint.color = Color.rgb(169, 175, 186)
        canvas.drawCircle(pivotX, pivotY, 9f, paint)
        paint.color = Color.rgb(242, 244, 248)
        canvas.drawCircle(pivotX, pivotY, 4f, paint)

        // Arm Shadow
        paint.color = Color.argb(60, 0, 0, 0)
        paint.strokeWidth = 6.0f
        paint.strokeCap = Paint.Cap.ROUND
        canvas.drawLine(pivotX + 4f, pivotY + 4f, stylusX + 4f, stylusY + 4f, paint)

        // Arm Tube (Aluminum gradient line)
        paint.color = Color.rgb(210, 215, 225)
        paint.strokeWidth = 4.0f
        canvas.drawLine(pivotX, pivotY, stylusX, stylusY, paint)

        // Headshell / Cartridge at stylus tip
        paint.color = Color.rgb(23, 23, 27)
        canvas.drawCircle(stylusX, stylusY, 6.0f, paint)
        paint.color = Color.rgb(255, 138, 61) // Accent stylus dot
        canvas.drawCircle(stylusX, stylusY, 2.5f, paint)

        memoryCache.put(cacheKey, bitmap)
        bitmap
    }

    fun clearCache() {
        memoryCache.evictAll()
    }
}

