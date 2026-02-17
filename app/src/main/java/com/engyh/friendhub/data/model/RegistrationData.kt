package com.engyh.friendhub.data.model

data class RegistrationData(
    val email: String = "",
    val password: String = "",
    val name: String = "",
    val gender: String = "",
    val birthdate: String = "",
    val about: String = "",
    val image: String? = null,
    val location: Pair<Double, Double> = Pair(0.0, 0.0)
)