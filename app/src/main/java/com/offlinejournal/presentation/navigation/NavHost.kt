package com.offlinejournal.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.offlinejournal.data.local.AppContainer
import com.offlinejournal.presentation.screens.CategoriesScreen
import com.offlinejournal.presentation.screens.HomeScreen
import com.offlinejournal.presentation.screens.NoteDetailScreen
import com.offlinejournal.presentation.screens.NoteEditorScreen
import com.offlinejournal.presentation.screens.ReminderEditorScreen
import com.offlinejournal.presentation.screens.RemindersScreen
import com.offlinejournal.presentation.screens.ReportsScreen
import com.offlinejournal.presentation.screens.SearchScreen

@Composable
fun OfflineJournalNavHost(container: AppContainer) {
    val navController = rememberNavController()

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    container = container,
                    onNewNote = { navController.navigate(Screen.NewNote.route) },
                    onNoteClick = { id -> navController.navigate(Screen.NoteDetail.createRoute(id)) },
                    onCategories = { navController.navigate(Screen.Categories.route) },
                    onReminders = { navController.navigate(Screen.Reminders.route) },
                    onReports = { navController.navigate(Screen.Reports.route) },
                    onSearch = { navController.navigate(Screen.Search.route) }
                )
            }

            composable(Screen.NewNote.route) {
                NoteEditorScreen(
                    container = container,
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.NoteDetail.route,
                arguments = listOf(navArgument("noteId") { type = NavType.LongType })
            ) { backStackEntry ->
                val noteId = backStackEntry.arguments?.getLong("noteId") ?: return@composable
                NoteDetailScreen(
                    container = container,
                    noteId = noteId,
                    onBack = { navController.popBackStack() },
                    onEdit = {
                        navController.navigate("edit_note/$noteId")
                    },
                    onDeleted = { navController.popBackStack() }
                )
            }

            composable(
                route = "edit_note/{noteId}",
                arguments = listOf(navArgument("noteId") { type = NavType.LongType })
            ) { backStackEntry ->
                val noteId = backStackEntry.arguments?.getLong("noteId") ?: return@composable
                NoteEditorScreen(
                    container = container,
                    noteId = noteId,
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() }
                )
            }

            composable(Screen.Categories.route) {
                CategoriesScreen(
                    container = container,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Reminders.route) {
                RemindersScreen(
                    container = container,
                    onBack = { navController.popBackStack() },
                    onNewReminder = { navController.navigate(Screen.NewReminder.route) },
                    onEditReminder = { id -> navController.navigate(Screen.EditReminder.createRoute(id)) }
                )
            }

            composable(Screen.NewReminder.route) {
                ReminderEditorScreen(
                    container = container,
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.EditReminder.route,
                arguments = listOf(navArgument("reminderId") { type = NavType.LongType })
            ) { backStackEntry ->
                val reminderId = backStackEntry.arguments?.getLong("reminderId") ?: return@composable
                ReminderEditorScreen(
                    container = container,
                    reminderId = reminderId,
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() }
                )
            }

            composable(Screen.Reports.route) {
                ReportsScreen(
                    container = container,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Search.route) {
                SearchScreen(
                    container = container,
                    onBack = { navController.popBackStack() },
                    onNoteClick = { id -> navController.navigate(Screen.NoteDetail.createRoute(id)) }
                )
            }
        }
    }
}
