package com.osudroid.difficulty

import com.osudroid.GameMode
import com.osudroid.beatmaps.parser.BeatmapParser
import com.osudroid.mods.ModCustomSpeed
import com.osudroid.mods.ModDifficultyAdjust
import com.osudroid.mods.ModDoubleTime
import com.osudroid.mods.ModRelax
import com.osudroid.utils.ModHashMap
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2

@RunWith(RobolectricTestRunner::class)
class ServerScorePPTest {
    @Test
    fun `Test RX with custom circle size still produces stars and PP`() {
        Config.setOnlineUsername("test")
        val beatmap = BeatmapParser(
            TestResourceManager.getBeatmapFile(
                "YOASOBI - Love Letter (ohm002) [Please accept my overflowing emotions.]"
            )!!
        ).parse(true, GameMode.Standard)

        val mods = ModHashMap(
            listOf(
                ModRelax(),
                ModDifficultyAdjust(cs = 10f),
                ModDoubleTime(),
                ModCustomSpeed(2f)
            )
        )
        val stat = StatisticV2().apply {
            setCombo(beatmap.maxCombo)
            setHit300(beatmap.hitObjects.objects.size)
            setPlayerName("test")
        }

        val result = ServerScorePP.calculate(beatmap, mods, stat)
            ?: Assert.fail("RX must produce a server PP result") as Nothing

        Assert.assertTrue("star rating should be positive", result.stars > 0.0)
        Assert.assertTrue("RX PP should be positive", result.value > 0.0)
        Assert.assertTrue("max RX PP should be positive", result.max > 0.0)
    }
}
