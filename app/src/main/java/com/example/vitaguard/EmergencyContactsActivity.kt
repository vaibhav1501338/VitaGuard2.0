package com.example.vitaguard

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.vitaguard.databinding.ActivityEmergencyContactsBinding

class EmergencyContactsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEmergencyContactsBinding
    private lateinit var adapter: ContactsAdapter
    private val contactsList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Ensure you have generated activity_emergency_contacts.xml
        binding = ActivityEmergencyContactsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadContacts()

        // Setup adapter with a delete callback
        adapter = ContactsAdapter(contactsList) { position ->
            removeContact(position)
        }
        binding.contactsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.contactsRecyclerView.adapter = adapter

        binding.addButton.setOnClickListener {
            addContact()
        }
    }

    private fun loadContacts() {
        SOSManager.loadContacts(this)
        contactsList.clear()
        contactsList.addAll(SOSManager.emergencyContacts)
    }

    private fun addContact() {
        val number = binding.contactInput.text.toString().trim()
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
        Toast.makeText(this, "Contacts updated.", Toast.LENGTH_SHORT).show()
    }
}
