package com.offlinejournal.presentation.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object SelectCategory : Screen("select_category")
    data object NewNote : Screen("new_note/{categoryId}") {
        fun createRoute(categoryId: Long) = "new_note/$categoryId"
    }
    data object NoteDetail : Screen("note_detail/{noteId}") {
        fun createRoute(noteId: Long) = "note_detail/$noteId"
    }
    data object Categories : Screen("categories")
    data object Reminders : Screen("reminders")
    data object NewReminder : Screen("new_reminder")
    data object EditReminder : Screen("edit_reminder/{reminderId}") {
        fun createRoute(reminderId: Long) = "edit_reminder/$reminderId"
    }
    data object NotesList : Screen("notes_list")
    data object Search : Screen("search")
}

enum class HomeTab {
    CATEGORIES,
    NOTES,
    REMINDERS
}
