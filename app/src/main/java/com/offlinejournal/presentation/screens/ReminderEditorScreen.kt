package com.offlinejournal.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.offlinejournal.data.local.AppContainer
import com.offlinejournal.presentation.components.JournalTopBar
import com.offlinejournal.presentation.viewmodel.ReminderEditorViewModel
import com.offlinejournal.util.JalaliCalendar
import com.offlinejournal.util.JalaliDate
import com.offlinejournal.util.PersianFormatter

@Composable
fun ReminderEditorScreen(
    container: AppContainer,
    reminderId: Long? = null,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: ReminderEditorViewModel = viewModel(
        factory = ReminderEditorViewModel.Factory(container, reminderId)
    )
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.saved) {
        if (uiState.saved) onSaved()
    }

    Scaffold(
        topBar = {
            JournalTopBar(
                title = if (reminderId == null) "یادآوری جدید" else "ویرایش یادآوری",
                onBack = onBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = uiState.title,
                onValueChange = viewModel::updateTitle,
                label = { Text("عنوان یادآوری") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = uiState.description,
                onValueChange = viewModel::updateDescription,
                label = { Text("توضیحات") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            Text(text = "تاریخ (شمسی)", style = MaterialTheme.typography.titleMedium)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                JalaliNumberField(
                    value = uiState.jalaliYear,
                    label = "سال",
                    onValueChange = { year ->
                        viewModel.updateDate(year, uiState.jalaliMonth, uiState.jalaliDay)
                    },
                    modifier = Modifier.weight(1f)
                )
                JalaliNumberField(
                    value = uiState.jalaliMonth,
                    label = "ماه",
                    onValueChange = { month ->
                        val clamped = month.coerceIn(1, 12)
                        viewModel.updateDate(uiState.jalaliYear, clamped, uiState.jalaliDay)
                    },
                    modifier = Modifier.weight(1f)
                )
                JalaliNumberField(
                    value = uiState.jalaliDay,
                    label = "روز",
                    onValueChange = { day ->
                        val maxDay = JalaliCalendar.daysInJalaliMonth(uiState.jalaliYear, uiState.jalaliMonth)
                        viewModel.updateDate(uiState.jalaliYear, uiState.jalaliMonth, day.coerceIn(1, maxDay))
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = PersianFormatter.formatJalaliDate(
                    JalaliCalendar.startOfJalaliDayMillis(
                        JalaliDate(uiState.jalaliYear, uiState.jalaliMonth, uiState.jalaliDay)
                    )
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Text(text = "ساعت (تهران)", style = MaterialTheme.typography.titleMedium)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                JalaliNumberField(
                    value = uiState.hour,
                    label = "ساعت",
                    onValueChange = { h -> viewModel.updateTime(h.coerceIn(0, 23), uiState.minute) },
                    modifier = Modifier.weight(1f)
                )
                JalaliNumberField(
                    value = uiState.minute,
                    label = "دقیقه",
                    onValueChange = { m -> viewModel.updateTime(uiState.hour, m.coerceIn(0, 59)) },
                    modifier = Modifier.weight(1f)
                )
            }

            uiState.errorMessage?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = viewModel::saveReminder,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("ذخیره یادآوری")
            }
        }
    }
}

@Composable
private fun JalaliNumberField(
    value: Int,
    label: String,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = if (value == 0) "" else value.toString(),
        onValueChange = { text ->
            val num = text.filter { it.isDigit() }.toIntOrNull() ?: 0
            onValueChange(num)
        },
        label = { Text(label) },
        modifier = modifier,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}
