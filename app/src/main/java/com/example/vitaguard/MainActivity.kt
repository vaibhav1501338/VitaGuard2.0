package com.example.vitaguard

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.vitaguard.databinding.ActivityMainBinding
import com.google.android.gms.location.LocationServices
import android.widget.TextView // ADDED import for TextView

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val PERMISSION_REQUEST_CODE = 101

    // Constants for theme colors
    private val DARK_BG = Color.parseColor("#1C1C24")
    private val LIGHT_BG = Color.parseColor("#F5F5F5")
    private val DARK_CARD = Color.parseColor("#2A2A38")
    private val LIGHT_CARD = Color.parseColor("#FFFFFF")
    private val DARK_TEXT = Color.parseColor("#BBBBBB")
    private val LIGHT_TEXT = Color.parseColor("#1C1C24")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Apply theme immediately (defaulting to dark)
        applyTheme(false)

        checkAndRequestPermissions()

        binding.startButton.setOnClickListener {
            SOSManager.loadContacts(this)
            startSensorService()
        }

        binding.stopButton.setOnClickListener {
            stopService(Intent(this, SensorService::class.java))
            binding.statusText.text = "Monitoring OFF"
        }

        binding.btnOpenMap.setOnClickListener {
            startActivity(Intent(this, MapActivity::class.java))
        }

        binding.btnAccidentHistory.setOnClickListener {
            startActivity(Intent(this, AccidentHistoryActivity::class.java))
        }

        binding.btnManualSos.setOnClickListener {
            SOSManager.loadContacts(this)
            sendManualSOS()
        }

        binding.btnManageContacts.setOnClickListener {
            startActivity(Intent(this, EmergencyContactsActivity::class.java))
        }

        // NEW: Theme Switch Listener
        binding.themeSwitch.setOnCheckedChangeListener { _, isChecked ->
            applyTheme(isChecked)
        }
    }

    // NEW: Theme application logic
    private fun applyTheme(isLight: Boolean) {
        // Change Main Background
        binding.mainLayout.setBackgroundColor(if (isLight) LIGHT_BG else DARK_BG)

        // Change Card Background
        binding.statusCard.setCardBackgroundColor(if (isLight) LIGHT_CARD else DARK_CARD)

        // Change Text Colors
        // FIX 1: Use binding.statusText to reference the status TextView directly
        binding.statusText.setTextColor(if (isLight) Color.parseColor("#C62828") else Color.parseColor("#FF9800"))

        // FIX 2: Correctly reference the TextView inside the LinearLayout within the CardView.
        // The original logic was incorrect and has been simplified by removing the duplicate CardView find.
        binding.themeSwitch.setTextColor(if (isLight) LIGHT_TEXT else DARK_TEXT)
        binding.themeSwitch.text = if (isLight) "Dark Theme" else "Light Theme"
    }

    private fun sendManualSOS() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location permission is required for SOS.", Toast.LENGTH_LONG).show()
            checkAndRequestPermissions()
            return
        }

        if (SOSManager.emergencyContacts.isEmpty()) {
            Toast.makeText(this, "Please add at least one emergency contact first.", Toast.LENGTH_LONG).show()
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                SOSManager.sendSOS(applicationContext, location.latitude, location.longitude)
                Toast.makeText(this, "Manual SOS triggered!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Could not get location. Cannot send SOS.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = arrayOf(
            Manifest.permission.SEND_SMS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CALL_PHONE
        )

        val permissionsNeeded = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toTypedArray(), PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, "Permissions granted. Ready to monitor.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Some permissions were denied. Monitoring requires location and SMS permissions.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun startSensorService() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location permission is required to start monitoring.", Toast.LENGTH_LONG).show()
            checkAndRequestPermissions()
            return
        }

        val serviceIntent = Intent(this, SensorService::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        binding.statusText.text = "Monitoring ON"
    }
}
