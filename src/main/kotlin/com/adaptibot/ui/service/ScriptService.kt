package com.adaptibot.ui.service

import com.adaptibot.common.model.Script
import com.adaptibot.common.model.ScriptSettings
import com.adaptibot.serialization.json.ScriptSerializer
import javafx.stage.FileChooser
import javafx.stage.Stage
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Paths

class ScriptService {
    
    private val logger = LoggerFactory.getLogger(ScriptService::class.java)
    
    private var currentScript: Script = createEmptyScript()
    private var currentFile: File? = null
    private var hasUnsavedChanges = false
    
    fun createNewScript() {
        currentScript = createEmptyScript()
        currentFile = null
        hasUnsavedChanges = false
        logger.info("Created new script")
    }
    
    fun openScript(stage: Stage? = null): Boolean {
        val fileChooser = FileChooser().apply {
            title = "Open Script"
            extensionFilters.add(
                FileChooser.ExtensionFilter("AdaptiBot Scripts", "*.json")
            )
        }
        
        val file = fileChooser.showOpenDialog(stage) ?: return false
        
        return try {
            val path = Paths.get(file.absolutePath)
            currentScript = ScriptSerializer.loadFromFile(path)
            currentFile = file
            hasUnsavedChanges = false
            logger.info("Opened script: ${file.name}")
            true
        } catch (e: Exception) {
            logger.error("Failed to open script", e)
            false
        }
    }
    
    fun saveScript(stage: Stage? = null): Boolean {
        return if (currentFile != null) {
            saveToFile(currentFile!!)
        } else {
            saveScriptAs(stage)
        }
    }
    
    fun saveScriptAs(stage: Stage? = null): Boolean {
        val fileChooser = FileChooser().apply {
            title = "Save Script As"
            initialFileName = "${currentScript.name}.json"
            extensionFilters.add(
                FileChooser.ExtensionFilter("AdaptiBot Scripts", "*.json")
            )
        }
        
        val file = fileChooser.showSaveDialog(stage) ?: return false
        
        return saveToFile(file)
    }
    
    fun getCurrentScript(): Script = currentScript
    
    fun updateScript(script: Script) {
        currentScript = script
        hasUnsavedChanges = true
    }
    
    fun hasUnsavedChanges(): Boolean = hasUnsavedChanges
    
    private fun saveToFile(file: File): Boolean {
        return try {
            val path = Paths.get(file.absolutePath)
            ScriptSerializer.saveToFile(currentScript, path)
            currentFile = file
            hasUnsavedChanges = false
            logger.info("Saved script: ${file.name}")
            true
        } catch (e: Exception) {
            logger.error("Failed to save script", e)
            false
        }
    }
    
    private fun createEmptyScript(): Script {
        return Script(
            name = "New Script",
            description = "",
            steps = emptyList(),
            settings = ScriptSettings()
        )
    }
}

