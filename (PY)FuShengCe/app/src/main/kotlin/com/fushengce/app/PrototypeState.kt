package com.fushengce.app

enum class PrototypePage { Home, Gallery, Immersive }

enum class IntroStage { World, LifeDuty, Permission, PhotoBloom, Crossing, WorkDuty }

enum class PrototypePresence { Browsing, Working, Immersive }

enum class PrototypeRealm {
    Life,
    Affairs;

    fun opposite(): PrototypeRealm = if (this == Life) Affairs else Life
}

data class PrototypeUiState(
    val introStage: IntroStage? = IntroStage.World,
    val page: PrototypePage = PrototypePage.Home,
    val realm: PrototypeRealm = PrototypeRealm.Life,
    val presence: PrototypePresence = PrototypePresence.Browsing,
    val voiceOpen: Boolean = false,
    val selectedPhotoIndex: Int = 0,
) {
    val introVisible: Boolean get() = introStage != null
}

sealed interface PrototypeAction {
    data object AdvanceIntro : PrototypeAction
    data object ContinueWithSamples : PrototypeAction
    data object FinishIntro : PrototypeAction
    data object RevisitIntro : PrototypeAction
    data object SwitchRealm : PrototypeAction
    data object OpenVoice : PrototypeAction
    data object CloseVoice : PrototypeAction
    data object OpenGallery : PrototypeAction
    data object BeginWork : PrototypeAction
    data object FinishWork : PrototypeAction
    data class OpenPhoto(val index: Int) : PrototypeAction
    data object Back : PrototypeAction
}

fun PrototypeUiState.reduce(action: PrototypeAction): PrototypeUiState = when (action) {
    PrototypeAction.AdvanceIntro -> copy(
        introStage = when (introStage) {
            IntroStage.World -> IntroStage.LifeDuty
            IntroStage.LifeDuty -> IntroStage.Permission
            IntroStage.Permission -> IntroStage.PhotoBloom
            IntroStage.PhotoBloom -> IntroStage.Crossing
            IntroStage.Crossing -> IntroStage.WorkDuty
            IntroStage.WorkDuty, null -> null
        },
    )
    PrototypeAction.ContinueWithSamples -> copy(introStage = IntroStage.PhotoBloom)
    PrototypeAction.FinishIntro -> copy(
        introStage = null,
        page = PrototypePage.Home,
        realm = PrototypeRealm.Life,
        presence = PrototypePresence.Browsing,
    )
    PrototypeAction.RevisitIntro -> PrototypeUiState()
    PrototypeAction.SwitchRealm -> copy(realm = realm.opposite(), voiceOpen = false)
    PrototypeAction.OpenVoice -> copy(voiceOpen = true)
    PrototypeAction.CloseVoice -> copy(voiceOpen = false)
    PrototypeAction.OpenGallery -> copy(
        page = PrototypePage.Gallery,
        presence = PrototypePresence.Browsing,
        voiceOpen = false,
    )
    PrototypeAction.BeginWork -> copy(presence = PrototypePresence.Working, voiceOpen = false)
    PrototypeAction.FinishWork -> copy(presence = PrototypePresence.Browsing)
    is PrototypeAction.OpenPhoto -> copy(
        page = PrototypePage.Immersive,
        presence = PrototypePresence.Immersive,
        selectedPhotoIndex = action.index.coerceAtLeast(0),
        voiceOpen = false,
    )
    PrototypeAction.Back -> when (page) {
        PrototypePage.Immersive -> copy(page = PrototypePage.Gallery, presence = PrototypePresence.Browsing)
        PrototypePage.Gallery -> copy(page = PrototypePage.Home, presence = PrototypePresence.Browsing)
        PrototypePage.Home -> this
    }
}

fun initialPrototypeState(introCompleted: Boolean): PrototypeUiState = if (introCompleted) {
    PrototypeUiState(introStage = null)
} else {
    PrototypeUiState()
}
