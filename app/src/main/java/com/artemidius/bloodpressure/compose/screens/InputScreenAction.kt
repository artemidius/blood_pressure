package com.artemidius.bloodpressure.compose.screens

sealed interface InputScreenAction {
    object LaunchSuccessScreen : InputScreenAction
    object LaunchLoginScreen : InputScreenAction
    object LaunchListScreen : InputScreenAction
    object LaunchGraphScreen : InputScreenAction
    object LaunchCamera : InputScreenAction
}