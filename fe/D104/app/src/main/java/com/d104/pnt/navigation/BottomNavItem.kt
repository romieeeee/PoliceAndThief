package com.d104.pnt.navigation

import com.d104.pnt.R


sealed class BottomNavItem(
    val route: String,
    val icon: Int,
    val label: String
) {
    data object Chat : BottomNavItem("chat", R.drawable.ic_chat, "chat")
    data object Home : BottomNavItem("home", R.drawable.ic_logo, "home")
    data object MyPage : BottomNavItem("com/d104/pnt/ui/mypage", R.drawable.ic_user, "com/d104/pnt/ui/mypage")

    companion object {
        val items = listOf(Chat, Home, MyPage)
    }
}