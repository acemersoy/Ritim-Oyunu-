package com.rhythmgame.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rhythmgame.data.repository.CurrencyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StoreViewModel @Inject constructor(
    private val currencyRepository: CurrencyRepository,
) : ViewModel() {

    val coins: StateFlow<Long> = currencyRepository.coins
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    private val _purchaseResult = MutableStateFlow<PurchaseResult?>(null)
    val purchaseResult = _purchaseResult.asStateFlow()

    fun purchaseItem(price: Int) {
        viewModelScope.launch {
            val success = currencyRepository.spendCoins(price.toLong())
            _purchaseResult.value = if (success) PurchaseResult.SUCCESS else PurchaseResult.INSUFFICIENT_FUNDS
        }
    }

    fun addCoinsFromAd(amount: Long = 10) {
        viewModelScope.launch {
            currencyRepository.addCoins(amount)
        }
    }

    fun clearPurchaseResult() {
        _purchaseResult.value = null
    }
}

enum class PurchaseResult {
    SUCCESS,
    INSUFFICIENT_FUNDS,
}
