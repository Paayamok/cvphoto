package com.fushengce.home

enum class CharacterMotion(
    val marker: String,
    val loops: Boolean,
) {
    Idle(marker = "idle", loops = true),
    Listening(marker = "listening", loops = true),
    Thinking(marker = "thinking", loops = true),
    Found(marker = "found", loops = false),
    Remind(marker = "remind", loops = false),
    Complete(marker = "complete", loops = false),
    NotFound(marker = "not_found", loops = false),
    Error(marker = "error", loops = false),
    RealmSwitch(marker = "realm_switch", loops = false),
}

sealed interface HomeFeedback {
    data class RealmSwitch(val destination: MainRealm) : HomeFeedback
    data object Listening : HomeFeedback
    data object Found : HomeFeedback
    data object Remind : HomeFeedback
    data object Complete : HomeFeedback
}

internal fun HomeFeedback.toCharacterMotion(): CharacterMotion = when (this) {
    is HomeFeedback.RealmSwitch -> CharacterMotion.RealmSwitch
    HomeFeedback.Listening -> CharacterMotion.Listening
    HomeFeedback.Found -> CharacterMotion.Found
    HomeFeedback.Remind -> CharacterMotion.Remind
    HomeFeedback.Complete -> CharacterMotion.Complete
}

data class CharacterTransform(
    val scale: Float,
    val translationYDp: Float,
    val rotationDegrees: Float,
)

fun CharacterMotion.targetTransform(): CharacterTransform = when (this) {
    CharacterMotion.Idle -> CharacterTransform(1f, 0f, 0f)
    CharacterMotion.Listening -> CharacterTransform(1.025f, -3f, 0f)
    CharacterMotion.Thinking -> CharacterTransform(0.99f, 1f, -0.6f)
    CharacterMotion.Found -> CharacterTransform(1.04f, -6f, 0f)
    CharacterMotion.Remind -> CharacterTransform(1.015f, -2f, 0.7f)
    CharacterMotion.Complete -> CharacterTransform(1.045f, -5f, 0f)
    CharacterMotion.NotFound -> CharacterTransform(0.975f, 2f, -0.8f)
    CharacterMotion.Error -> CharacterTransform(0.98f, 1f, 0.9f)
    CharacterMotion.RealmSwitch -> CharacterTransform(0.94f, 0f, 0f)
}
