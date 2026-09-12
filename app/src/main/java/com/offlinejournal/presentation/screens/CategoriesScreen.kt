package com.offlinejournal.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.offlinejournal.data.local.AppContainer
import com.offlinejournal.domain.model.Category
import com.offlinejournal.presentation.components.EmptyStateMessage
import com.offlinejournal.presentation.components.JournalTopBar
import com.offlinejournal.presentation.viewmodel.CategoriesViewModel

@Composable
fun CategoriesScreen(
    container: AppContainer,
    onBack: () -> Unit,
    viewModel: CategoriesViewModel = viewModel(factory = CategoriesViewModel.Factory(container))
) {
    val categories by viewModel.categories.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            JournalTopBar(title = "دسته‌بندی‌ها", onBack = onBack)
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = uiState.newCategoryName,
                onValueChange = viewModel::updateNewCategoryName,
                label = {
                    Text(if (uiState.editingCategory != null) "ویرایش نام دسته" else "نام دسته جدید")
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = viewModel::saveCategory) {
                    Text(if (uiState.editingCategory != null) "ذخیره تغییرات" else "افزودن دسته")
                }
                if (uiState.editingCategory != null) {
                    Button(onClick = viewModel::cancelEditing) {
                        Text("لغو")
                    }
                }
            }

            uiState.errorMessage?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error)
            }

            if (categories.isEmpty()) {
                EmptyStateMessage("هنوز دسته‌بندی ایجاد نشده است.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories, key = { it.id }) { category ->
                        CategoryItem(
                            category = category,
                            onEdit = { viewModel.startEditing(category) },
                            onDelete = { viewModel.deleteCategory(category) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryItem(
    category: Category,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = category.name, style = MaterialTheme.typography.titleMedium)
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "ویرایش")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "حذف")
                }
            }
        }
    }
}
