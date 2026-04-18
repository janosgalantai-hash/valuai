package com.valuai.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.valuai.TokenManager
import com.valuai.network.ApiClient
import com.valuai.network.EstimationResponse
import com.valuai.network.ResultRepository
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

    // Ezek túlélik az orientáció váltást
    private val _selectedImages = MutableStateFlow<List<Uri>>(emptyList())
    val selectedImages: StateFlow<List<Uri>> = _selectedImages

    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description

    fun addImage(uri: Uri) {
        if (_selectedImages.value.size < 4) {
            _selectedImages.value = _selectedImages.value + uri
        }
    }

    fun removeImage(index: Int) {
        _selectedImages.value = _selectedImages.value.toMutableList().also { it.removeAt(index) }
    }

    fun setDescription(text: String) {
        _description.value = text
    }

    private var resultNavigated = false

    fun markResultNavigated() {
        resultNavigated = true
    }

    fun isResultNavigated() = resultNavigated

    // Csak az estimation state-et reseteli, képeket és leírást megtartja
    fun resetState() {
        _state.value = EstimationState.Idle
        resultNavigated = false
    }

    // Teljes reset (új értékbecslés gomb után)
    fun reset() {
        _state.value = EstimationState.Idle
        _selectedImages.value = emptyList()
        _description.value = ""
        resultNavigated = false
    }
    fun startEstimation(
        context: Context,
        imageUris: List<Uri>,
        description: String
    ) {
        viewModelScope.launch {
            _state.value = EstimationState.Loading
            try {
                val tokenManager = TokenManager(context)
                val token = tokenManager.token.first()
                val currency = tokenManager.currency.first()
                val authToken = "Bearer ${token ?: "TEST_TOKEN"}"

                val imageParts = imageUris.mapIndexed { index, uri ->
                    val file = uriToFile(context, uri, "image_$index.jpg")
                    val requestBody = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    MultipartBody.Part.createFormData("images", file.name, requestBody)
                }

                val descriptionBody = description
                    .toRequestBody("text/plain".toMediaTypeOrNull())

                val currencyBody = currency
                    .toRequestBody("text/plain".toMediaTypeOrNull())

                val response = ApiClient.api.createEstimation(
                    token       = authToken,
                    images      = imageParts,
                    description = descriptionBody,
                    currency    = currencyBody
                )

                if (response.isSuccessful) {
                    response.body()?.let {
                        ResultRepository.lastResult = it
                        _state.value = EstimationState.Success(it)
                    } ?: run {
                        _state.value = EstimationState.Error("Empty response from server")
                    }
                } else {
                    _state.value = EstimationState.Error(
                        "Hiba: ${response.code()} — ${response.message()}"
                    )
                }
            } catch (e: Exception) {
                _state.value = EstimationState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun uriToFile(context: Context, uri: Uri, fileName: String): File {
        val file = File(context.cacheDir, fileName)

        // Bitmap-ként olvassuk be és JPEG-ként mentjük
        val inputStream = context.contentResolver.openInputStream(uri)!!
        val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
        inputStream.close()

        java.io.FileOutputStream(file).use { out ->
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, out)
        }

        return file
    }
}