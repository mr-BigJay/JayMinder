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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.material3.LocalTextStyle
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

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                JalaliNumberField(
                    value = uiState.jalaliDay,
                    label = "روز",
                    maxLength = 2,
                    onValueChange = { day ->
                        viewModel.updateDate(uiState.jalaliYear, uiState.jalaliMonth, day)
                    },
                    modifier = Modifier.weight(1f),
                    resetKey = uiState.dateFieldsResetKey
                )
                JalaliNumberField(
                    value = uiState.jalaliMonth,
                    label = "ماه",
                    maxLength = 2,
                    onValueChange = { month ->
                        viewModel.updateDate(uiState.jalaliYear, month, uiState.jalaliDay)
                    },
                    modifier = Modifier.weight(1f),
                    resetKey = uiState.dateFieldsResetKey
                )
                JalaliNumberField(
                    value = uiState.jalaliYear,
                    label = "سال",
                    maxLength = 4,
                    onValueChange = { year ->
                        viewModel.updateDate(year, uiState.jalaliMonth, uiState.jalaliDay)
                    },
                    modifier = Modifier.weight(1f),
                    resetKey = uiState.dateFieldsResetKey
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

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                JalaliNumberField(
                    value = uiState.minute,
                    label = "دقیقه",
                    maxLength = 2,
                    allowZero = true,
                    onValueChange = { m -> viewModel.updateTime(uiState.hour, m) },
                    modifier = Modifier.weight(1f),
                    resetKey = uiState.timeFieldsResetKey
                )
                JalaliNumberField(
                    value = uiState.hour,
                    label = "ساعت",
                    maxLength = 2,
                    allowZero = true,
                    onValueChange = { h -> viewModel.updateTime(h, uiState.minute) },
                    modifier = Modifier.weight(1f),
                    resetKey = uiState.timeFieldsResetKey
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
    modifier: Modifier = Modifier,
    maxLength: Int = 4,
    allowZero: Boolean = false,
    resetKey: Int = 0
) {
    var text by remember(resetKey, label) {
        mutableStateOf(value.toDisplayText(allowZero))
    }

    LaunchedEffect(resetKey, value) {
        val parsed = text.toIntOrNull()
        if (parsed != value) {
            text = value.toDisplayText(allowZero)
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        OutlinedTextField(
            value = text,
            onValueChange = { raw ->
                val digits = PersianFormatter.normalizeToLatinDigits(raw)
                    .filter { it.isDigit() }
                    .take(maxLength)
                text = digits
                if (digits.isNotEmpty()) {
                    digits.toIntOrNull()?.let(onValueChange)
                }
            },
            label = { Text(label) },
            modifier = modifier,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr)
        )
    }
}

private fun Int.toDisplayText(allowZero: Boolean): String =
    when {
        this == 0 && !allowZero -> ""
        else -> toString()
    }
