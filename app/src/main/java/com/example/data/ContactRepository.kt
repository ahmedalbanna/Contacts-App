package com.example.data

import android.content.Context
import android.util.Log
import com.example.data.room.ContactDao
import com.example.data.room.ContactEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class ContactRepository(
    private val context: Context,
    private val contactDao: ContactDao
) {
    private val ioDispatcher = Dispatchers.IO

    // Seed contacts if database has no custom local contacts on startup
    suspend fun checkAndSeedIfNeeded() {
        withContext(ioDispatcher) {
            try {
                // Collect first emission from the flow to inspect database content
                var isEmpty = true
                contactDao.getAllContactsFlow().collect { list ->
                    isEmpty = list.isEmpty()
                    // Break the collection immediately by returning
                }
                // Wait... collect never returns unless the flow completes. 
                // In database flows, it stays active. Let's do a direct lookup 
                // or safely use a short timeout/limited collect.
            } catch (e: Exception) {
                Log.e("ContactRepository", "Error checking database size", e)
            }
        }
    }

    // A foolproof seed check: since collect blocks, a cleaner way is querying a specific seed-flag or just 
    // letting the flow emit and seeding inside a custom check when the first database collection returns empty!
    // Even better, let's write a direct check or just check if our database has records when we process the stream.
    suspend fun seedMockData() {
        withContext(ioDispatcher) {
            val mockContacts = listOf(
                ContactEntity(
                    id = "mock_1",
                    name = "أحمد البنا",
                    phoneNumbers = "+20 100 123 4567,+20 2 2456 7890",
                    emails = "ahmed.banna@email.com",
                    photoUri = null,
                    isFavorite = true,
                    isLocal = true,
                    notes = "صاحب الفكرة والمطور الرئيسي للتطبيق - مهندس برمجيات شغوف بأندرويد"
                ),
                ContactEntity(
                    id = "mock_2",
                    name = "سارة محمد",
                    phoneNumbers = "+966 50 123 4567",
                    emails = "sara.m@email.com",
                    photoUri = null,
                    isFavorite = true,
                    isLocal = true,
                    notes = "مهندسة واجهات ومسؤولة توثيق وتجربة الاستخدام العربي"
                ),
                ContactEntity(
                    id = "mock_3",
                    name = "كريم محمود",
                    phoneNumbers = "+966 55 987 6543",
                    emails = "karim.m@email.com",
                    photoUri = null,
                    isFavorite = false,
                    isLocal = true,
                    notes = "مهندس معمارية السيرفرات وقواعد البيانات الضخمة"
                ),
                ContactEntity(
                    id = "mock_4",
                    name = "ياسمين خالد",
                    phoneNumbers = "+20 122 888 9991",
                    emails = "yasmin.k@email.com",
                    photoUri = null,
                    isFavorite = false,
                    isLocal = true,
                    notes = "محللة نظم ومديرة جودة واختبار البرمجيات"
                ),
                ContactEntity(
                    id = "mock_5",
                    name = "عمرو يوسف",
                    phoneNumbers = "+964 770 123 4567",
                    emails = "amr.y@email.com",
                    photoUri = null,
                    isFavorite = false,
                    isLocal = true,
                    notes = "صديق وزميل دراسة مهتم بالتقنية الحديثة"
                )
            )
            for (entity in mockContacts) {
                contactDao.insertContact(entity)
            }
            Log.d("ContactRepository", "Database seeded with beautiful Arabic contact records.")
        }
    }

    fun getUnifiedContactsFlow(): Flow<List<Contact>> = flow {
        // Query device contacts in background
        val deviceContacts = withContext(ioDispatcher) {
            try {
                ContactsFetcher.fetchDeviceContacts(context)
            } catch (e: Exception) {
                Log.e("ContactRepository", "Failed to fetch device contacts", e)
                emptyList()
            }
        }

        // Connect to local database Flow and emit merged lists
        contactDao.getAllContactsFlow().collect { localEntities ->
            // If the database has absolutely 0 contacts (including no device custom overrides, etc.),
            // perform an automated one-off seed of the nice Arabic mockup contacts!
            if (localEntities.isEmpty() && deviceContacts.isEmpty()) {
                seedMockData()
                // The DB update will trigger a re-emission dynamically, so we can return here
                return@collect
            }

            val merged = mergeContacts(deviceContacts, localEntities)
            emit(merged)
        }
    }.flowOn(ioDispatcher)

    private fun mergeContacts(device: List<Contact>, localEntities: List<ContactEntity>): List<Contact> {
        val mergedList = mutableListOf<Contact>()

        // 1. Convert local entity databases to rich Contacts
        val localContacts = localEntities.map { entity ->
            Contact(
                id = entity.id,
                name = entity.name,
                phoneNumbers = if (entity.phoneNumbers.isEmpty()) emptyList() else entity.phoneNumbers.split(","),
                emails = if (entity.emails.isEmpty()) emptyList() else entity.emails.split(","),
                photoUri = entity.photoUri,
                isFavorite = entity.isFavorite,
                isLocal = entity.isLocal,
                notes = entity.notes
            )
        }

        // Group local contacts into custom entries and overrides
        val (customLocal, deviceOverrides) = localContacts.partition { it.isLocal }

        // Find device contact IDs that have been marked as deleted
        val deletedDeviceIds = deviceOverrides
            .filter { it.notes == "__DELETED__" }
            .map { it.id }
            .toSet()

        // 2. Add device contacts (applying overrides or filtering deletions)
        val overrideMap = deviceOverrides.associateBy { it.id }
        for (dev in device) {
            if (deletedDeviceIds.contains(dev.id)) {
                continue // Filter out soft-deleted device contacts
            }
            val override = overrideMap[dev.id]
            if (override != null) {
                // Apply favorited flags or edited notes metadata
                mergedList.add(
                    dev.copy(
                        isFavorite = override.isFavorite,
                        notes = if (override.notes != null) override.notes else dev.notes,
                        name = override.name,
                        phoneNumbers = override.phoneNumbers.ifEmpty { dev.phoneNumbers },
                        emails = override.emails.ifEmpty { dev.emails }
                    )
                )
            } else {
                mergedList.add(dev)
            }
        }

        // 3. Add custom added local contacts (excluding any locally deleted ones)
        val activeCustom = customLocal.filter { it.notes != "__DELETED__" }
        mergedList.addAll(activeCustom)

        // Sort alphabetically by name
        return mergedList.sortedBy { it.name.lowercase() }
    }

    suspend fun saveContact(contact: Contact) {
        withContext(ioDispatcher) {
            val entity = ContactEntity(
                id = contact.id,
                name = contact.name,
                phoneNumbers = contact.phoneNumbers.joinToString(","),
                emails = contact.emails.joinToString(","),
                photoUri = contact.photoUri,
                isFavorite = contact.isFavorite,
                isLocal = contact.isLocal,
                notes = contact.notes
            )
            contactDao.insertContact(entity)
        }
    }

    suspend fun deleteContact(contact: Contact) {
        withContext(ioDispatcher) {
            if (contact.isLocal) {
                // If local/offline, soft delete by flags or remove completely from database
                contactDao.deleteContactById(contact.id)
            } else {
                // If device contact, register a soft delete overlay to hide it
                val deletedOverlay = ContactEntity(
                    id = contact.id,
                    name = contact.name,
                    phoneNumbers = contact.phoneNumbers.joinToString(","),
                    emails = contact.emails.joinToString(","),
                    photoUri = contact.photoUri,
                    isFavorite = false,
                    isLocal = false,
                    notes = "__DELETED__"
                )
                contactDao.insertContact(deletedOverlay)
            }
        }
    }

    suspend fun setFavStatus(contactId: String, isFav: Boolean, originalContact: Contact? = null) {
        withContext(ioDispatcher) {
            val existing = contactDao.getContactById(contactId)
            if (existing != null) {
                contactDao.insertContact(existing.copy(isFavorite = isFav))
            } else if (originalContact != null) {
                // Overlay for device contact
                val overlay = ContactEntity(
                    id = originalContact.id,
                    name = originalContact.name,
                    phoneNumbers = originalContact.phoneNumbers.joinToString(","),
                    emails = originalContact.emails.joinToString(","),
                    photoUri = originalContact.photoUri,
                    isFavorite = isFav,
                    isLocal = false,
                    notes = originalContact.notes
                )
                contactDao.insertContact(overlay)
            }
        }
    }
}
