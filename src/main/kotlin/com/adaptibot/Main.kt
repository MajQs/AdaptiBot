package com.adaptibot

import com.adaptibot.infrastructure.AppPaths
import com.adaptibot.ui.AdaptiBotApp
import javafx.application.Application

fun main(args: Array<String>) {
    System.setProperty("adaptibot.logDir", AppPaths.logDir().toString())
    Application.launch(AdaptiBotApp::class.java, *args)
}

