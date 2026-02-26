package com.adaptibot.action.domain

internal interface ActionHandler<Action> {
    fun handle(action: Action)
}