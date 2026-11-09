package com.isai.player.ui.components

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class TonearmGeometryTest {

    @Test
    fun defaultAnglesMatchSpecification() {
        val center = Offset(200f, 200f)
        val discRadius = 150f
        val geometry = TonearmGeometry.forDisc(center, discRadius)

        // Spec: alphaOuter is about 90 (+-2), alphaInner about 124 (+-3)
        assertEquals(90f, geometry.alphaOuter, 2.0f)
        assertEquals(124f, geometry.alphaInner, 3.0f)
        assertTrue("alphaRest must be less than alphaOuter", geometry.alphaRest < geometry.alphaOuter)
        assertEquals(geometry.alphaOuter - 28f, geometry.alphaRest, 0.001f)
    }

    @Test
    fun roundTripProgressToAngleToProgress() {
        val geometry = TonearmGeometry.forDisc(Offset(300f, 300f), 200f)

        listOf(0.0f, 0.15f, 0.5f, 0.73f, 1.0f).forEach { progress ->
            val angle = geometry.trackAngle(progress)
            val recoveredProgress = geometry.progressFromAngle(angle)
            assertEquals("Round trip failed for progress $progress", progress, recoveredProgress, 0.001f)
        }
    }

    @Test
    fun progressClamping() {
        val geometry = TonearmGeometry.forDisc(Offset(0f, 0f), 100f)

        assertEquals(0.0f, geometry.progressFromAngle(geometry.alphaRest), 0.001f)
        assertEquals(0.0f, geometry.progressFromAngle(geometry.alphaOuter - 10f), 0.001f)
        assertEquals(1.0f, geometry.progressFromAngle(geometry.alphaInner + 10f), 0.001f)

        assertEquals(geometry.alphaOuter, geometry.trackAngle(-0.5f), 0.001f)
        assertEquals(geometry.alphaInner, geometry.trackAngle(1.5f), 0.001f)
    }

    @Test
    fun isOnRecordEdgeCases() {
        val geometry = TonearmGeometry.forDisc(Offset(100f, 100f), 100f)

        assertTrue(geometry.isOnRecord(geometry.alphaOuter))
        assertTrue(geometry.isOnRecord(geometry.alphaInner))
        assertTrue(geometry.isOnRecord((geometry.alphaOuter + geometry.alphaInner) / 2f))

        // Tolerances: +- 2 degrees
        assertTrue(geometry.isOnRecord(geometry.alphaOuter - 1.9f))
        assertTrue(geometry.isOnRecord(geometry.alphaInner + 1.9f))
        assertFalse(geometry.isOnRecord(geometry.alphaOuter - 3.5f))
        assertFalse(geometry.isOnRecord(geometry.alphaRest))
    }

    @Test
    fun scaleInvariance() {
        val center = Offset(500f, 500f)
        val geometrySmall = TonearmGeometry.forDisc(center, 100f)
        val geometryLarge = TonearmGeometry.forDisc(center, 400f)

        assertEquals(geometrySmall.alphaOuter, geometryLarge.alphaOuter, 0.05f)
        assertEquals(geometrySmall.alphaInner, geometryLarge.alphaInner, 0.05f)
        assertEquals(geometrySmall.alphaRest, geometryLarge.alphaRest, 0.05f)
    }

    @Test
    fun stylusTouchPointsIntersectGrooveBoundaries() {
        val center = Offset(300f, 400f)
        val discRadius = 250f
        val geometry = TonearmGeometry.forDisc(center, discRadius)

        val stylusOuter = geometry.stylusPosition(geometry.alphaOuter)
        val distOuter = sqrt((stylusOuter.x - center.x) * (stylusOuter.x - center.x) +
                (stylusOuter.y - center.y) * (stylusOuter.y - center.y))
        assertEquals(geometry.rOuter, distOuter, 0.5f)

        val stylusInner = geometry.stylusPosition(geometry.alphaInner)
        val distInner = sqrt((stylusInner.x - center.x) * (stylusInner.x - center.x) +
                (stylusInner.y - center.y) * (stylusInner.y - center.y))
        assertEquals(geometry.rInner, distInner, 0.5f)
    }
}

