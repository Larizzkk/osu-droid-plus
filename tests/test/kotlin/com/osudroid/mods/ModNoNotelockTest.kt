package com.osudroid.mods

import com.osudroid.utils.allModsClassesByAcronym
import org.junit.After
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.game.GameHelper

@RunWith(RobolectricTestRunner::class)
class ModNoNotelockTest {
    @After
    fun resetGameHelper() {
        GameHelper.setNoNotelock(null)
    }

    @Test
    fun `Test No Notelock is registered and unlocks sliders`() {
        val mod = ModNoNotelock()

        Assert.assertEquals("No Notelock", mod.name)
        Assert.assertEquals("NL", mod.acronym)
        Assert.assertTrue(mod.isRanked)
        Assert.assertSame(ModNoNotelock::class, allModsClassesByAcronym["NL"])

        GameHelper.setNoNotelock(mod)

        Assert.assertTrue(GameHelper.isNoNotelock())
        Assert.assertTrue(Config.isRemoveSliderLock())
    }

    @Test
    fun `Test Random Plus is registered as a ranked, harder mod`() {
        val mod = ModRandomPlus()

        Assert.assertEquals("Random +", mod.name)
        Assert.assertEquals("R+", mod.acronym)
        Assert.assertTrue(mod.isRanked)
        Assert.assertEquals(220f, mod.minimumJump, 0f)
        Assert.assertEquals(0.3f, mod.edgeBias, 0f)
        Assert.assertSame(ModRandomPlus::class, allModsClassesByAcronym["R+"])
    }
}
