package com.engyh.friendhub.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.engyh.friendhub.domain.usecase.SetOfflineUseCase
import com.engyh.friendhub.domain.usecase.SetOnlineUseCase
import com.engyh.friendhub.domain.usecase.StartPresenceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PresenceViewModel @Inject constructor(
    private val startPresence: StartPresenceUseCase,
    private val setOnline: SetOnlineUseCase,
    private val setOffline: SetOfflineUseCase
) : ViewModel() {

    fun start() = viewModelScope.launch { startPresence() }
    fun onAppForeground() = viewModelScope.launch { setOnline() }
    fun onAppBackground() = viewModelScope.launch { setOffline() }
}
