package com.engyh.friendhub.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.engyh.friendhub.core.Response
import com.engyh.friendhub.domain.model.User
import com.engyh.friendhub.domain.usecase.GetUserUseCase
import com.engyh.friendhub.domain.usecase.UpdateUserUseCase
import com.engyh.friendhub.domain.usecase.UploadImageUseCase
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditViewModel @Inject constructor(
    private val getUserUseCase: GetUserUseCase,
    private val updateUserUseCase: UpdateUserUseCase,
    private val uploadImageUseCase: UploadImageUseCase,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user

    private val _updateState = MutableStateFlow<Response<Boolean>?>(null)
    val updateState: StateFlow<Response<Boolean>?> = _updateState

    init {
        getUser()
    }

    private fun getUser() {
        viewModelScope.launch {
            auth.currentUser?.uid?.let {
                getUserUseCase(it).collect {
                    _user.value = it
                }
            }
        }
    }

    fun updateUser(name: String, about: String, imageUri: Uri?, gender: String, birthdate: String) {
        viewModelScope.launch {
            _updateState.value = Response.Loading
            val currentUser = _user.value
            if (currentUser != null) {
                if (imageUri != null) {
                    val userId = currentUser.userId
                    uploadImageUseCase(imageUri.toString(),  "profile_images/$userId").collect { response ->
                        when (response) {
                            is Response.Success -> {
                                val updatedUser = currentUser.copy(name = name, about = about, imageUrl = response.data, gender = gender, birthdate = birthdate)
                                _updateState.value = updateUserUseCase(updatedUser)
                            }
                            is Response.Error -> {
                                _updateState.value = response
                            }
                            is Response.Loading -> {}
                        }
                    }
                } else {
                    val updatedUser = currentUser.copy(name = name, about = about, gender = gender, birthdate = birthdate)
                    _updateState.value = updateUserUseCase(updatedUser)
                }
            }
        }
    }
}
