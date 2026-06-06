package com.example.data

import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import android.util.Log

object ContactsFetcher {
    private const val TAG = "ContactsFetcher"

    fun hasReadContactsPermission(context: Context): Boolean {
        return context.checkSelfPermission(android.Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
    }

    fun fetchDeviceContacts(context: Context): List<Contact> {
        if (!hasReadContactsPermission(context)) {
            Log.e(TAG, "Permission READ_CONTACTS not granted.")
            return emptyList()
        }

        val contactsMap = mutableMapOf<String, MutableContact>()

        try {
            // 1. Fetch phone numbers and names
            val phoneProjection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.PHOTO_URI
            )

            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                phoneProjection,
                null,
                null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val photoIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)

                while (cursor.moveToNext()) {
                    val id = cursor.getString(idIdx) ?: continue
                    val name = cursor.getString(nameIdx) ?: "جهات اتصال بدون اسم"
                    val number = cursor.getString(numIdx) ?: continue
                    val photoUri = cursor.getString(photoIdx)

                    val mutContact = contactsMap.getOrPut(id) {
                        MutableContact(
                            id = id,
                            name = name,
                            phoneNumbers = mutableListOf(),
                            emails = mutableListOf(),
                            photoUri = photoUri
                        )
                    }
                    if (!mutContact.phoneNumbers.contains(number)) {
                        mutContact.phoneNumbers.add(number)
                    }
                }
            }

            // 2. Fetch email addresses
            val emailProjection = arrayOf(
                ContactsContract.CommonDataKinds.Email.CONTACT_ID,
                ContactsContract.CommonDataKinds.Email.ADDRESS
            )

            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                emailProjection,
                null,
                null,
                null
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.CONTACT_ID)
                val emailIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)

                while (cursor.moveToNext()) {
                    val id = cursor.getString(idIdx) ?: continue
                    val email = cursor.getString(emailIdx) ?: continue

                    contactsMap[id]?.let { mutContact ->
                        if (!mutContact.emails.contains(email)) {
                            mutContact.emails.add(email)
                        }
                    }
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error fetching device contacts", e)
        }

        return contactsMap.values.map { 
            Contact(
                id = it.id,
                name = it.name,
                phoneNumbers = it.phoneNumbers,
                emails = it.emails,
                photoUri = it.photoUri,
                isFavorite = false,
                isLocal = false,
                notes = null
            )
        }.sortedBy { it.name.lowercase() }
    }

    private data class MutableContact(
        val id: String,
        val name: String,
        val phoneNumbers: MutableList<String>,
        val emails: MutableList<String>,
        val photoUri: String?
    )
}
