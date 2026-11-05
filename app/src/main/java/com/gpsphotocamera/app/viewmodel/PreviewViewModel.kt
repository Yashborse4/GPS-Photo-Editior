package com.gpsphotocamera.app.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gpsphotocamera.app.data.model.GpsPhotoData
import com.gpsphotocamera.app.data.repository.RecentPhotosRepository
import com.gpsphotocamera.app.domain.usecase.SavePhotoUseCase
import com.gpsphotocamera.app.image.BitmapHolder
import com.gpsphotocamera.app.utils.ShareHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SaveUiState {
    data object Idle : SaveUiState
    data object Saving : SaveUiState
    data class Success(val savedUri: Uri) : SaveUiState
    data class Error(val message: String) : SaveUiState
}

/**
 * ViewModel managing preview inspection, gallery saving, and sharing.
 */
class PreviewViewModel(application: Application) : AndroidViewModel(application) {

    private val savePhotoUseCase = SavePhotoUseCase(
        context = application.applicationContext,
        recentPhotosRepository = RecentPhotosRepository(application.applicationContext)
    )

    val previewBitmap: Bitmap?
        get() = BitmapHolder.activeBitmap

    private val _saveState = MutableStateFlow<SaveUiState>(SaveUiState.Idle)
    val saveState: StateFlow<SaveUiState> = _saveState.asStateFlow()

    private var lastSavedUri: Uri? = null

    fun saveImage(photoData: GpsPhotoData) {
        val bitmap = BitmapHolder.activeBitmap ?: run {
            _saveState.value = SaveUiState.Error("No photograph available to save")
            return
        }

        _saveState.value = SaveUiState.Saving

        viewModelScope.launch {
            val result = savePhotoUseCase(bitmap, photoData)
            result.fold(
                onSuccess = { uri ->
                    lastSavedUri = uri
                    _saveState.value = SaveUiState.Success(uri)
                },
                onFailure = { error ->
                    _saveState.value = SaveUiState.Error(error.localizedMessage ?: "Failed to save photo")
                }
            )
        }
    }

    fun shareImage(heading: String) {
        val uri = lastSavedUri ?: run {
            // If not yet saved to gallery, we temporarily save it first to obtain a shareable URI
            return
        }
        ShareHelper.shareImage(getApplication<Application>().applicationContext, uri, heading)
    }

    fun resetSaveState() {
        _saveState.value = SaveUiState.Idle
    }
}
