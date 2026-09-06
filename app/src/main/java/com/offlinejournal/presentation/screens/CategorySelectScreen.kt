package com.offlinejournal.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.offlinejournal.data.local.AppContainer
import com.offlinejournal.domain.model.Category
import com.offlinejournal.presentation.components.EmptyStateMessage
import com.offlinejournal.presentation.components.JournalTopBar
import com.offlinejournal.presentation.viewmodel.CategoriesViewModel

@Composable
fun CategorySelectScreen(
    container: AppContainer,
    onBack: () -> Unit,
    onCategorySelected: (Long) -> Unit,
    viewModel: CategoriesViewModel = viewModel(factory = CategoriesViewModel.Factory(container))
) {
    val categories by viewModel.categories.collectAsState()

    Scaffold(
        topBar = {
            JournalTopBar(title = "انتخاب دسته‌بندی", onBack = onBack)
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = "ابتدا دسته‌بندی یادداشت را انتخاب کنید",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (categories.isEmpty()) {
                EmptyStateMessage(
                    message = "دسته‌بندی وجود ندارد. از منوی پایین «دسته‌بندی‌ها» یک دسته بسازید.",
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(categories, key = { it.id }) { category ->
                        CategorySelectCard(
                            category = category,
                            onClick = { onCategorySelected(category.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategorySelectCard(category: Category, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = category.name,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
