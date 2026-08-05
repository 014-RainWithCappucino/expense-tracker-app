package com.nijika21.yourmoney.ui.diagnostics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nijika21.yourmoney.data.repository.CaptureRepository
import com.nijika21.yourmoney.domain.capture.DiscoveredSource
import com.nijika21.yourmoney.domain.capture.SourceRegistry
import com.nijika21.yourmoney.domain.model.RawNotification
import com.nijika21.yourmoney.platform.notification.TxNotificationListenerService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DiagnosticsUiState(
    val listenerEnabled: Boolean = false,
    val capturedCount: Long = 0,
    val recent: List<RawNotification> = emptyList(),
    val discovered: List<DiscoveredSource> = emptyList(),
) {
    /** Candidate packages that have actually delivered something. */
    val confirmedCandidates: List<String>
        get() = SourceRegistry.candidates
            .map { it.packageName }
            .filter { pkg -> recent.any { it.packageName == pkg } }

    /** Candidates still silent — either wrong package name, or no spending yet. */
    val unconfirmedCandidates: List<String>
        get() = SourceRegistry.candidates
            .map { it.packageName }
            .filterNot { it in confirmedCandidates }

    /** Noisy non-whitelisted packages, most active first. Content never stored. */
    val discoveryLeaderboard: List<DiscoveredSource>
        get() = discovered.filterNot { it.isKnownCandidate }.take(30)
}

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    application: Application,
    private val captureRepository: CaptureRepository,
) : AndroidViewModel(application) {

    private val listenerEnabled = MutableStateFlow(
        TxNotificationListenerService.isEnabled(application),
    )

    val uiState: StateFlow<DiagnosticsUiState> = combine(
        listenerEnabled.asStateFlow(),
        captureRepository.observeCapturedCount(),
        captureRepository.observeRecent(),
        captureRepository.observeDiscoveredSources(),
    ) { enabled, count, recent, discovered ->
        DiagnosticsUiState(
            listenerEnabled = enabled,
            capturedCount = count,
            recent = recent,
            discovered = discovered,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DiagnosticsUiState(),
    )

    /**
     * Notification access is a Settings toggle, not a runtime permission, so
     * there is no result callback — the only way to learn it changed is to
     * re-read it when we come back to the foreground.
     */
    fun refreshListenerState() {
        listenerEnabled.value = TxNotificationListenerService.isEnabled(getApplication())
    }

    fun clearDiscovered() {
        viewModelScope.launch { captureRepository.clearDiscoveredSources() }
    }
}
