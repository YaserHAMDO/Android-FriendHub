package com.engyh.friendhub.domain.model

data class User(
    val userId: String = "",
    val email: String = "",
    val fcm: String = "",
    val name: String = "",
    val uniqueId: String = "",
    val about: String = "",
    val birthdate: String = "",
    val gender: String = "",
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val imageUrl: String = "",
    val imageName: String = ""
)