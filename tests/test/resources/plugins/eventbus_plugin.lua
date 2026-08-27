name = "EventBus Plugin"
version = "1.0.0"
author = "TestSuite"
description = "Tests inter-plugin event bus communication"

local received = {}
local messageCount = 0

function onLoad()
    on("test_event", function(data)
        messageCount = messageCount + 1
        table.insert(received, data)
        log("Received test_event: " .. tostring(data))
    end)
end

function onUnload()
    off("test_event")
end

function getMessageCount()
    return messageCount
end

function getReceived()
    return received
end

function sendMessage(data)
    emit("test_event", data)
end
