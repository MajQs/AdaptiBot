package com.adaptibot.serialization

import com.adaptibot.TestUtils
import com.adaptibot.serialization.json.ScriptSerializer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class ScriptSerializerTest {
    
    @Test
    fun `serialize and deserialize script maintains data`() {
        val originalScript = TestUtils.createTestScript()
        
        val json = ScriptSerializer.serialize(originalScript)
        val deserializedScript = ScriptSerializer.deserialize(json)
        
        assertEquals(originalScript.name, deserializedScript.name)
        assertEquals(originalScript.description, deserializedScript.description)
        assertEquals(originalScript.steps.size, deserializedScript.steps.size)
        assertEquals(originalScript.settings, deserializedScript.settings)
    }
    
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
    fun `serialize conditional script works`() {
        val script = TestUtils.createConditionalScript()
        
        val json = ScriptSerializer.serialize(script)
        val deserializedScript = ScriptSerializer.deserialize(json)
        
        assertEquals(script.name, deserializedScript.name)
        assertTrue(deserializedScript.steps.isNotEmpty())
    }
}

