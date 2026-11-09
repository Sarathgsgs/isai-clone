package com.isai.player.ui.widget

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.CircularProgressIndicator
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.isai.player.R
import com.isai.player.util.Formatters
import dagger.hilt.android.EntryPointAccessors

class IsaiGlanceWidget : GlanceAppWidget() {

    companion object {
        val SMALL = DpSize(100.dp, 100.dp)
        val MEDIUM = DpSize(220.dp, 100.dp)
        val LARGE = DpSize(260.dp, 180.dp)
    }

    override val sizeMode = SizeMode.Responsive(setOf(SMALL, MEDIUM, LARGE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java
        )
        val playerState = entryPoint.playerController().playerState.value
        val track = playerState.currentTrack
        val isPlaying = playerState.isPlaying

        val turntableBitmap = WidgetArtRenderer.renderTurntable(
            context = context,
            trackId = track?.id ?: 0L,
            artPath = track?.artPath,
            isPlaying = isPlaying
        )

        provideContent {
            GlanceTheme {
                val size = LocalSize.current
                when {
                    size.width >= LARGE.width && size.height >= LARGE.height -> {
                        LargeWidget(
                            context = context,
                            title = track?.title ?: context.getString(R.string.miniplayer_placeholder_title),
                            artist = track?.artist ?: context.getString(R.string.miniplayer_placeholder_artist),
                            album = track?.album ?: "",
                            positionMs = playerState.positionMs,
                            durationMs = playerState.durationMs,
                            progress = playerState.progress,
                            isPlaying = isPlaying,
                            turntable = turntableBitmap
                        )
                    }

                    size.width >= MEDIUM.width -> {
                        MediumWidget(
                            context = context,
                            title = track?.title ?: context.getString(R.string.miniplayer_placeholder_title),
                            artist = track?.artist ?: context.getString(R.string.miniplayer_placeholder_artist),
                            progress = playerState.progress,
                            isPlaying = isPlaying,
                            turntable = turntableBitmap
                        )
                    }

                    else -> {
                        SmallWidget(
                            context = context,
                            turntable = turntableBitmap,
                            isPlaying = isPlaying
                        )
                    }
                }
            }
        }
    }
}

class IsaiGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = IsaiGlanceWidget()
}

@Composable
private fun SmallWidget(
    context: Context,
    turntable: Bitmap,
    isPlaying: Boolean
) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(androidx.compose.ui.graphics.Color(0xFF16161B)))
            .cornerRadius(20.dp)
            .clickable(actionRunCallback<TogglePlayPauseAction>()),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = ImageProvider(turntable),
            contentDescription = "Turntable widget",
            modifier = GlanceModifier.fillMaxSize().padding(8.dp)
        )
    }
}

@Composable
private fun MediumWidget(
    context: Context,
    title: String,
    artist: String,
    progress: Float,
    isPlaying: Boolean,
    turntable: Bitmap
) {
    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(androidx.compose.ui.graphics.Color(0xFF16161B)))
            .cornerRadius(20.dp)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Turntable
        Image(
            provider = ImageProvider(turntable),
            contentDescription = null,
            modifier = GlanceModifier
                .size(76.dp)
                .clickable(actionStartActivity(openAppIntent(context)))
        )

        Spacer(modifier = GlanceModifier.width(12.dp))

        // Center: Track info & progress
        Column(
            modifier = GlanceModifier
                .defaultWeight()
                .fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = TextStyle(
                    color = ColorProvider(androidx.compose.ui.graphics.Color(0xFFF5F5F7)),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1
            )
            Spacer(modifier = GlanceModifier.height(2.dp))
            Text(
                text = artist,
                style = TextStyle(
                    color = ColorProvider(androidx.compose.ui.graphics.Color(0xFF9A9AA5)),
                    fontSize = 12.sp
                ),
                maxLines = 1
            )
            Spacer(modifier = GlanceModifier.height(6.dp))
            LinearProgressIndicator(
                progress = progress.coerceIn(0f, 1f),
                modifier = GlanceModifier.fillMaxWidth().height(3.dp),
                color = ColorProvider(androidx.compose.ui.graphics.Color(0xFFFF8A3D)),
                backgroundColor = ColorProvider(androidx.compose.ui.graphics.Color(0xFF2E2E38))
            )
        }

        Spacer(modifier = GlanceModifier.width(8.dp))

        // Right: Prev / Play / Next Controls
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "⏮",
                style = TextStyle(
                    color = ColorProvider(androidx.compose.ui.graphics.Color(0xFFF5F5F7)),
                    fontSize = 18.sp
                ),
                modifier = GlanceModifier
                    .padding(6.dp)
                    .clickable(actionRunCallback<SkipPreviousAction>())
            )
            Text(
                text = if (isPlaying) "⏸" else "▶",
                style = TextStyle(
                    color = ColorProvider(androidx.compose.ui.graphics.Color(0xFFFF8A3D)),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = GlanceModifier
                    .padding(8.dp)
                    .clickable(actionRunCallback<TogglePlayPauseAction>())
            )
            Text(
                text = "⏭",
                style = TextStyle(
                    color = ColorProvider(androidx.compose.ui.graphics.Color(0xFFF5F5F7)),
                    fontSize = 18.sp
                ),
                modifier = GlanceModifier
                    .padding(6.dp)
                    .clickable(actionRunCallback<SkipNextAction>())
            )
        }
    }
}

