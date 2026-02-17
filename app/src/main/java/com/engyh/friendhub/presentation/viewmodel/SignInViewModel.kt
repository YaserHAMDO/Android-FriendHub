package com.engyh.friendhub.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.engyh.friendhub.core.Response
import com.engyh.friendhub.domain.repository.AuthenticationRepository
import com.engyh.friendhub.domain.usecase.CheckIfUserExistsUseCase
import com.google.firebase.auth.AuthCredential
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val repository: AuthenticationRepository,
    private val checkIfUserExistsUseCase: CheckIfUserExistsUseCase
) : ViewModel() {

    private val _signInState = MutableStateFlow<Response<Boolean>?>(null)
    val signInState: StateFlow<Response<Boolean>?> = _signInState

    fun consumeSignInState() {
        _signInState.value = null
    }

    fun signInWithCredential(credential: AuthCredential) {
        viewModelScope.launch {
            repository.signInWithCredential(credential).collect { resp ->
                when (resp) {
                    is Response.Loading -> _signInState.value = Response.Loading

                    is Response.Success -> {
                        val uid = resp.data.user?.uid
                        if (uid.isNullOrBlank()) {
                            _signInState.value = Response.Error("User ID is null")
                            return@collect
                        }
                        checkUserExists(uid)
                    }

                    is Response.Error -> _signInState.value = Response.Error(resp.message)
                }
            }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            repository.signIn(email, password).collect { resp ->
                when (resp) {
                    is Response.Loading -> _signInState.value = Response.Loading

                    is Response.Success -> {
                        val uid = resp.data.user?.uid
                        if (uid.isNullOrBlank()) {
                            _signInState.value = Response.Error("User ID is null")
                            return@collect
                        }
                        checkUserExists(uid)
                    }

                    is Response.Error -> _signInState.value = Response.Error(resp.message)
                }
            }
        }
    }

    private fun checkUserExists(uid: String) {
        viewModelScope.launch {
            try {
                _signInState.value = Response.Loading
                val exists = checkIfUserExistsUseCase(uid)
                _signInState.value = Response.Success(exists)
            } catch (e: Exception) {
                _signInState.value =
                    Response.Error(e.localizedMessage ?: "Failed to check user profile")
            }
        }
    }
}