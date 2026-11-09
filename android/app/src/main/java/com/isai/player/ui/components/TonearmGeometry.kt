package com.isai.player.ui.components

import androidx.compose.ui.geometry.Offset
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Pure geometry model for the Isai tonearm.
 *
 * Angles are in degrees in screen coordinates (+X right, +Y down),
 * describing the direction vector from [pivot] to stylus tip [stylusPosition].
 */
class TonearmGeometry(
    val center: Offset,
    val discRadius: Float,
    val pivot: Offset,
    val armLength: Float,
    val innerRadiusFraction: Float = 0.15f,
    val outerMarginFraction: Float = 0.04f
) {
    val rOuter: Float = discRadius * (1f - outerMarginFraction)
    val rInner: Float = discRadius * innerRadiusFraction

    val alphaOuter: Float
    val alphaInner: Float
    val alphaRest: Float

    init {
        val dx = center.x - pivot.x
        val dy = center.y - pivot.y
        val d = sqrt(dx * dx + dy * dy)
        val thetaRad = atan2(dy, dx)
        val thetaDeg = Math.toDegrees(thetaRad.toDouble()).toFloat()

        // Solve circle-circle intersection for outer groove:
        alphaOuter = solveIntersectionAngle(d, armLength, rOuter, thetaDeg)

        // Solve circle-circle intersection for inner groove:
        alphaInner = solveIntersectionAngle(d, armLength, rInner, thetaDeg)

        // Parked position lifted off the record:
        alphaRest = alphaOuter - 28f
    }

    /**
     * Calculates the position of the stylus tip in screen coordinates for a given arm angle [alphaDeg].
     */
    fun stylusPosition(alphaDeg: Float): Offset {
        val rad = Math.toRadians(alphaDeg.toDouble())
        val x = pivot.x + armLength * cos(rad).toFloat()
        val y = pivot.y + armLength * sin(rad).toFloat()
        return Offset(x, y)
    }

    /**
     * Maps playback progress [0..1] to the corresponding tonearm angle in degrees.
     */
    fun trackAngle(progress: Float): Float {
        val safeProgress = if (progress.isNaN() || progress.isInfinite()) 0f else progress
        val p = safeProgress.coerceIn(0f, 1f)
        return alphaOuter + p * (alphaInner - alphaOuter)
    }

    /**
     * Maps a given tonearm angle [alphaDeg] to normalized playback progress [0..1].
     */
    fun progressFromAngle(alphaDeg: Float): Float {
        if (alphaDeg.isNaN() || alphaDeg.isInfinite() || alphaInner == alphaOuter) return 0f
        return ((alphaDeg - alphaOuter) / (alphaInner - alphaOuter)).coerceIn(0f, 1f)
    }

    /**
     * Determines whether the given tonearm angle [alphaDeg] is over the playable vinyl grooves.
     */
    fun isOnRecord(alphaDeg: Float): Boolean {
        val minAngle = minOf(alphaOuter, alphaInner) - 2f
        val maxAngle = maxOf(alphaOuter, alphaInner) + 2f
        return alphaDeg in minAngle..maxAngle
    }

    private fun solveIntersectionAngle(d: Float, l: Float, r: Float, thetaDeg: Float): Float {
        val cosDelta = ((l * l + d * d - r * r) / (2f * l * d)).coerceIn(-1f, 1f)
        val deltaRad = acos(cosDelta.toDouble())
        val deltaDeg = Math.toDegrees(deltaRad).toFloat()
        // Solution on the near (playable) side of the disc:
        return thetaDeg - deltaDeg
    }

    companion object {
        /**
         * Factory producing standard tonearm layout scaled to [discRadius] and positioned at [center]:
         * - Pivot P = center + (0.95R, -1.35R) (sits top-right, partly off-screen)
         * - Arm length L = 1.5R
         */
        fun forDisc(
            center: Offset,
            discRadius: Float,
            innerRadiusFraction: Float = 0.15f,
            outerMarginFraction: Float = 0.04f
        ): TonearmGeometry {
            val pivot = Offset(
                x = center.x + 0.95f * discRadius,
                y = center.y - 1.35f * discRadius
            )
            val armLength = 1.5f * discRadius
            return TonearmGeometry(
                center = center,
                discRadius = discRadius,
                pivot = pivot,
                armLength = armLength,
                innerRadiusFraction = innerRadiusFraction,
                outerMarginFraction = outerMarginFraction
            )
        }
    }
}

