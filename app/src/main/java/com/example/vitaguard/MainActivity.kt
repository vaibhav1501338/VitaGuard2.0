package com.example.vitaguard

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.vitaguard.databinding.ActivityMainBinding
import com.google.android.gms.location.LocationServices

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val PERMISSION_REQUEST_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Request permissions immediately when the app starts
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
