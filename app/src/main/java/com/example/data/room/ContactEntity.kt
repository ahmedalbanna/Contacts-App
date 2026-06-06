package com.example.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey val id: String,
    val name: String,
    val phoneNumbers: String, // Comma-separated list of phone numbers
    val emails: String,       // Comma-separated list of emails
    val photoUri: String?,
    val isFavorite: Boolean,
    val isLocal: Boolean,
    val notes: String?
)
