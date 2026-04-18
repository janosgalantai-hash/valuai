package com.valuai.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.valuai.TokenManager
import com.valuai.network.ApiClient
import com.valuai.network.LoginRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class LoginState {
    object Idle    : LoginState()
    object Loading : LoginState()
    object Success : LoginState()
    data class Error(val message: String) : LoginState()
}

class LoginViewModel : ViewModel() {
    private val _state = MutableStateFlow<LoginState>(LoginState.Idle)
    val state: StateFlow<LoginState> = _state

    fun login(context: Context, email: String, password: String) {
        viewModelScope.launch {
            _state.value = LoginState.Loading
            try {
                val response = ApiClient.api.login(LoginRequest(email, password))
                if (response.isSuccessful) {
                    response.body()?.let { body ->
                        TokenManager(context).saveToken(body.access_token)
                        _state.value = LoginState.Success
                    }
                } else {
                    _state.value = LoginState.Error("Incorrect email or password")
                }
            } catch (e: Exception) {
                _state.value = LoginState.Error(e.message ?: "Connection error")
            }
        }
    }
}