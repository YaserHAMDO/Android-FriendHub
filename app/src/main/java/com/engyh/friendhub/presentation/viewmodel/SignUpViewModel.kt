package com.engyh.friendhub.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.engyh.friendhub.core.Response
import com.engyh.friendhub.core.UiState
import com.engyh.friendhub.data.model.RegistrationData
import com.engyh.friendhub.domain.usecase.LogoutUseCase
import com.engyh.friendhub.domain.usecase.SignUpUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val signUpUseCase: SignUpUseCase,
    private val logOutUseCase: LogoutUseCase
) : ViewModel() {

    private val _data = MutableStateFlow(RegistrationData())
    val data: StateFlow<RegistrationData> = _data.asStateFlow()

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun updateEmail(email: String) {
        _data.value = _data.value.copy(email = email.trim())
    }

    fun updatePassword(password: String) {
        _data.value = _data.value.copy(password = password)
    }

    fun updateUserName(userName: String) {
        _data.value = _data.value.copy(name = userName.trim())
    }

    fun updateGender(gender: String) {
        _data.value = _data.value.copy(gender = gender)
    }

    fun updateAbout(about: String) {
        _data.value = _data.value.copy(about = about.trim())
    }

    fun updateImage(uri: Uri) {
        _data.value = _data.value.copy(image = uri.toString())
    }

    fun updateBirthdate(year: Int, month: Int, day: Int) {
        val birthdate = String.format("%02d-%02d-%04d", day, month, year)
        _data.value = _data.value.copy(birthdate = birthdate)
    }

    fun updateLocation(latitude: Double, longitude: Double) {
        _data.value = _data.value.copy(location = Pair(latitude, longitude))
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun resetState() {
        _state.value = UiState()
    }

    fun logout() {
        viewModelScope.launch {
            logOutUseCase()
        }
    }
    fun signUpUser() {
        val current = _data.value

        if (current.name.isBlank()) {
            _state.value = UiState(error = "Name is required")
            return
        }
        if (current.gender.isBlank()) {
            _state.value = UiState(error = "Gender is required")
            return
        }
        if (current.birthdate.isBlank()) {
            _state.value = UiState(error = "Birthdate is required")
            return
        }
        val (lat, lon) = current.location
        if (lat == 0.0 && lon == 0.0) {
            _state.value = UiState(error = "Location is required")
            return
        }
        val image = current.image ?: run {
            _state.value = UiState(error = "Profile image is required")
            return
        }

        viewModelScope.launch {

            if (current.email.isBlank()) {
                signUpUseCase(
                    current.name,
                    current.about,
                    current.birthdate,
                    current.gender,
                    current.location,
                    image
                ).collect { resp ->
                    when (resp) {
                        is Response.Loading -> _state.value = UiState(loading = true)
                        is Response.Success -> _state.value = UiState(success = true)
                        is Response.Error -> _state.value = UiState(error = resp.message)
                    }
                }
            } else {
                signUpUseCase(
                    current.email,
                    current.password,
                    current.name,
                    current.about,
                    current.birthdate,
                    current.gender,
                    current.location,
                    image
                ).collect { resp ->
                    when (resp) {
                        is Response.Loading -> _state.value = UiState(loading = true)
                        is Response.Success -> _state.value = UiState(success = true)
                        is Response.Error -> _state.value = UiState(error = resp.message)
                    }
                }
            }
        }
    }
}