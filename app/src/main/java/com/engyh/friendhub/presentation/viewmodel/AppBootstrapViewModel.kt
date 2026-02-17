package com.engyh.friendhub.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.engyh.friendhub.data.cache.UserProfileStore
import com.engyh.friendhub.domain.usecase.BootstrapAppUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@HiltViewModel
class AppBootstrapViewModel @Inject constructor(
    private val profileStore: UserProfileStore,
    private val bootstrapAppUseCase: BootstrapAppUseCase
) : ViewModel() {

    sealed class BootstrapState {
        data object Loading : BootstrapState()
        data object LoggedOut : BootstrapState()
        data object Ready : BootstrapState()
    }

    private val _state = MutableStateFlow<BootstrapState>(BootstrapState.Loading)
    val state: StateFlow<BootstrapState> = _state.asStateFlow()

    private val started = AtomicBoolean(false)

    fun start() {
        if (!started.compareAndSet(false, true)) return

        viewModelScope.launch {
            _state.value = BootstrapState.Loading

            when (val result = bootstrapAppUseCase()) {
                BootstrapAppUseCase.Result.LoggedOut -> {
                    _state.value = BootstrapState.LoggedOut
                }
                is BootstrapAppUseCase.Result.Ready -> {
                    profileStore.refresh(result.idsToPrefetch)
                    _state.value = BootstrapState.Ready
                }
            }
        }
    }
}