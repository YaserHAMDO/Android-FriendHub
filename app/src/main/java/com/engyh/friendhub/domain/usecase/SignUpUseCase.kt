package com.engyh.friendhub.domain.usecase

import com.engyh.friendhub.domain.repository.AuthenticationRepository
import javax.inject.Inject

class SignUpUseCase @Inject constructor(
    private val authenticationRepository: AuthenticationRepository
) {

    suspend operator fun invoke(email: String, password: String) =
        authenticationRepository.signUp(email, password)

    suspend operator fun invoke(
        email: String,
        password: String,
        userName: String,
        about: String,
        birthdate: String,
        gender: String,
        location: Pair<Double, Double>,
        image: String
    ) = authenticationRepository.signUp2(email, password, userName, about, birthdate, gender, location, image)

    suspend operator fun invoke(
        userName: String,
        about: String,
        birthdate: String,
        gender: String,
        location: Pair<Double, Double>,
        image: String
    ) = authenticationRepository.signUp3(userName, about, birthdate, gender, location, image)

}