package com.adaptibot.ui

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.stage.Stage
import org.slf4j.LoggerFactory

class AdaptiBotApp : Application() {
    
    private val logger = LoggerFactory.getLogger(AdaptiBotApp::class.java)
    
    override fun start(primaryStage: Stage) {
        logger.info("Starting AdaptiBot application")
        
        try {
            val loader = FXMLLoader(javaClass.getResource("/fxml/main.fxml"))
            val root = loader.load<Any>()
            val scene = Scene(root as javafx.scene.Parent, 900.0, 650.0)

            primaryStage.apply {
                title = "AdaptiBot"
                this.scene = scene
                minWidth = 700.0
                minHeight = 500.0
                show()
            }
            
            logger.info("Application started successfully")
            
        } catch (e: Exception) {
            logger.error("Failed to start application", e)
            throw e
        }
    }
    
    override fun stop() {
        logger.info("Shutting down AdaptiBot")
        super.stop()
    }
}

