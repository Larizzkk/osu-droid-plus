@file:JvmName("MultiplayerConverter")

package com.osudroid.multiplayer

import com.rian.osu.utils.ModUtils
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

    playerName = json.optString("username", "")
    setForcedScore(json.optInt("score", 0))
    time = System.currentTimeMillis()
    mod = ModUtils.deserializeMods(json.optJSONArray("mods")?.toString() ?: "")
    scoreMaxCombo = json.optInt("maxCombo", 0)
    hit300k = json.optInt("geki", 0)
    hit300 = json.optInt("perfect", 0)
    hit100k = json.optInt("katu", 0)
    hit100 = json.optInt("good", 0)
    hit50 = json.optInt("bad", 0)
    misses = json.optInt("miss", 0)
    isAlive = json.optBoolean("isAlive", true)
}
