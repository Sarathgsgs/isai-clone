package com.isai.player.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.isai.player.data.repository.LiftBehavior
import com.isai.player.ui.theme.IsaiTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Stage ambient background:
 * - Dark theme: Blurred, darkened album art (65% black scrim) with 400ms crossfade.
 * - Light theme: Pale vertical gradient tinted by accent swatch at 12% alpha.
 */
@Composable
fun StageBackground(
    art: Any?,
    fallbackSeed: String,
    modifier: Modifier = Modifier
) {
    val isDark = IsaiTheme.colors.isDark
    val accent = IsaiTheme.colors.accent

    Box(modifier = modifier.fillMaxSize()) {
        if (isDark) {
            if (art != null) {
                AsyncImage(
                    model = art,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = 0.35f
                        }
                )
            } else {
                VinylFallbackArt(seed = fallbackSeed, modifier = Modifier.fillMaxSize())
            }
            // 65% black scrim for ambient depth
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
            )
        } else {
            // Light theme: pale vertical gradient tinted by accent
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.12f),
                            IsaiTheme.colors.background.copy(alpha = 0.95f),
                            IsaiTheme.colors.background
                        )
                    )
                )
            }
        }
    }
}

/**
 * Unified Now Playing "stage" composable synchronizing the disc rotation physics,
 * tonearm tracking, needle bounce, gestures, and track slide animations.
 */
