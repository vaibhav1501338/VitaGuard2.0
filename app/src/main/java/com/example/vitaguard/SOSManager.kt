package com.example.vitaguard

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.telephony.SmsManager
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object SOSManager {

    private const val PREF_NAME = "EmergencyPrefs"
    private const val KEY_CONTACTS = "emergency_contacts_list"

    var emergencyContacts: List<String> = emptyList()

    fun loadContacts(context: Context) {
        val sharedPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = sharedPrefs.getString(KEY_CONTACTS, "[]")

        val type = object : TypeToken<List<String>>() {}.type
        emergencyContacts = Gson().fromJson(json, type) ?: emptyList()
    }

    fun saveContacts(context: Context, contacts: List<String>) {
        val sharedPrefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = Gson().toJson(contacts.distinct())
        with(sharedPrefs.edit()) {
            putString(KEY_CONTACTS, json)
            apply()
        }
        emergencyContacts = contacts.distinct()
    }

    fun sendSOS(context: Context, latitude: Double, longitude: Double) {
        if (emergencyContacts.isEmpty()) {
            Toast.makeText(context, "No emergency contacts set. SOS failed.", Toast.LENGTH_LONG).show()
            return
        }

        val locationLink = "http://maps.google.com/maps?q=$latitude,$longitude"
        val message = "Emergency! I may have been in an accident. My last known location is: $locationLink"

        // Send SMS to ALL contacts
        for (number in emergencyContacts) {
            sendSms(context, number, message)
        }

        // Call only the first contact
        emergencyContacts.firstOrNull()?.let { firstContact ->
            makePhoneCall(context, firstContact)
        }
    }

    private fun sendSms(context: Context, number: String, message: String) {
        try {
            if (number.length < 8) return

            val smsManager = context.getSystemService(SmsManager::class.java)
            smsManager.sendTextMessage(number, null, message, null, null)
            Toast.makeText(context, "SOS SMS sent to $number.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to send SMS to $number. Check permissions.", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun makePhoneCall(context: Context, number: String) {
        try {
            val callIntent = Intent(Intent.ACTION_CALL)
            callIntent.data = Uri.parse("tel:$number")
            callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(callIntent)
            Toast.makeText(context, "Calling $number...", Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            Toast.makeText(context, "Failed to make call. Check permissions.", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }
}
