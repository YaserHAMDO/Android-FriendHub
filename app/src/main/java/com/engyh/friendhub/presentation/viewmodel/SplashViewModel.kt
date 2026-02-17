package com.engyh.friendhub.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.engyh.friendhub.domain.usecase.IsLoggedInUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val isLoggedInUseCase: IsLoggedInUseCase
) : ViewModel() {

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _startDestination = MutableStateFlow<Int?>(null)
    val startDestination: StateFlow<Int?> = _startDestination.asStateFlow()

    fun load() {
        if (_isReady.value) return
        viewModelScope.launch {
            val loggedIn = isLoggedInUseCase()
            _startDestination.value = if (loggedIn) {
                com.engyh.friendhub.R.id.mainFragment
            } else {
                com.engyh.friendhub.R.id.signInFragment
            }
            _isReady.value = true
        }
    }
}