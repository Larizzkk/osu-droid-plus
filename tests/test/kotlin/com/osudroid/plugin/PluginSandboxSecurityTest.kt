package com.osudroid.plugin

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.luaj.vm2.LuaValue
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class PluginSandboxSecurityTest {

    private val context: Context
        get() = RuntimeEnvironment.getApplication()

    @Test
    fun `removes the Java bridge and file loaders`() {
        val globals = LuaPluginSandbox.createSandbox(context, "security-test")

        for (name in listOf(
            "luajava", "java", "javax", "io", "os", "debug", "package",
            "require", "load", "loadstring", "dofile", "loadfile"
        )) {
            assertTrue("$name must be blocked", globals.get(name).isnil())
        }
        assertTrue(globals.get("log").isfunction())
    }

    @Test
    fun `plugin settings are isolated and persisted`() {
        val first = LuaPluginSandbox.createSandbox(context, "settings-a")
        val second = LuaPluginSandbox.createSandbox(context, "settings-b")

        first.get("setPluginSetting").invoke(
            LuaValue.valueOf("volume"),
            LuaValue.valueOf(35)
        )
        first.get("setPluginSetting").invoke(
            LuaValue.valueOf("mode"),
            LuaValue.valueOf("compact")
        )

        val readFirst = first.get("getPluginSetting").call(LuaValue.valueOf("volume"))
        val readSecond = second.get("getPluginSetting").call(LuaValue.valueOf("volume"))
        assertEquals(35, readFirst.toint())
        assertTrue(readSecond.isnil())

        second.get("setPluginSetting").invoke(
            LuaValue.valueOf("volume"),
            LuaValue.valueOf(70)
        )
        assertEquals(
            35,
            first.get("getPluginSetting").call(LuaValue.valueOf("volume")).toint()
        )
        assertEquals(
            70,
            second.get("getPluginSetting").call(LuaValue.valueOf("volume")).toint()
        )
    }

    @Test
    fun `file io cannot escape the plugin data directory`() {
        assertFalse(
            PluginFileIO.writeFile(context, "escape", "../outside.txt", "nope")
        )
        assertFalse(
            PluginFileIO.writeFile(context, "escape", "/data/local/tmp/outside.txt", "nope")
        )
        assertFalse(PluginFileIO.fileExists(context, "escape", "../outside.txt"))

        assertTrue(PluginFileIO.writeFile(context, "escape", "inside.txt", "hello"))
        assertEquals("hello", PluginFileIO.readFile(context, "escape", "inside.txt"))

        val escaped = File(context.filesDir, "plugins/escape/outside.txt")
        assertFalse(escaped.exists())
    }

    @Test
    fun `http policy only allows public HTTPS hosts`() {
        assertFalse(PluginUrlPolicy.isAllowed("http://example.com/"))
        assertFalse(PluginUrlPolicy.isAllowed("file:///sdcard/secret"))
        assertFalse(PluginUrlPolicy.isAllowed("https://127.0.0.1/"))
        assertFalse(PluginUrlPolicy.isAllowed("https://10.0.0.1/"))
        assertFalse(PluginUrlPolicy.isAllowed("https://192.168.1.1/"))
        assertFalse(PluginUrlPolicy.isAllowed("https://169.254.169.254/"))
        assertFalse(PluginUrlPolicy.isAllowed("https://[::1]/"))
        assertFalse(PluginUrlPolicy.isAllowed("https://[fc00::1]/"))
        // IPv4-mapped IPv6 still routes to the embedded private address.
        assertFalse(PluginUrlPolicy.isAllowed("https://[::ffff:127.0.0.1]/"))
        assertFalse(PluginUrlPolicy.isAllowed("https://[::ffff:10.0.0.1]/"))
        assertFalse(PluginUrlPolicy.isAllowed("https://[::ffff:169.254.169.254]/"))
        assertFalse(PluginUrlPolicy.isAllowed("https://localhost/"))

        assertTrue(PluginUrlPolicy.isAllowed("https://8.8.8.8/"))
        assertTrue(PluginUrlPolicy.isAllowed("https://1.1.1.1/api"))
    }
}
