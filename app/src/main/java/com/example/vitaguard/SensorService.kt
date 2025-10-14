package com.example.vitaguard

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.sqrt
import android.os.Handler
import android.os.Binder

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

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())

        accelerometer?.also { accel ->
            sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_NORMAL)
        }

        Log.d("SensorService", "Service started and sensor listener registered.")

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        Log.d("SensorService", "Service destroyed and listener unregistered.")
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
                    Log.d("SensorService", "High severity crash detected. Sending immediate SOS.")
                    triggerSOS(true)

                } else if (acceleration >= LOW_THRESHOLD) {
                    isSosSent = true
                    Log.d("SensorService", "Low severity incident detected. Starting 5-second countdown.")
                    triggerSOS(false)
                }
            }
        }
    }

    fun resetSosState() {
        isSosSent = false
        Log.d("SensorService", "SOS state manually reset.")
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
            .addOnSuccessListener { Log.d("SensorService", "Accident saved to Firestore.") }

        SOSManager.loadContacts(applicationContext)
        SOSManager.sendSOS(applicationContext, latitude, longitude)

        Handler(Looper.getMainLooper()).postDelayed({
            isSosSent = false
        }, COOLDOWN_TIME)
    }


    private fun triggerSOS(immediate: Boolean) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e("SensorService", "Location permission not granted. Cannot send SOS.")
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
                Log.e("SensorService", "Could not get location. Resetting state.")
                isSosSent = false
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    // LOCAL BINDER CLASS: This definition is necessary to fix the error in AlertActivity.kt
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
