name = "Counter Plugin"
version = "1.0.0"
author = "TestSuite"
description = "A test plugin that counts hits and tracks game state"

local hitCount = 0
local lastScore = 0
local totalScore = 0
local loaded = false

function onLoad()
    loaded = true
    log("Counter Plugin loaded!")
end

function onUnload()
    log("Counter Plugin unloaded!")
    loaded = false
end

function onCircleHit(objectId, accuracy, x, y, endCombo, scoreValue)
    hitCount = hitCount + 1
    lastScore = scoreValue
    totalScore = totalScore + scoreValue
end

function onSliderHit(objectId, scoreType, x, y, endCombo)
    hitCount = hitCount + 1
    lastScore = scoreType
    totalScore = totalScore + scoreType
end

function onSliderEnd(objectId, accuracy)
    -- no-op
end

function onSpinnerStart(objectId)
    -- no-op
end

function onSpinnerHit(objectId, scoreValue)
    hitCount = hitCount + 1
    lastScore = scoreValue
    totalScore = totalScore + scoreValue
end

function onSpinnerEnd(objectId)
    -- no-op
end

function onBeatmapLoaded(name, artist, difficulty, objectCount)
    log("Beatmap: " .. name .. " by " .. artist .. " [" .. difficulty .. "] (" .. objectCount .. " objects)")
end

function onBeatmapFinished(totalScore, maxCombo, accuracy, grade)
    log("Finished! Score=" .. totalScore .. " Combo=" .. maxCombo .. " Acc=" .. accuracy .. " Grade=" .. grade)
end
