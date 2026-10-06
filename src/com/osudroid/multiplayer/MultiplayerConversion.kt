@file:JvmName("MultiplayerConverter")

package com.osudroid.multiplayer

import com.rian.osu.utils.ModUtils.deserializeMods
import org.json.JSONObject
import ru.nsu.ccfit.zuev.osu.menu.ScoreBoardItem
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2


// Statistics

/**
 * Specifically made to handle `liveScoreData` event.
 */
fun jsonToScoreboardItem(json: JSONObject) = ScoreBoardItem().apply {

    userName = json.optString("username", "")
    playScore = json.optInt("score", 0)
    maxCombo = json.optInt("combo", 0)
    accuracy = json.optDouble("accuracy", 0.0).toFloat()
    isAlive = json.optBoolean("isAlive", true)
}

/**
 * Specifically made to handle `scoreSubmission` event.
 */
fun jsonToStatistic(json: JSONObject) = StatisticV2().apply {

    uid = json.optLong("uid", -1L)
    playerName = json.optString("username", "")
    setForcedScore(json.optInt("score", 0))
    time = System.currentTimeMillis()
    mod = deserializeMods(json.optJSONArray("mods")?.toString() ?: "")
    scoreMaxCombo = json.optInt("maxCombo")
    hit300k = json.optInt("geki")
    hit300 = json.optInt("perfect")
    hit100k = json.optInt("katu")
    hit100 = json.optInt("good")
    hit50 = json.optInt("bad")
    misses = json.optInt("miss")
    isAlive = json.optBoolean("isAlive", true)
}
