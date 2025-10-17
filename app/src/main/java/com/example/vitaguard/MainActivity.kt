package com.example.vitaguard

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.vitaguard.databinding.ActivityMainBinding
import com.google.android.gms.location.LocationServices
import android.view.ViewGroup
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import android.view.View // ADDED IMPORT
import java.util.concurrent.TimeUnit
import androidx.core.widget.NestedScrollView
import com.airbnb.lottie.LottieAnimationView

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val PERMISSION_REQUEST_CODE = 101

    // SharedPreferences key for saving theme state
    private val PREF_THEME_KEY = "is_light_theme"

    // Theme color definitions for manual background swap
    private val DARK_BG = Color.parseColor("#120D2F")
    private val LIGHT_BG = Color.parseColor("#FFFFFF")
    private val DARK_CARD = Color.parseColor("#211A3E")
    private val LIGHT_CARD = Color.parseColor("#F0F0F0")
    private val DARK_TEXT = Color.parseColor("#FFFFFF")
    private val LIGHT_TEXT = Color.parseColor("#121212")

    // Status color constants
    private val COLOR_MONITORING = Color.parseColor("#4CAF50") // Green for monitoring
    private val COLOR_STANDBY = Color.parseColor("#FF9800") // Amber for standby


    // Receiver to get data from SensorService
    private val monitorUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                // 1. Active Time
                val elapsedTime = it.getLongExtra("ACTIVE_TIME", 0)
                updateActiveTime(elapsedTime)

                // 2. GPS Signal/Accuracy
                val accuracy = it.getFloatExtra("GPS_ACCURACY", 0f)
                updateGpsSignal(accuracy)

                // 3. Battery Level
                val batteryLevel = it.getIntExtra("BATTERY_LEVEL", 0)
                updateBattery(batteryLevel)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. Load preference and Must call setTheme BEFORE super.onCreate()
        val isLight = getSharedPreferences(PREF_THEME_KEY, Context.MODE_PRIVATE).getBoolean(PREF_THEME_KEY, false)
        if (isLight) {
            setTheme(R.style.Theme_VitaGuard_Light)
        } else {
            setTheme(R.style.Theme_VitaGuard_Dark)
        }

        super.onCreate(savedInstanceState)

        try {
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Layout inflation failed: ${e.message}")
            Toast.makeText(this, "Error loading UI. Please check dependencies.", Toast.LENGTH_LONG).show()
            return
        }

        // Apply initial theme settings
        applyInitialTheme(isLight)

        // Initialize toolbar
        setSupportActionBar(binding.toolbar)

        // Set the toolbar title
        supportActionBar?.title = "VitaGuard Monitor"

        // Set initial state of switch based on loaded preference
        binding.themeSwitch.isChecked = isLight

        checkAndRequestPermissions()

        // --- BUTTON LISTENERS ---
        binding.startButton.setOnClickListener {
            SOSManager.loadContacts(this)
            startSensorService()
            binding.statusText.text = "MONITORING"
            binding.statusText.setTextColor(Color.parseColor("#4CAF50"))
            setAnimationState(true) // Start animation
        }

        binding.stopButton.setOnClickListener {
            stopService(Intent(this, SensorService::class.java))
            binding.statusText.text = "STANDBY"
            binding.statusText.setTextColor(Color.parseColor("#FF9800"))
            updateActiveTime(0) // Reset active time display
            updateGpsSignal(0f)
            setAnimationState(false) // Stop animation
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

        // Link Settings Card to the Emergency Contacts Activity
        binding.settingsCard.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // FAB Click Listener to toggle scroll position
        binding.fabMenu.setOnClickListener {
            val nestedScrollView = binding.root.getChildAt(1)
            if (nestedScrollView is NestedScrollView) {
                val totalHeight = nestedScrollView.getChildAt(0).height
                val visibleHeight = nestedScrollView.height
                val currentScroll = nestedScrollView.scrollY

                val isAtBottom = currentScroll >= (totalHeight - visibleHeight - 100)

                if (isAtBottom) {
                    nestedScrollView.smoothScrollTo(0, 0)
                } else {
                    nestedScrollView.smoothScrollTo(0, totalHeight)
                }
            }
        }

        // Theme Switch Listener: Saves new state and triggers screen refresh
        binding.themeSwitch.setOnCheckedChangeListener { _, isChecked ->
            saveThemeState(isChecked)
        }

        // Set initial animation state (STANDBY)
        setAnimationState(false)
    }

    private fun setAnimationState(isMonitoring: Boolean) {
        val animationView = binding.statusCard.findViewById<View>(R.id.statusAnimation) // statusAnimation is a View now
        if (isMonitoring) {
            animationView?.setBackgroundColor(COLOR_MONITORING)
        } else {
            animationView?.setBackgroundColor(COLOR_STANDBY)
        }
    }

    override fun onStart() {
        super.onStart()
        LocalBroadcastManager.getInstance(this).registerReceiver(
            monitorUpdateReceiver,
            IntentFilter("MONITOR_UPDATE")
        )
    }

    override fun onStop() {
        super.onStop()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(monitorUpdateReceiver)
    }

    // --- UI UPDATE METHODS ---

    private fun updateActiveTime(elapsedTime: Long) {
        val hours = TimeUnit.MILLISECONDS.toHours(elapsedTime)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(elapsedTime) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(elapsedTime) % 60

        binding.statsRow.findViewById<TextView>(R.id.activeTimeText)?.text =
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun updateGpsSignal(accuracy: Float) {
        val signalText = when {
            accuracy > 50f -> "Low"
            accuracy > 10f -> "Medium"
            accuracy > 0f -> "High"
            else -> "Searching"
        }
        binding.statsRow.findViewById<TextView>(R.id.gpsAccuracyText)?.text = signalText
    }

    private fun updateBattery(level: Int) {
        binding.statsRow.findViewById<TextView>(R.id.batteryText)?.text = "$level%"
    }

    // --- THEME LOGIC AND HELPER FUNCTIONS ---

    private fun applyInitialTheme(isLight: Boolean) {
        val targetTextColor = if (isLight) LIGHT_TEXT else DARK_TEXT

        binding.mainLayout.setBackgroundColor(if (isLight) LIGHT_BG else DARK_BG)

        // Adjust the switch label color based on the dark background
        binding.themeSwitch.setTextColor(if (isLight) LIGHT_TEXT else DARK_TEXT)
        binding.themeSwitch.text = if (isLight) "Dark Theme" else "Light Theme"

        // Manually set the toolbar text color after applying the theme
        binding.toolbar.setTitleTextColor(targetTextColor)

        // Manually update Quick Actions title color
        binding.root.findViewById<TextView>(R.id.quickActionsTitle)?.setTextColor(targetTextColor)
    }

    private fun saveThemeState(isLight: Boolean) {
        getSharedPreferences(PREF_THEME_KEY, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(PREF_THEME_KEY, isLight)
            .apply()

        recreate()
    }

    // --- PERMISSIONS AND SERVICE CONTROL ---

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
                Toast.makeText(this, "Getting current location...", Toast.LENGTH_SHORT).show()
                SOSManager.sendSOS(applicationContext, location.latitude, location.longitude)
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

        binding.statusText.text = "MONITORING"
    }
}
