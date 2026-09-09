package com.offlinejournal.presentation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.offlinejournal.presentation.components.CategorySelectModal
import com.offlinejournal.presentation.components.EmptyStateMessage
import com.offlinejournal.presentation.components.JayMinderBottomBar
import com.offlinejournal.presentation.components.MountainBackground
import com.offlinejournal.presentation.navigation.HomeTab
import com.offlinejournal.presentation.theme.NavyCard
import com.offlinejournal.presentation.theme.NavyDark
import com.offlinejournal.presentation.theme.NavySurface
import com.offlinejournal.presentation.theme.PurpleDark
import com.offlinejournal.presentation.theme.PurplePrimary
import com.offlinejournal.presentation.viewmodel.CategoriesViewModel
import com.offlinejournal.presentation.viewmodel.HomeViewModel
import com.offlinejournal.presentation.viewmodel.RemindersViewModel
import com.offlinejournal.util.PersianFormatter
import com.offlinejournal.util.TehranTime
import kotlinx.coroutines.delay

private enum class VoiceNoteStep { None, Category, Recording }

@Composable
fun HomeScreen(
    container: AppContainer,
    onNewReminder: () -> Unit,
    onNoteClick: (Long) -> Unit,
    onAllNotesClick: () -> Unit,
    onSearch: () -> Unit,
    onEditReminder: (Long) -> Unit,
    homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory(container)),
    categoriesViewModel: CategoriesViewModel = viewModel(factory = CategoriesViewModel.Factory(container)),
    remindersViewModel: RemindersViewModel = viewModel(factory = RemindersViewModel.Factory(container))
) {
    var selectedTab by rememberSaveable { mutableStateOf(HomeTab.HOME) }
    var voiceNoteStep by rememberSaveable { mutableStateOf(VoiceNoteStep.None) }
    var voiceCategoryId by rememberSaveable { mutableStateOf<Long?>(null) }
    val homeState by homeViewModel.uiState.collectAsState()
    val categories by categoriesViewModel.categories.collectAsState()
    val upcomingReminders by remindersViewModel.upcomingReminders.collectAsState()

    Scaffold(
        containerColor = NavyDark,
        bottomBar = {
            JayMinderBottomBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        }
    ) { padding ->
        when (selectedTab) {
            HomeTab.HOME -> HomeDashboardContent(
                modifier = Modifier.padding(padding),
                homeState = homeState,
                onNewVoiceNote = { voiceNoteStep = VoiceNoteStep.Category },
                onNewReminder = onNewReminder,
                onNoteClick = onNoteClick,
                onAllNotesClick = onAllNotesClick,
                onSearch = onSearch,
                onEditReminder = onEditReminder,
                formatReminderTime = homeViewModel::formatReminderTime,
                formatReminderRelative = homeViewModel::formatReminderRelative
            )
            HomeTab.CATEGORIES -> CategoriesTabContent(
                modifier = Modifier.padding(padding),
                categories = categories,
                onDelete = categoriesViewModel::deleteCategory,
                viewModel = categoriesViewModel
            )
            HomeTab.REMINDERS -> RemindersTabContent(
                modifier = Modifier.padding(padding),
                reminders = upcomingReminders,
                formatTime = remindersViewModel::formatReminderTime,
                onEdit = onEditReminder,
                onDelete = remindersViewModel::deleteReminder,
                onComplete = remindersViewModel::markCompleted
            )
        }
    }

    if (voiceNoteStep == VoiceNoteStep.Category) {
        CategorySelectModal(
            container = container,
            onDismiss = { voiceNoteStep = VoiceNoteStep.None },
            onCategorySelected = { categoryId ->
                voiceCategoryId = categoryId
                voiceNoteStep = VoiceNoteStep.Recording
            }
        )
    }

    if (voiceNoteStep == VoiceNoteStep.Recording && voiceCategoryId != null) {
        VoiceRecordScreen(
            container = container,
            categoryId = voiceCategoryId!!,
            onDismiss = {
                voiceNoteStep = VoiceNoteStep.None
                voiceCategoryId = null
            },
            onSaved = {
                voiceNoteStep = VoiceNoteStep.None
                voiceCategoryId = null
            }
        )
    }
}