@Composable
private fun LargeWidget(
    context: Context,
    title: String,
    artist: String,
    album: String,
    positionMs: Long,
    durationMs: Long,
    progress: Float,
    isPlaying: Boolean,
    turntable: Bitmap
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(androidx.compose.ui.graphics.Color(0xFF16161B)))
            .cornerRadius(24.dp)
            .padding(16.dp)
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Turntable
            Image(
                provider = ImageProvider(turntable),
                contentDescription = null,
                modifier = GlanceModifier
                    .size(110.dp)
                    .clickable(actionStartActivity(openAppIntent(context)))
            )

            Spacer(modifier = GlanceModifier.width(16.dp))

            // Right: Detailed metadata
            Column(
                modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = TextStyle(
                        color = ColorProvider(androidx.compose.ui.graphics.Color(0xFFF5F5F7)),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
                Spacer(modifier = GlanceModifier.height(3.dp))
                Text(
                    text = artist,
                    style = TextStyle(
                        color = ColorProvider(androidx.compose.ui.graphics.Color(0xFFFF8A3D)),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1
                )
                if (album.isNotEmpty()) {
                    Spacer(modifier = GlanceModifier.height(2.dp))
                    Text(
                        text = album,
                        style = TextStyle(
                            color = ColorProvider(androidx.compose.ui.graphics.Color(0xFF9A9AA5)),
                            fontSize = 11.sp
                        ),
                        maxLines = 1
                    )
                }

                Spacer(modifier = GlanceModifier.height(10.dp))

                // Progress Bar
                LinearProgressIndicator(
                    progress = progress.coerceIn(0f, 1f),
                    modifier = GlanceModifier.fillMaxWidth().height(4.dp),
                    color = ColorProvider(androidx.compose.ui.graphics.Color(0xFFFF8A3D)),
                    backgroundColor = ColorProvider(androidx.compose.ui.graphics.Color(0xFF2E2E38))
                )

                Spacer(modifier = GlanceModifier.height(4.dp))

                // Time labels
                Row(modifier = GlanceModifier.fillMaxWidth()) {
                    Text(
                        text = Formatters.formatDurationMs(positionMs),
                        style = TextStyle(
                            color = ColorProvider(androidx.compose.ui.graphics.Color(0xFF9A9AA5)),
                            fontSize = 10.sp
                        )
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    Text(
                        text = Formatters.formatDurationMs(durationMs),
                        style = TextStyle(
                            color = ColorProvider(androidx.compose.ui.graphics.Color(0xFF9A9AA5)),
                            fontSize = 10.sp
                        )
                    )
                }
            }
        }

        Spacer(modifier = GlanceModifier.height(12.dp))

        // Bottom Controls Row
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "♡",
                style = TextStyle(
                    color = ColorProvider(androidx.compose.ui.graphics.Color(0xFF9A9AA5)),
                    fontSize = 20.sp
                ),
                modifier = GlanceModifier
                    .padding(horizontal = 14.dp)
                    .clickable(actionRunCallback<ToggleFavoriteAction>())
            )

            Text(
                text = "⏮",
                style = TextStyle(
                    color = ColorProvider(androidx.compose.ui.graphics.Color(0xFFF5F5F7)),
                    fontSize = 22.sp
                ),
                modifier = GlanceModifier
                    .padding(horizontal = 14.dp)
                    .clickable(actionRunCallback<SkipPreviousAction>())
            )

            Text(
                text = if (isPlaying) "⏸" else "▶",
                style = TextStyle(
                    color = ColorProvider(androidx.compose.ui.graphics.Color(0xFFFF8A3D)),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = GlanceModifier
                    .padding(horizontal = 18.dp)
                    .clickable(actionRunCallback<TogglePlayPauseAction>())
            )

            Text(
                text = "⏭",
                style = TextStyle(
                    color = ColorProvider(androidx.compose.ui.graphics.Color(0xFFF5F5F7)),
                    fontSize = 22.sp
                ),
                modifier = GlanceModifier
                    .padding(horizontal = 14.dp)
                    .clickable(actionRunCallback<SkipNextAction>())
            )
        }
    }
}

