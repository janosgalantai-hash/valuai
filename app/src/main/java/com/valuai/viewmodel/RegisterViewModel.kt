package com.valuai.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.valuai.network.ApiClient
import com.valuai.network.RegisterRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class RegisterState {
    object Idle    : RegisterState()
    object Loading : RegisterState()
    object Success : RegisterState()
    data class Error(val message: String) : RegisterState()
}

class RegisterViewModel : ViewModel() {
    private val _state = MutableStateFlow<RegisterState>(RegisterState.Idle)
    val state: StateFlow<RegisterState> = _state

    fun register(email: String, password: String, confirmPassword: String) {
        if (email.isBlank() || password.isBlank()) {
            _state.value = RegisterState.Error("Please fill in all fields")
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _state.value = RegisterState.Error("Invalid email address")
            return
        }
        if (password.length < 8) {
            _state.value = RegisterState.Error("Password must be at least 8 characters")
            return
        }
        if (password != confirmPassword) {
            _state.value = RegisterState.Error("Passwords do not match")
            return
        }

        viewModelScope.launch {
            _state.value = RegisterState.Loading
            try {
                val response = ApiClient.api.register(RegisterRequest(email, password))
                if (response.isSuccessful) {
                    _state.value = RegisterState.Success
                } else {
                    val errorMsg = when (response.code()) {
                        400 -> "This email is already in use"
                        422 -> "Invalid data"
                        else -> "Hiba: ${response.code()}"
                    }
                    _state.value = RegisterState.Error(errorMsg)
                }
            } catch (e: Exception) {
                _state.value = RegisterState.Error(e.message ?: "Connection error")
            }
        }
    }

    fun resetState() {
        _state.value = RegisterState.Idle
    }
}
