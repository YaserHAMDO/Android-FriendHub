package com.engyh.friendhub.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.engyh.friendhub.core.Response
import com.engyh.friendhub.domain.repository.AuthenticationRepository
import com.engyh.friendhub.domain.usecase.SetOfflineUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: AuthenticationRepository,
    private val setOffline: SetOfflineUseCase
) : ViewModel() {

    private val _deleteState = MutableStateFlow<Response<Void?>?>(null)
    val deleteState: StateFlow<Response<Void?>?> = _deleteState.asStateFlow()

    fun logout() {
        viewModelScope.launch {
            setOffline()
            repository.logout()
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            repository.deleteAccount().collect { resp ->
                setOffline()
                _deleteState.value = resp
            }
        }
    }

    fun resetDeleteState() {
        _deleteState.value = null
    }
}
