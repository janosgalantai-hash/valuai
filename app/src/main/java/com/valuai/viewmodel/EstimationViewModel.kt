package com.valuai.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.valuai.TokenManager
import com.valuai.network.ApiClient
import com.valuai.network.EstimationResponse
import com.valuai.network.ImageRepository
import com.valuai.network.ResultRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

sealed class EstimationState {
    object Idle    : EstimationState()
    object Loading : EstimationState()
    data class Success(val result: EstimationResponse) : EstimationState()
    data class Error(val message: String)              : EstimationState()
}

class EstimationViewModel : ViewModel() {

    private val _state = MutableStateFlow<EstimationState>(EstimationState.Idle)
    val state: StateFlow<EstimationState> = _state

    private val _imagePaths = MutableStateFlow<List<String>>(emptyList())
    val imagePaths: StateFlow<List<String>> = _imagePaths

    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description

    fun addImage(filePath: String) {
        if (_imagePaths.value.size < 4) {
            _imagePaths.value = _imagePaths.value + filePath
        }
    }

    fun addImageFromGallery(context: Context, uri: Uri) {
        if (_imagePaths.value.size >= 4) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dir = File(context.filesDir, "valuai_images").also { it.mkdirs() }
                val dest = File(dir, "img_${System.currentTimeMillis()}.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(dest).use { output -> input.copyTo(output) }
                }
                _imagePaths.value = _imagePaths.value + dest.absolutePath
            } catch (e: Exception) {
                android.util.Log.e("Gallery", "Failed to copy image: ${e.message}")
            }
        }
    }

    fun removeImage(index: Int) {
        _imagePaths.value = _imagePaths.value.toMutableList().also { it.removeAt(index) }
    }

    fun setDescription(text: String) {
        _description.value = text
    }

    private var resultNavigated = false

    fun markResultNavigated() { resultNavigated = true }
    fun isResultNavigated() = resultNavigated

    fun resetState() {
        _state.value = EstimationState.Idle
        resultNavigated = false
    }

    fun reset() {
        _state.value = EstimationState.Idle
        _imagePaths.value = emptyList()
        _description.value = ""
        resultNavigated = false
    }

    fun startEstimation(context: Context, description: String) {
        val paths = _imagePaths.value
        viewModelScope.launch {
            _state.value = EstimationState.Loading
            try {
                val tokenManager = TokenManager(context)
                val token = tokenManager.token.first()
                val currency = tokenManager.currency.first()
                val authToken = "Bearer ${token ?: "TEST_TOKEN"}"

                val imageParts = paths.mapIndexed { index, path ->
                    val src = File(path)
                    val tempFile = File(context.cacheDir, "upload_$index.jpg")
                    val bitmap = android.graphics.BitmapFactory.decodeFile(path)
                    FileOutputStream(tempFile).use { out ->
                        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, out)
                    }
                    val requestBody = tempFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    MultipartBody.Part.createFormData("images", tempFile.name, requestBody)
                }

                val descriptionBody = description.toRequestBody("text/plain".toMediaTypeOrNull())
                val currencyBody = currency.toRequestBody("text/plain".toMediaTypeOrNull())

                val response = ApiClient.api.createEstimation(
                    token       = authToken,
                    images      = imageParts,
                    description = descriptionBody,
                    currency    = currencyBody
                )

                if (response.isSuccessful) {
                    response.body()?.let {
                        ResultRepository.lastResult = it
                        ImageRepository.saveImages(context, it.id, paths)
                        _state.value = EstimationState.Success(it)
                    } ?: run {
                        _state.value = EstimationState.Error("Empty response from server")
                    }
                } else {
                    _state.value = EstimationState.Error("Hiba: ${response.code()} — ${response.message()}")
                }
            } catch (e: Exception) {
                _state.value = EstimationState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
