package ru.nsu.ccfit.zuev.osu.scoring

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import ru.nsu.ccfit.zuev.osu.Config

@RunWith(RobolectricTestRunner::class)
class StatisticV2Test {
    @Test
    fun `slider parts use the osu standard combo multiplier`() {
        Config.setOnlineUsername("Test")
        val stat = StatisticV2()
        stat.setDiffModifier(2.5f)

        // Slider head: 30 + 30 * 1 * 2.5 / 25 = 33
        stat.registerHit(30, false, false)
        // Slider tick: 10 + 10 * 2 * 2.5 / 25 = 12
        stat.registerHit(10, false, false)
        // Slider end is judged as 300 but awards 30: 30 + 30 * 3 * 2.5 / 25 = 39
        stat.registerHit(300, false, false, true, 30)

        Assert.assertEquals(84L, stat.getTotalScore())

        // Hit circle: 300 + 300 * 4 * 2.5 / 25 = 420
        stat.registerHit(300, false, false)

        Assert.assertEquals(504L, stat.getTotalScore())
    }
}
