package com.offlinejournal.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.offlinejournal.data.local.AppContainer
import com.offlinejournal.service.market.MarketPrices
import com.offlinejournal.util.PersianFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class MarketPriceUiState(
    val usdText: String = "—",
    val goldText: String = "—"
)

class MarketPriceViewModel(private val container: AppContainer) : ViewModel() {
    private val _uiState = MutableStateFlow(MarketPriceUiState())
    val uiState: StateFlow<MarketPriceUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            while (isActive) {
                refresh()
                delay(60_000)
            }
        }
    }

    private suspend fun refresh() {
        val prices = container.marketPriceRepository.fetchPrices()
        _uiState.value = MarketPriceUiState(
            usdText = formatToman(prices.usdToman),
            goldText = formatToman(prices.goldToman)
        )
    }

    private fun formatToman(value: Long?): String {
        if (value == null) return "—"
        return PersianFormatter.toPersianDigits(
            value.toString().reversed().chunked(3).joinToString(",").reversed()
        ) + " تومان"
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MarketPriceViewModel(container) as T
        }
    }
}
