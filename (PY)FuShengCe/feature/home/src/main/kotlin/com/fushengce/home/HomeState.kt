package com.fushengce.home

enum class MainRealm {
    Life,
    Affairs;

    fun opposite(): MainRealm = if (this == Life) Affairs else Life
}

enum class HomeBottomItem(
    val label: String,
    val mark: String,
) {
    Gallery(label = "归册", mark = "册"),
    Voice(label = "一语相托", mark = "语"),
    Profile(label = "我的", mark = "我"),
}

sealed interface HomeAction {
    data class SelectMainRealm(val realm: MainRealm) : HomeAction

    data class ShowMessage(
        val message: String,
        val feedback: HomeFeedback = HomeFeedback.Remind,
    ) : HomeAction

    data object OpenGallery : HomeAction
}

data class HomeState(
    val mainRealm: MainRealm = MainRealm.Life,
) {
    val lifeWeight: Float
        get() = if (mainRealm == MainRealm.Life) MAIN_REALM_WEIGHT else SIDE_REALM_WEIGHT

    val affairsWeight: Float
        get() = if (mainRealm == MainRealm.Affairs) MAIN_REALM_WEIGHT else SIDE_REALM_WEIGHT

    fun onRealmPressed(realm: MainRealm): HomeAction = if (realm == mainRealm) {
        HomeAction.ShowMessage(REALM_NOT_OPEN_MESSAGE)
    } else {
        HomeAction.SelectMainRealm(realm)
    }

    fun onBottomItemPressed(item: HomeBottomItem): HomeAction = when (item) {
        HomeBottomItem.Gallery -> HomeAction.OpenGallery
        HomeBottomItem.Voice -> HomeAction.ShowMessage(
            message = SCROLL_NOT_OPEN_MESSAGE,
            feedback = HomeFeedback.Listening,
        )

        HomeBottomItem.Profile -> HomeAction.ShowMessage(SCROLL_NOT_OPEN_MESSAGE)
    }

    companion object {
        const val MAIN_REALM_WEIGHT = 7f
        const val SIDE_REALM_WEIGHT = 3f
        const val REALM_NOT_OPEN_MESSAGE = "此境尚待启封"
        const val SCROLL_NOT_OPEN_MESSAGE = "此卷尚待启封"
    }
}
