package com.valuai.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.valuai.AppEventBus
import com.valuai.TokenManager
import com.valuai.network.ApiClient
import com.valuai.network.EstimationResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class HistoryState {
    object Loading : HistoryState()
    data class Success(val items: List<EstimationResponse>) : HistoryState()
    data class Error(val message: String) : HistoryState()
}

class HistoryViewModel : ViewModel() {
    private val _state = MutableStateFlow<HistoryState>(HistoryState.Loading)
    val state: StateFlow<HistoryState> = _state

    fun loadHistory(context: Context) {
        viewModelScope.launch {
            _state.value = HistoryState.Loading
            try {
                val token = TokenManager(context).token.first()
                val response = ApiClient.api.getEstimations("Bearer $token")
                when {
                    response.code() == 401 -> {
                        TokenManager(context).clearToken()
                        AppEventBus.emitUnauthorized()
                    }
                    response.isSuccessful -> {
                        _state.value = HistoryState.Success(response.body() ?: emptyList())
                    }
                    else -> {
                        _state.value = HistoryState.Error("Hiba: ${response.code()}")
                    }
                }
            } catch (e: Exception) {
                _state.value = HistoryState.Error(e.message ?: "Ismeretlen hiba")
            }
        }
    }
}