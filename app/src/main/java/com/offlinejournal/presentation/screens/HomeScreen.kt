package com.offlinejournal.presentation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.offlinejournal.R
import com.offlinejournal.data.local.AppContainer
import com.offlinejournal.domain.model.Category
import com.offlinejournal.domain.model.Note
import com.offlinejournal.domain.model.Reminder
import com.offlinejournal.presentation.components.EmptyStateMessage
import com.offlinejournal.presentation.navigation.HomeTab
import com.offlinejournal.presentation.theme.PurpleDark
import com.offlinejournal.presentation.theme.PurplePrimary
import com.offlinejournal.presentation.viewmodel.CategoriesViewModel
import com.offlinejournal.presentation.viewmodel.HomeViewModel
import com.offlinejournal.presentation.viewmodel.RemindersViewModel

@Composable
fun HomeScreen(
    container: AppContainer,
    onNewNote: () -> Unit,
    onNewReminder: () -> Unit,
    onNoteClick: (Long) -> Unit,
    onEditReminder: (Long) -> Unit,
    homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory(container)),
    categoriesViewModel: CategoriesViewModel = viewModel(factory = CategoriesViewModel.Factory(container)),
    remindersViewModel: RemindersViewModel = viewModel(factory = RemindersViewModel.Factory(container))
) {
    var selectedTab by rememberSaveable { mutableStateOf(HomeTab.NOTES) }
    val homeState by homeViewModel.uiState.collectAsState()
    val categories by categoriesViewModel.categories.collectAsState()
    val upcomingReminders by remindersViewModel.upcomingReminders.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                NavigationBarItem(
                    selected = selectedTab == HomeTab.CATEGORIES,
                    onClick = { selectedTab = HomeTab.CATEGORIES },
                    icon = { Icon(Icons.Default.Category, contentDescription = null) },
                    label = { Text("دسته‌بندی‌ها") },
                    colors = navItemColors()
                )
                NavigationBarItem(
                    selected = selectedTab == HomeTab.NOTES,
                    onClick = { selectedTab = HomeTab.NOTES },
                    icon = { Icon(Icons.Default.Notes, contentDescription = null) },
                    label = { Text("یادداشت‌ها") },
                    colors = navItemColors()
                )
                NavigationBarItem(
                    selected = selectedTab == HomeTab.REMINDERS,
                    onClick = { selectedTab = HomeTab.REMINDERS },
                    icon = { Icon(Icons.Default.Notifications, contentDescription = null) },
                    label = { Text("یادآوری‌ها") },
                    colors = navItemColors()
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            Image(
                painter = painterResource(R.drawable.jayminder_logo),
                contentDescription = "JayMinder",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .padding(horizontal = 32.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(24.dp))

            ActionPlusButton(
                label = "یادداشت جدید",
                large = true,
                onClick = onNewNote
            )

            Spacer(modifier = Modifier.height(12.dp))

            ActionPlusButton(
                label = "یادآوری جدید",
                large = false,
                onClick = onNewReminder
            )

            Spacer(modifier = Modifier.height(20.dp))

            when (selectedTab) {
                HomeTab.CATEGORIES -> CategoriesTabContent(
                    categories = categories,
                    onDelete = categoriesViewModel::deleteCategory,
                    viewModel = categoriesViewModel
                )
                HomeTab.NOTES -> NotesTabContent(
                    notes = homeState.notes,
                    categories = categories,
                    formatDate = homeViewModel::formatNoteDate,
                    onNoteClick = onNoteClick,
                    onDelete = homeViewModel::deleteNote
                )
                HomeTab.REMINDERS -> RemindersTabContent(
                    reminders = upcomingReminders,
                    formatTime = remindersViewModel::formatReminderTime,
                    onEdit = onEditReminder,
                    onDelete = remindersViewModel::deleteReminder,
                    onComplete = remindersViewModel::markCompleted
                )
            }
        }
    }
}

@Composable
private fun navItemColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = PurplePrimary,
    selectedTextColor = PurplePrimary,
    indicatorColor = PurpleDark.copy(alpha = 0.35f),
    unselectedIconColor = Color(0xFF94A3B8),
    unselectedTextColor = Color(0xFF94A3B8)
)

@Composable
private fun ActionPlusButton(label: String, large: Boolean, onClick: () -> Unit) {
    val height = if (large) 72.dp else 56.dp
    val iconSize = if (large) 32.dp else 24.dp
    val fontSize = if (large) 18.sp else 15.sp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(if (large) 48.dp else 40.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(PurplePrimary, PurpleDark)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(iconSize)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun CategoriesTabContent(
    categories: List<Category>,
    onDelete: (Category) -> Unit,
    viewModel: CategoriesViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = uiState.newCategoryName,
            onValueChange = viewModel::updateNewCategoryName,
            label = { Text("دسته‌بندی جدید") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Button(
            onClick = viewModel::saveCategory,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("افزودن دسته‌بندی")
        }
        uiState.errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        if (categories.isEmpty()) {
            EmptyStateMessage("هنوز دسته‌بندی ایجاد نشده است.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories, key = { it.id }) { category ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(category.name, style = MaterialTheme.typography.titleMedium)
                            IconButton(onClick = { onDelete(category) }) {
                                Icon(Icons.Default.Delete, contentDescription = "حذف")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotesTabContent(
    notes: List<Note>,
    categories: List<Category>,
    formatDate: (Note) -> String,
    onNoteClick: (Long) -> Unit,
    onDelete: (Note) -> Unit
) {
    if (notes.isEmpty()) {
        EmptyStateMessage("هنوز یادداشتی ندارید.")
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(notes, key = { it.id }) { note ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNoteClick(note.id) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = note.title ?: "بدون عنوان",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = note.displayText,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = formatDate(note),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (note.audioFilePath != null) {
                                    Icon(Icons.Default.Mic, contentDescription = null, tint = PurplePrimary, modifier = Modifier.size(16.dp))
                                }
                                categories.find { it.id == note.categoryId }?.name?.let {
                                    Text(" • $it", style = MaterialTheme.typography.bodySmall, color = PurpleLight)
                                }
                                IconButton(onClick = { onDelete(note) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف", modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RemindersTabContent(
    reminders: List<Reminder>,
    formatTime: (Reminder) -> String,
    onEdit: (Long) -> Unit,
    onDelete: (Reminder) -> Unit,
    onComplete: (Reminder) -> Unit
) {
    if (reminders.isEmpty()) {
        EmptyStateMessage("یادآوری آینده‌ای ندارید.")
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(reminders, key = { it.id }) { reminder ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEdit(reminder.id) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(reminder.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = formatTime(reminder),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            Text(
                                text = "انجام شد",
                                color = PurplePrimary,
                                modifier = Modifier
                                    .clickable { onComplete(reminder) }
                                    .padding(8.dp)
                            )
                            IconButton(onClick = { onDelete(reminder) }) {
                                Icon(Icons.Default.Delete, contentDescription = "حذف")
                            }
                        }
                    }
                }
            }
        }
    }
}

private val PurpleLight = Color(0xFFA78BFA)
