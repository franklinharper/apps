package com.franklinharper.wordlecoach.domain

data class CoachingState(
    val steps: List<CoachingStep>,
    val currentStepIndex: Int
) {
    val currentStep: CoachingStep get() = steps[currentStepIndex]
    val canGoBack: Boolean get() = currentStepIndex > 0
    val canGoForward: Boolean get() = currentStepIndex < steps.size - 1

    fun goBack(): CoachingState =
        if (canGoBack) copy(currentStepIndex = currentStepIndex - 1) else this

    fun goForward(): CoachingState =
        if (canGoForward) copy(currentStepIndex = currentStepIndex + 1) else this

    companion object {
        fun initial() = CoachingState(
            steps = listOf(CoachingStep.BeforeFirstGuess),
            currentStepIndex = 0
        )
    }
}
