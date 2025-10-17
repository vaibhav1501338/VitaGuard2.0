package com.example.vitaguard

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.vitaguard.databinding.ActivityEmergencyContactsBinding
import android.Manifest
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class EmergencyContactsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEmergencyContactsBinding
    private lateinit var adapter: ContactsAdapter
    private val contactsList = mutableListOf<String>()
    private val PREF_THEME_KEY = "is_light_theme"

    private val PICK_CONTACT_REQUEST = 1
    private val READ_CONTACTS_PERMISSION_CODE = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. CRITICAL: Load and apply theme state before super.onCreate()
        val isLight = getSharedPreferences(PREF_THEME_KEY, Context.MODE_PRIVATE).getBoolean(PREF_THEME_KEY, false)
        if (isLight) {
            setTheme(R.style.Theme_VitaGuard_Light)
        } else {
            setTheme(R.style.Theme_VitaGuard_Dark)
        }

        super.onCreate(savedInstanceState)

        // 2. Safely inflate and set content view
        try {
            binding = ActivityEmergencyContactsBinding.inflate(layoutInflater)
            setContentView(binding.root)
        } catch (e: Exception) {
            android.util.Log.e("ContactsActivity", "Layout inflation failed: ${e.message}")
            Toast.makeText(this, "Error loading Contacts UI. Please check dependencies.", Toast.LENGTH_LONG).show()
            // Returning home if the activity fails to load its layout
            finish()
            return
        }

        supportActionBar?.title = "Emergency Contacts" // Set toolbar title

        loadContacts()

        adapter = ContactsAdapter(contactsList) { position ->
            removeContact(position)
        }
        binding.contactsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.contactsRecyclerView.adapter = adapter

        binding.addButton.setOnClickListener {
            addContact()
        }

        // 3. Pick contact listener - This button launches the permission check/picker
        binding.btnPickContact.setOnClickListener {
            checkContactPermissions()
        }
    }

    private fun checkContactPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_CONTACTS), READ_CONTACTS_PERMISSION_CODE)
        } else {
            openContactPicker()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == READ_CONTACTS_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openContactPicker()
            } else {
                Toast.makeText(this, "Permission denied. Cannot read contacts.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openContactPicker() {
        // Opens the system contact picker focused on phone numbers
        val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
        startActivityForResult(intent, PICK_CONTACT_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_CONTACT_REQUEST && resultCode == Activity.RESULT_OK) {
            val contactUri: Uri? = data?.data
            val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER)

            contactUri?.let { uri ->
                contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        val phoneNumber = cursor.getString(numberIndex)

                        // Clean up the phone number (remove spaces, dashes, parentheses, etc.)
                        val cleanedNumber = phoneNumber.replace(Regex("[^0-9]"), "")

                        if (cleanedNumber.length >= 8) {
                            addPickedContact(cleanedNumber)
                        } else {
                            Toast.makeText(this, "Invalid number selected.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun addPickedContact(number: String) {
        if (contactsList.contains(number)) {
            Toast.makeText(this, "Contact already exists.", Toast.LENGTH_SHORT).show()
            return
        }

        contactsList.add(number)
        saveContacts()
        Toast.makeText(this, "Added $number to emergency contacts.", Toast.LENGTH_SHORT).show()
    }


    private fun loadContacts() {
        SOSManager.loadContacts(this)
        contactsList.clear()
        contactsList.addAll(SOSManager.emergencyContacts)
    }

    private fun addContact() {
        // Clean up the phone number (remove spaces, dashes, etc.)
        val number = binding.contactInput.text.toString().trim().replace(Regex("[^0-9]"), "")
        if (number.length < 8) {
            Toast.makeText(this, "Please enter a valid phone number.", Toast.LENGTH_SHORT).show()
            return
        }

        if (contactsList.contains(number)) {
            Toast.makeText(this, "Contact already exists.", Toast.LENGTH_SHORT).show()
            return
        }

        contactsList.add(number)
        saveContacts()
        binding.contactInput.text.clear()
    }

    private fun removeContact(position: Int) {
        contactsList.removeAt(position)
        saveContacts()
    }

    private fun saveContacts() {
        SOSManager.saveContacts(this, contactsList)
        adapter.notifyDataSetChanged()
    }
}
