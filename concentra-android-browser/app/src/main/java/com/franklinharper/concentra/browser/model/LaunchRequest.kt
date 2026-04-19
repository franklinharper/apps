package com.franklinharper.concentra.browser.model

sealed interface LaunchRequest {
    data object Empty : LaunchRequest

    data class OpenUrl(val url: String) : LaunchRequest
}
