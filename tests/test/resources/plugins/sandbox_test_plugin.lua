name = "Sandbox Test"
version = "1.0.0"
author = "TestSuite"
description = "Tests that dangerous APIs are properly blocked"

local results = {}

function onLoad()
    -- Test: io library should be nil (blocked)
    results.io_blocked = (io == nil)

    -- Test: os library should be nil (blocked)
    results.os_blocked = (os == nil)

    -- Test: debug library should be nil (blocked)
    results.debug_blocked = (debug == nil)

    -- Test: require should be nil (blocked)
    results.require_blocked = (require == nil)

    -- Test: loadlib should be nil (blocked)
    results.loadlib_blocked = (loadlib == nil)

    -- Test: collectgarbage should be nil (blocked)
    results.collectgarbage_blocked = (collectgarbage == nil)

    -- Test: rawget should be nil (blocked)
    results.rawget_blocked = (rawget == nil)

    -- Test: rawset should be nil (blocked)
    results.rawset_blocked = (rawset == nil)

    -- Test: setmetatable should be nil (blocked)
    results.setmetatable_blocked = (setmetatable == nil)

    -- Test: getmetatable should be nil (blocked)
    results.getmetatable_blocked = (getmetatable == nil)

    -- Test: Safe APIs should work
    results.math_exists = (math ~= nil)
    results.string_exists = (string ~= nil)
    results.table_exists = (table ~= nil)
    results.log_exists = (log ~= nil)
    results.getGameTime_exists = (getGameTime ~= nil)
    results.createText_exists = (createText ~= nil)
    results.emit_exists = (emit ~= nil)
    results.on_exists = (on ~= nil)

    -- Test: File I/O works in sandbox
    results.fileWrite_exists = (fileWrite ~= nil)
    results.fileRead_exists = (fileRead ~= nil)
end

function getResults()
    return results
end
