name = "Visual Sandbox"
version = "1.0.0"
author = "TestSuite"
description = "Simulates visual plugin behavior for headless testing"

local frameCount = 0
local cursorPositions = {}
local gameUpdates = {}
local pauseCount = 0
local resumeCount = 0
local beatmapInfo = nil
local finishInfo = nil

function onLoad()
    log("Visual Sandbox plugin loaded")
    onPause(function()
        pauseCount = pauseCount + 1
    end)
    onResume(function()
        resumeCount = resumeCount + 1
    end)
end

function onGameUpdate(dt, gameTime)
    frameCount = frameCount + 1
    table.insert(gameUpdates, {dt = dt, time = gameTime})
end

function onCursorUpdate(x, y, timestamp)
    table.insert(cursorPositions, {x = x, y = y, t = timestamp})
end

function onBeatmapLoaded(name, artist, difficulty, objectCount)
    beatmapInfo = {
        name = name,
        artist = artist,
        difficulty = difficulty,
        objectCount = objectCount
    }
end

function onBeatmapFinished(totalScore, maxCombo, accuracy, grade)
    finishInfo = {
        score = totalScore,
        combo = maxCombo,
        accuracy = accuracy,
        grade = grade
    }
end

function onCircleHit(objectId, accuracy, x, y, endCombo, scoreValue)
    -- record hit for verification
end

function getFrameCount()
    return frameCount
end

function getPauseCount()
    return pauseCount
end

function getResumeCount()
    return resumeCount
end

function getBeatmapInfo()
    return beatmapInfo
end

function getFinishInfo()
    return finishInfo
end
