package com.punitkumar.gruhkharch.domain.model

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val phoneNumber: String? = null
)