@Composable
private fun HomeDashboardContent(
    modifier: Modifier = Modifier,
    homeState: com.offlinejournal.presentation.viewmodel.HomeUiState,
    onNewVoiceNote: () -> Unit,
    onNewReminder: () -> Unit,
    onNoteClick: (Long) -> Unit,
    onAllNotesClick: () -> Unit,
    onSearch: () -> Unit,
    onEditReminder: (Long) -> Unit,
    formatReminderTime: (Reminder) -> String,
    formatReminderRelative: (Reminder) -> String?
) {
    var menuExpanded by rememberSaveable { mutableStateOf(false) }
    var liveTime by remember { mutableStateOf(PersianFormatter.formatTime(TehranTime.nowMillis())) }
    var liveDate by remember {
        mutableStateOf(PersianFormatter.formatJalaliDate(TehranTime.nowMillis(), includeWeekday = true))
    }

    LaunchedEffect(Unit) {
        while (true) {
            val now = TehranTime.nowMillis()
            liveTime = PersianFormatter.formatTime(now)
            liveDate = PersianFormatter.formatJalaliDate(now, includeWeekday = true)
            delay(1000)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item {
            DashboardHeader(
                greetingDate = liveDate,
                currentTime = liveTime,
                menuExpanded = menuExpanded,
                onMenuToggle = { menuExpanded = it },
                onSearch = onSearch,
                onNewVoiceNote = onNewVoiceNote,
                onNewReminder = onNewReminder
            )
        }

        item {
            SectionHeader(
                title = "امروز",
                icon = Icons.Default.CalendarMonth,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )
        }

        if (homeState.todayReminders.isEmpty()) {
            item {
                Text(
                    text = "یادآوری برای امروز ندارید.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }
        } else {
            items(homeState.todayReminders, key = { it.id }) { reminder ->
                TodayReminderCard(
                    reminder = reminder,
                    timeText = formatReminderTime(reminder),
                    relativeText = formatReminderRelative(reminder),
                    onClick = { onEditReminder(reminder.id) },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp, top = 20.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeader(
                    title = "یادداشت‌های اخیر",
                    icon = Icons.Default.Description,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onAllNotesClick) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "همه یادداشت‌ها",
                        tint = PurplePrimary
                    )
                }
            }
        }

        if (homeState.recentNotes.isEmpty()) {
            item {
                Text(
                    text = "هنوز یادداشتی ندارید.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }
        } else {
            items(homeState.recentNotes, key = { it.id }) { note ->
                RecentNoteCard(
                    note = note,
                    onClick = { onNoteClick(note.id) },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun DashboardHeader(
    greetingDate: String,
    currentTime: String,
    menuExpanded: Boolean,
    onMenuToggle: (Boolean) -> Unit,
    onSearch: () -> Unit,
    onNewVoiceNote: () -> Unit,
    onNewReminder: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(290.dp)
        ) {
            MountainBackground()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        IconButton(onClick = { onMenuToggle(true) }) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = "منو",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { onMenuToggle(false) }
                        ) {
                            DropdownMenuItem(
                                text = { Text("جستجو") },
                                onClick = {
                                    onMenuToggle(false)
                                    onSearch()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Search, contentDescription = null)
                                }
                            )
                        }
                    }
                    Image(
                        painter = painterResource(R.drawable.ic_app_icon),
                        contentDescription = "JayMinder",
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .border(2.dp, PurplePrimary.copy(alpha = 0.6f), CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = PurplePrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = greetingDate,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFCBD5E1)
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            tint = PurplePrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = currentTime,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFCBD5E1)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                ShortcutCardsRow(
                    onNewReminder = onNewReminder,
                    onNewVoiceNote = onNewVoiceNote,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ShortcutCardsRow(
    onNewReminder: () -> Unit,
    onNewVoiceNote: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ShortcutCard(
            label = "یادداشت صوتی",
            icon = Icons.Default.Mic,
            onClick = onNewVoiceNote,
            modifier = Modifier.weight(1f)
        )
        ShortcutCard(
            label = "یادآوری جدید",
            icon = Icons.Default.Notifications,
            onClick = onNewReminder,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ShortcutCard(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(112.dp)
            .shadow(8.dp, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E2A42)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF243352),
                            NavyCard
                        )
                    ),
                    RoundedCornerShape(18.dp)
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(PurplePrimary.copy(alpha = 0.7f), PurpleDark.copy(alpha = 0.4f))
                    ),
                    shape = RoundedCornerShape(18.dp)
                )
                .padding(14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(PurplePrimary.copy(alpha = 0.35f), PurpleDark.copy(alpha = 0.2f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = PurplePrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PurplePrimary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun TodayReminderCard(
    reminder: Reminder,
    timeText: String,
    relativeText: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = NavySurface.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF2D3A52), RoundedCornerShape(14.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = PurplePrimary
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(PurplePrimary)
                )
                Column {
                    Text(
                        text = reminder.title,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    relativeText?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }
            Icon(
                Icons.Default.Event,
                contentDescription = null,
                tint = PurplePrimary.copy(alpha = 0.7f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun RecentNoteCard(
    note: Note,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NavyCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF2D3A52), RoundedCornerShape(16.dp))
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = note.title ?: "بدون عنوان",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (note.displayText.isNotBlank()) {
                    val previewLines = note.displayText.lines().take(2)
                    previewLines.forEach { line ->
                        Text(
                            text = if (line.startsWith("-") || line.startsWith("•")) line else "• $line",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
            Icon(
                Icons.Default.Description,
                contentDescription = null,
                tint = PurplePrimary,
                modifier = Modifier
                    .size(32.dp)
                    .align(Alignment.CenterVertically)
            )
        }
    }
}

@Composable
private fun CategoriesTabContent(
    modifier: Modifier = Modifier,
    categories: List<Category>,
    onDelete: (Category) -> Unit,
    viewModel: CategoriesViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "دسته‌بندی‌ها",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )
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
            colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
            shape = RoundedCornerShape(12.dp)
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
                        colors = CardDefaults.cardColors(containerColor = NavyCard),
                        shape = RoundedCornerShape(14.dp)
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
private fun RemindersTabContent(
    modifier: Modifier = Modifier,
    reminders: List<Reminder>,
    formatTime: (Reminder) -> String,
    onEdit: (Long) -> Unit,
    onDelete: (Reminder) -> Unit,
    onComplete: (Reminder) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = "یادآوری‌ها",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp)
        )

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
                        colors = CardDefaults.cardColors(containerColor = NavyCard),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(reminder.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = formatTime(reminder),
                                style = MaterialTheme.typography.bodySmall,
                                color = PurplePrimary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
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
}
