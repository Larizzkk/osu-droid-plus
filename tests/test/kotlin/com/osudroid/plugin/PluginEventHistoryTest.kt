package com.osudroid.plugin

import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PluginEventHistoryTest {

    @Before
    fun setUp() {
        PluginEventHistory.clear()
    }

    @After
    fun tearDown() {
        PluginEventHistory.clear()
    }

    @Test
    fun `initial state has zero events`() {
        assertEquals(0, PluginEventHistory.getHitEventCount())
    }

    @Test
    fun `addHitEvent increments count`() {
        PluginEventHistory.addHitEvent(1000L, 1, 0.95f, 100f, 200f, 300, true)
        assertEquals(1, PluginEventHistory.getHitEventCount())
    }

    @Test
    fun `addHitEvent stores correct data`() {
        PluginEventHistory.addHitEvent(1000L, 42, 0.8f, 150.5f, 250.5f, 100, false)
        val event = PluginEventHistory.getHitEvent(0)
        assertNotNull(event)
        assertEquals(1000L, event!!.timeMs)
        assertEquals(42, event.objectId)
        assertEquals(0.8f, event.accuracy, 0.001f)
        assertEquals(150.5f, event.x, 0.001f)
        assertEquals(250.5f, event.y, 0.001f)
        assertEquals(100, event.scoreValue)
        assertFalse(event.endCombo)
    }

    @Test
    fun `circular buffer respects max capacity`() {
        for (i in 0..600) {
            PluginEventHistory.addHitEvent(i.toLong(), i, 1.0f, 0f, 0f, 300, true)
        }
        assertTrue(PluginEventHistory.getHitEventCount() <= 500)
    }

    @Test
    fun `getRecentHits returns correct count`() {
        for (i in 0..9) {
            PluginEventHistory.addHitEvent(i.toLong(), i, 1.0f, 0f, 0f, 300, true)
        }
        val recent = PluginEventHistory.getRecentHits(5)
        assertEquals(5, recent.size)
        assertEquals(5, recent[0].objectId)
        assertEquals(9, recent[4].objectId)
    }

    @Test
    fun `getRecentHits with count larger than available returns all`() {
        PluginEventHistory.addHitEvent(0L, 1, 1.0f, 0f, 0f, 300, true)
        val recent = PluginEventHistory.getRecentHits(100)
        assertEquals(1, recent.size)
    }

    @Test
    fun `getTimingErrors returns correct error values`() {
        PluginEventHistory.addHitEvent(0L, 1, 1.0f, 0f, 0f, 300, true)
        PluginEventHistory.addHitEvent(1L, 2, 0.5f, 0f, 0f, 100, true)
        PluginEventHistory.addHitEvent(2L, 3, 0.0f, 0f, 0f, 0, false)
        val errors = PluginEventHistory.getTimingErrors()
        assertEquals(3, errors.size)
        assertEquals(0f, errors[0], 0.001f)     // 1.0 accuracy = 0 error
        assertEquals(50f, errors[1], 0.001f)     // 0.5 accuracy = 50 error
        assertEquals(100f, errors[2], 0.001f)    // 0.0 accuracy = 100 error
    }

    @Test
    fun `clear resets all events`() {
        for (i in 0..9) {
            PluginEventHistory.addHitEvent(i.toLong(), i, 1.0f, 0f, 0f, 300, true)
        }
        PluginEventHistory.clear()
        assertEquals(0, PluginEventHistory.getHitEventCount())
    }

    @Test
    fun `getHitEvent returns null for out of bounds index`() {
        assertNull(PluginEventHistory.getHitEvent(-1))
        assertNull(PluginEventHistory.getHitEvent(0))
        PluginEventHistory.addHitEvent(0L, 1, 1.0f, 0f, 0f, 300, true)
        assertNull(PluginEventHistory.getHitEvent(1))
    }
}
