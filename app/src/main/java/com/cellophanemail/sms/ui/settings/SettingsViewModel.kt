package com.cellophanemail.sms.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.cellophanemail.sms.data.local.NerModelManager
import com.cellophanemail.sms.data.local.NerProviderMode
import com.cellophanemail.sms.data.local.NerProviderPreferences
import com.cellophanemail.sms.data.local.TextRenderingPreferences
import com.cellophanemail.sms.data.remote.api.CellophoneMailApi
import com.cellophanemail.sms.data.remote.model.UserProfile
import com.cellophanemail.sms.data.repository.AuthRepository
import com.cellophanemail.sms.domain.model.IlluminatedStylePack
import com.cellophanemail.sms.workers.NerModelDownloadWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ProfileState {
    data object Loading : ProfileState()
    data class Loaded(val profile: UserProfile) : ProfileState()
    data class Error(val message: String) : ProfileState()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository,
    private val api: CellophoneMailApi,
    private val textRenderingPreferences: TextRenderingPreferences,
    private val nerPreferences: NerProviderPreferences,
    private val nerModelManager: NerModelManager
) : ViewModel() {

    private val workManager = WorkManager.getInstance(context)

    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Loading)
    val profileState: StateFlow<ProfileState> = _profileState.asStateFlow()

    val illuminatedEnabled: StateFlow<Boolean> = textRenderingPreferences.illuminatedEnabled
    val selectedStylePack: StateFlow<IlluminatedStylePack> = textRenderingPreferences.selectedStylePack
    val entityHighlightsEnabled: StateFlow<Boolean> = textRenderingPreferences.entityHighlightsEnabled

    // NER provider preferences
    val nerProviderMode: StateFlow<NerProviderMode> = nerPreferences.selectedProvider
    val nerModelDownloaded: StateFlow<Boolean> = nerPreferences.modelDownloaded
    val nerWifiOnlyDownload: StateFlow<Boolean> = nerPreferences.wifiOnlyDownload

    // NER model download state
    private val _nerDownloadProgress = MutableStateFlow<Int?>(null)
    val nerDownloadProgress: StateFlow<Int?> = _nerDownloadProgress.asStateFlow()

    private val _nerIsDownloading = MutableStateFlow(false)
    val nerIsDownloading: StateFlow<Boolean> = _nerIsDownloading.asStateFlow()

    init {
        observeDownloadWork()
    }

    private fun observeDownloadWork() {
        viewModelScope.launch {
            workManager.getWorkInfosForUniqueWorkLiveData(NerModelDownloadWorker.WORK_NAME)
                .observeForever { workInfos ->
                    val info = workInfos?.firstOrNull()
                    when (info?.state) {
                        WorkInfo.State.RUNNING -> {
                            _nerIsDownloading.value = true
                            val progress = info.progress.getInt(NerModelDownloadWorker.KEY_PROGRESS, 0)
                            _nerDownloadProgress.value = progress
                        }
                        WorkInfo.State.ENQUEUED -> {
                            _nerIsDownloading.value = true
                            _nerDownloadProgress.value = null
                        }
                        WorkInfo.State.SUCCEEDED -> {
                            _nerIsDownloading.value = false
                            _nerDownloadProgress.value = null
                        }
                        WorkInfo.State.FAILED, WorkInfo.State.CANCELLED -> {
                            _nerIsDownloading.value = false
                            _nerDownloadProgress.value = null
                        }
                        else -> {
                            _nerIsDownloading.value = false
                            _nerDownloadProgress.value = null
                        }
                    }
                }
        }
    }

    fun startNerModelDownload(downloadUrl: String) {
        val request = NerModelDownloadWorker.buildRequest(downloadUrl)
        workManager.enqueueUniqueWork(
            NerModelDownloadWorker.WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    fun setNerProviderMode(mode: NerProviderMode) {
        nerPreferences.setSelectedProvider(mode)
    }

    fun setNerWifiOnlyDownload(wifiOnly: Boolean) {
        nerPreferences.setWifiOnlyDownload(wifiOnly)
    }

    fun deleteNerModel() {
        nerModelManager.deleteModel()
    }

    fun setIlluminatedEnabled(enabled: Boolean) {
        textRenderingPreferences.setIlluminatedEnabled(enabled)
    }

    fun setSelectedStylePack(pack: IlluminatedStylePack) {
        textRenderingPreferences.setSelectedStylePack(pack)
    }

    fun setEntityHighlightsEnabled(enabled: Boolean) {
        textRenderingPreferences.setEntityHighlightsEnabled(enabled)
    }

    fun loadProfile() {
        viewModelScope.launch {
            _profileState.value = ProfileState.Loading
            try {
                val response = api.getUserProfile()
                if (response.isSuccessful && response.body() != null) {
                    _profileState.value = ProfileState.Loaded(response.body()!!)
                } else {
                    _profileState.value = ProfileState.Error("Failed to load profile")
                }
            } catch (e: Exception) {
                _profileState.value = ProfileState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun logout() {
        authRepository.logout()
    }
}
