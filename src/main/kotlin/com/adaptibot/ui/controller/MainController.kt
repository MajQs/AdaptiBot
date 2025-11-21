package com.adaptibot.ui.controller

import com.adaptibot.ui.view.MainView
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.layout.BorderPane
import java.net.URL
import java.util.*

class MainController : Initializable {
    
    @FXML
    private lateinit var rootPane: BorderPane
    
    private lateinit var mainView: MainView
    
    override fun initialize(location: URL?, resources: ResourceBundle?) {
        mainView = MainView()
        rootPane.center = mainView
    }
}

