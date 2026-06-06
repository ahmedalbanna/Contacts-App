package com.example.data

data class Contact(
    val id: String,
    val name: String,
    val phoneNumbers: List<String>,
    val emails: List<String>,
    val photoUri: String? = null,
    val isFavorite: Boolean = false,
    val isLocal: Boolean = false,
    val notes: String? = null
)
