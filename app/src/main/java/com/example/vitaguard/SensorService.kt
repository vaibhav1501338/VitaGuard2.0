package com.example.vitaguard

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.sqrt
import android.os.Handler
import android.os.Binder
import java.util.Timer
import java.util.TimerTask

class SensorService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private lateinit var fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient
    private val db = FirebaseFirestore.getInstance()

    private val NOTIFICATION_ID = 1
    private val NOTIFICATION_CHANNEL_ID = "SensorServiceChannel"

    private val LOW_THRESHOLD = 30.0
    private val HIGH_THRESHOLD = 45.0
    private val COOLDOWN_TIME = 60000L

    private var isSosSent = false

    // Status tracking fields
    private var startTime: Long = 0
    private var currentGpsAccuracy: Float = 0f
    private val broadcastManager by lazy { LocalBroadcastManager.getInstance(this) }
    private val updateTimer = Timer()

    private val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
        .setMinUpdateIntervalMillis(2000L)
        .build()

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.lastLocation?.let { location ->
                currentGpsAccuracy = location.accuracy
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())

        // Start sensor and location updates
        accelerometer?.also { accel ->
            sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_NORMAL)
        }
        startLocationUpdates()

        // Start time and UI update timer
        startTime = System.currentTimeMillis()
        updateTimer.scheduleAtFixedRate(UpdateTask(), 0, 1000)

        Log.d("SensorService", "Service started and monitoring registered.")

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        stopLocationUpdates()
        updateTimer.cancel()
        Log.d("SensorService", "Service destroyed and monitoring stopped.")
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val acceleration = sqrt(x * x + y * y + z * z)

            if (!isSosSent) {
                if (acceleration >= HIGH_THRESHOLD) {
                    isSosSent = true
                    triggerSOS(true)

                } else if (acceleration >= LOW_THRESHOLD) {
                    isSosSent = true
                    triggerSOS(false)
                }
            }
        }
    }

    // UI Update Task (Runs every 1 second)
    private inner class UpdateTask : TimerTask() {
        override fun run() {
            val intent = Intent("MONITOR_UPDATE")

            // 1. Active Time
            val elapsedTime = System.currentTimeMillis() - startTime
            intent.putExtra("ACTIVE_TIME", elapsedTime)

            // 2. GPS Signal/Accuracy
            intent.putExtra("GPS_ACCURACY", currentGpsAccuracy)

            // 3. Battery Level
            val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            intent.putExtra("BATTERY_LEVEL", batteryLevel)

            broadcastManager.sendBroadcast(intent)
        }
    }

    // --- SOS and Binding Functions (Unchanged for this request) ---

    fun resetSosState() {
        isSosSent = false
        Handler(Looper.getMainLooper()).postDelayed({
            isSosSent = false
        }, COOLDOWN_TIME)
    }

    fun finishSosProcess(latitude: Double, longitude: Double) {
        val accident = Accident(
            id = System.currentTimeMillis(),
            latitude = latitude,
            longitude = longitude,
            timestamp = System.currentTimeMillis()
        )
        db.collection("accidents").add(accident)

        SOSManager.loadContacts(applicationContext)
        SOSManager.sendSOS(applicationContext, latitude, longitude)

        Handler(Looper.getMainLooper()).postDelayed({
            isSosSent = false
        }, COOLDOWN_TIME)
    }

    private fun triggerSOS(immediate: Boolean) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            isSosSent = false
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val latitude = location.latitude
                val longitude = location.longitude

                if (immediate) {
                    finishSosProcess(latitude, longitude)
                } else {
                    val alertIntent = Intent(this, AlertActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        putExtra("LATITUDE", latitude)
                        putExtra("LONGITUDE", longitude)
                    }
                    startActivity(alertIntent)
                }

            } else {
                isSosSent = false
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private val binder = LocalBinder()
    inner class LocalBinder : Binder() {
        fun getService(): SensorService = this@SensorService
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    private fun createNotification(): Notification {
        val channelId = NOTIFICATION_CHANNEL_ID
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Monitoring Service",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("VitaGuard Active")
            .setContentText("Monitoring for accidents in the background.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()
    }
}
