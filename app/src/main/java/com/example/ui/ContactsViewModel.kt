package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.Contact
import com.example.data.ContactRepository
import com.example.data.room.ContactDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ContactsViewModel(
    application: Application,
    private val repository: ContactRepository
) : AndroidViewModel(application) {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedTab = MutableStateFlow(0) // 0: الكل (All), 1: المفضلة (Favorites)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // Combined Flow: automatically joins contact list changes + search queries + active tab filters
    val contactsState: StateFlow<List<Contact>> = combine(
        repository.getUnifiedContactsFlow(),
        _searchQuery,
        _selectedTab
    ) { allContacts, query, tab ->
        var list = allContacts
        
        // 1. Filter Favorites tab
        if (tab == 1) {
            list = list.filter { it.isFavorite }
        }

        // 2. Filter Search Queries (by Name, Number or Email)
        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            list = list.filter { contact ->
                contact.name.lowercase().contains(q) ||
                contact.phoneNumbers.any { it.contains(q) } ||
                contact.emails.any { it.lowercase().contains(q) }
            }
        }
        
        list
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedTab(tabIndex: Int) {
        _selectedTab.value = tabIndex
    }

    fun toggleFavorite(contact: Contact) {
        viewModelScope.launch {
            repository.setFavStatus(contact.id, !contact.isFavorite, contact)
        }
    }

    fun addOrUpdateContact(contact: Contact) {
        viewModelScope.launch {
            repository.saveContact(contact)
        }
    }

    fun deleteContact(contact: Contact) {
        viewModelScope.launch {
            repository.deleteContact(contact)
        }
    }

    // Factory Class for standard ViewModel dependency injection
    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ContactsViewModel::class.java)) {
                val db = ContactDatabase.getDatabase(application)
                val repo = ContactRepository(application, db.contactDao())
                return ContactsViewModel(application, repo) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
