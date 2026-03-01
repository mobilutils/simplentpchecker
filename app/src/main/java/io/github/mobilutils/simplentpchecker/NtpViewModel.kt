package io.github.mobilutils.simplentpchecker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Represents the complete UI state for the NTP checker screen.
 */
data class NtpUiState(
    /** Text field contents. */
    val serverAddress: String = "pool.ntp.org",
    /** Whether a network request is currently in progress. */
    val isLoading: Boolean = false,
    /** The result of the last check, or null if no check has been run yet. */
    val result: NtpResult? = null,
)

/**
 * ViewModel for the NTP checker.
 *
 * Survives configuration changes (rotation, etc.) and automatically cancels
 * any in-flight coroutine when [onCleared] is called.
 */
class SimpleNtpViewModel(
    private val repository: NtpRepository = NtpRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(NtpUiState())
    val uiState: StateFlow<NtpUiState> = _uiState.asStateFlow()

    /** Reference to the currently running check job so we can cancel it. */
    private var checkJob: Job? = null

    /** Called whenever the user edits the server address text field. */
    fun onServerAddressChange(newValue: String) {
        _uiState.value = _uiState.value.copy(serverAddress = newValue, result = null)
    }

    /**
     * Starts an NTP reachability check against the current [NtpUiState.serverAddress].
     *
     * If a check is already running it is cancelled before the new one starts.
     */
    fun checkReachability() {
        val host = _uiState.value.serverAddress.trim()
        if (host.isEmpty()) return

        // Cancel any previous in-flight request.
        checkJob?.cancel()

        _uiState.value = _uiState.value.copy(isLoading = true, result = null)

        checkJob = viewModelScope.launch {
            val result = repository.query(host)
            _uiState.value = _uiState.value.copy(isLoading = false, result = result)
        }
    }
}