@Composable
fun VinylStage(
    track: StageTrack?,
    isPlaying: Boolean,
    progress: () -> Float,
    durationMs: Long,
    lastSwipeDirection: SwipeDirection,
    liftBehavior: LiftBehavior,
    onTogglePlay: () -> Unit,
    onScrubStart: () -> Unit,
    onScrubPreview: (Float) -> Unit,
    onScrubEnd: (commit: Boolean, progress: Float) -> Unit,
    onLiftedStop: () -> Unit,
    onSwipeNext: () -> Unit,
    onSwipePrevious: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        val stageWidthPx = with(density) { maxWidth.toPx() }
        val stageHeightPx = with(density) { maxHeight.toPx() }

        // Disc sizing: responsive to viewport bounds
        val discSizeDp = minOf(maxWidth * 0.82f, maxHeight * 0.80f)
        val discSizePx = with(density) { discSizeDp.toPx() }
        val discRadiusPx = discSizePx / 2f

        // Center disc horizontally and place slightly below top so the tonearm has room
        val discCenterOffset = Offset(
            x = stageWidthPx / 2f,
            y = discRadiusPx + with(density) { 36.dp.toPx() }
        )

        // Geometry: pure Kotlin calculation pinned to disc center
        val geometry = remember(discCenterOffset, discRadiusPx) {
            TonearmGeometry.forDisc(discCenterOffset, discRadiusPx)
        }

        val tonearmState = rememberTonearmState(geometry, scope)
        val vinylColors = VinylColors.current()
        val tonearmColors = TonearmColors.current()

        // Sync rules: disc spins only when playing and not scrubbing
        var discSpinning by remember { mutableStateOf(false) }

        LaunchedEffect(isPlaying, tonearmState.isScrubbing) {
            if (isPlaying && !tonearmState.isScrubbing) {
                // Play: disc starts immediately, arm lowers over ~700ms with spring bounce
                discSpinning = true
                tonearmState.animateLower()
            } else {
                // Pause: arm starts lifting first, disc coasts down ~150ms later
                tonearmState.animatePark()
                delay(150L)
                discSpinning = false
            }
        }

        // Lift arm during track changes and re-drop if playing
        LaunchedEffect(track?.id) {
            if (track != null) {
                tonearmState.animatePark()
                delay(400L)
                if (isPlaying) {
                    tonearmState.animateLower()
                }
            }
        }

        // 1. Ambient Background Layer (Blurred art / tinted gradient)
        StageBackground(
            art = track?.art,
            fallbackSeed = track?.let { "${it.title} ${it.artist}" } ?: "Isai Offline Player"
        )

        // 2. Disc Layer with track slide transitions and horizontal swipe handling
        var discSwipeOffsetDx by remember { mutableFloatStateOf(0f) }
        val swipeThresholdPx = with(density) { 96.dp.toPx() }
        val hitRadiusPx = with(density) { 32.dp.toPx() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(geometry, track?.id) {
                    val pivot = geometry.pivot
                    val armUnit = geometry.armLength / 100f
                    val headshellMidAu = Offset(-1.5f, 91f)

                    awaitEachGesture {
                        val down: PointerInputChange = awaitFirstDown(requireUnconsumed = false)

                        // Compute current headshell center
                        val currentAngle = tonearmState.displayAngle(progress())
                        val rotRad = Math.toRadians((currentAngle - 90f).toDouble())
                        val cosR = cos(rotRad).toFloat()
                        val sinR = sin(rotRad).toFloat()
                        val rx = headshellMidAu.x * cosR - headshellMidAu.y * sinR
                        val ry = headshellMidAu.x * sinR + headshellMidAu.y * cosR
                        val headshellCenter = Offset(pivot.x + rx * armUnit, pivot.y + ry * armUnit)

                        val distToHeadshell = (down.position - headshellCenter).getDistance()
                        if (distToHeadshell <= hitRadiusPx) {
                            // Touch began on tonearm hit target -> Tonearm takes priority, block disc swipe!
                            return@awaitEachGesture
                        }

                        val velocityTracker = VelocityTracker()
                        velocityTracker.addPosition(down.uptimeMillis, down.position)
                        var totalDx = 0f
                        var lastX = down.position.x

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break

                            if (!change.pressed) {
                                // Swipe release
                                val velocity = velocityTracker.calculateVelocity()
                                if (totalDx < -swipeThresholdPx || velocity.x < -800f) {
                                    onSwipeNext()
                                } else if (totalDx > swipeThresholdPx || velocity.x > 800f) {
                                    onSwipePrevious()
                                }
                                discSwipeOffsetDx = 0f
                                break
                            }

                            val deltaX = change.position.x - lastX
                            totalDx += deltaX
                            discSwipeOffsetDx = totalDx
                            lastX = change.position.x
                            velocityTracker.addPosition(change.uptimeMillis, change.position)
                            change.consume()
                        }
                    }
                }
        ) {
            // Centered disc with slide transition
            Box(
                modifier = Modifier
                    .size(discSizeDp)
                    .offset(
                        x = with(density) { (discCenterOffset.x - discRadiusPx + discSwipeOffsetDx).toDp() },
                        y = with(density) { (discCenterOffset.y - discRadiusPx).toDp() }
                    )
            ) {
                AnimatedContent(
                    targetState = track,
                    transitionSpec = {
                        val isNext = lastSwipeDirection == SwipeDirection.NEXT ||
                                (lastSwipeDirection == SwipeDirection.NONE)
                        if (isNext) {
                            (slideInHorizontally(
                                initialOffsetX = { it },
                                animationSpec = tween(450, easing = FastOutSlowInEasing)
                            ) + fadeIn()).togetherWith(
                                slideOutHorizontally(
                                    targetOffsetX = { -it },
                                    animationSpec = tween(450, easing = FastOutSlowInEasing)
                                ) + fadeOut()
                            )
                        } else {
                            (slideInHorizontally(
                                initialOffsetX = { -it },
                                animationSpec = tween(450, easing = FastOutSlowInEasing)
                            ) + fadeIn()).togetherWith(
                                slideOutHorizontally(
                                    targetOffsetX = { it },
                                    animationSpec = tween(450, easing = FastOutSlowInEasing)
                                ) + fadeOut()
                            )
                        }
                    },
                    label = "DiscTrackChangeTransition"
                ) { currentTrack ->
                    key(currentTrack?.id) {
                        val discState = rememberVinylDiscState()
                        discState.SpinEffect(spinning = discSpinning)

                        VinylDisc(
                            state = discState,
                            art = currentTrack?.art,
                            fallbackSeed = currentTrack?.let { "${it.title} ${it.artist}" } ?: "Empty Track",
                            colors = vinylColors,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        // 3. Tonearm Layer and Interactive Hit Area Overlay (Rendered above the disc)
        TonearmInteractiveContainer(
            state = tonearmState,
            progress = progress,
            isPlaying = isPlaying,
            hasTrack = track != null,
            onScrubStart = onScrubStart,
            onScrubPreview = onScrubPreview,
            onScrubEnd = onScrubEnd,
            onLiftedStop = onLiftedStop,
            onTapToggle = onTogglePlay,
            colors = tonearmColors,
            modifier = Modifier.fillMaxSize()
        )
    }
}

