package com.adaptibot.action.domain

interface ActionHandler<Action> {
    fun handle(action: Action)
}