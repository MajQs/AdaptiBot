package com.adaptibot.ui.controller

import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.Label
import java.net.URL
import java.util.*

class MainController : Initializable {
    
    @FXML
    private lateinit var welcomeLabel: Label
    
    override fun initialize(location: URL?, resources: ResourceBundle?) {
        welcomeLabel.text = "Welcome to AdaptiBot!"
    }
}

