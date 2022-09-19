package com.es.multivs.presentation.view.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.es.multivs.data.models.VideoModel
import com.es.multivs.data.repository.VideoRepository
import com.es.multivs.data.utils.AppUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import retrofit2.Response
import javax.inject.Inject

@HiltViewModel
class VideoViewModel @Inject constructor(
    private val videoRepository: VideoRepository,
    application: Application
) : AndroidViewModel(application) {

    private lateinit var videoModel :VideoModel
    fun getVideoUrl(): VideoModel {
        return videoModel
    }

    suspend fun loadVideoUrl() : VideoModel {
        val isInternetAvailable = AppUtils.isInternetAvailable(getApplication())
        if (isInternetAvailable) {
            val response: Response<VideoModel> = videoRepository.getUserURL()

            if (response.isSuccessful) {
                response.body()?.let {
                    videoModel = it
                }
            }
        }
        return videoModel
    }
}