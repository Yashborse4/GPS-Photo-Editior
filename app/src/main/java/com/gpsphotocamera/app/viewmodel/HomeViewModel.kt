package com.gpsphotocamera.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gpsphotocamera.app.data.model.SavedPhotoItem
import com.gpsphotocamera.app.data.repository.RecentPhotosRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel powering the HomeScreen:
 * - Observes recent stamped photographs
 * - Handles deletion of items
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RecentPhotosRepository(application.applicationContext)
    val recentPhotos: StateFlow<List<SavedPhotoItem>> = repository.recentPhotos

    fun deleteRecentPhoto(id: String) {
        viewModelScope.launch {
            repository.removePhoto(id)
        }
    }
}
