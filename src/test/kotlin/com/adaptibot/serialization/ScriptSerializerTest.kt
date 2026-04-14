package com.adaptibot.serialization

import com.adaptibot.TestUtils
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class ScriptSerializerTest {

    @Test
    fun `save and load script to file works correctly`(@TempDir tempDir: Path) {
        val originalScript = TestUtils.createTestScript("File Test Script")
        val filePath = tempDir.resolve("test_script.json")

        ScriptSerializer.saveToFile(originalScript, filePath)
        val loadedScript = ScriptSerializer.loadFromFile(filePath)

        assertEquals(originalScript.name, loadedScript.name)
        assertEquals(originalScript.steps.size, loadedScript.steps.size)
    }

    @Test
    fun `step IDs are preserved after serialization round-trip`(@TempDir tempDir: Path) {
        val originalScript = TestUtils.createTestScript("ID Round-trip Test")
        val filePath = tempDir.resolve("id_test.json")

        ScriptSerializer.saveToFile(originalScript, filePath)
        val loadedScript = ScriptSerializer.loadFromFile(filePath)

        val originalIds = originalScript.steps.map { it.id.value }
        val loadedIds   = loadedScript.steps.map { it.id.value }
        assertEquals(originalIds, loadedIds)
    }

}

