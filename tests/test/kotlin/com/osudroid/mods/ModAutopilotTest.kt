package com.osudroid.mods

import com.osudroid.utils.ModHashMap
import org.junit.Assert
import org.junit.Test

class ModAutopilotTest {
    @Test
    fun `Test Autopilot is compatible with No Fail`() {
        val autopilot = ModAutopilot()
        val noFail = ModNoFail()

        Assert.assertTrue(autopilot.isCompatibleWith(noFail))
        Assert.assertTrue(noFail.isCompatibleWith(autopilot))

        val mods = ModHashMap()
        mods.put(autopilot)
        mods.put(noFail)

        Assert.assertTrue(ModAutopilot::class in mods)
        Assert.assertTrue(ModNoFail::class in mods)
    }
}
