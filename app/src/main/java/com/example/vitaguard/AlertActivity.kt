package com.example.vitaguard

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.CountDownTimer
import android.os.IBinder
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.vitaguard.databinding.ActivityAlertBinding

class AlertActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlertBinding
    private var countDownTimer: CountDownTimer? = null
    private val COUNTDOWN_MILLIS = 5000L // 5 seconds

    private var sensorService: SensorService? = null
    private var isBound = false

    private var latitude: Double = 0.0
    private var longitude: Double = 0.0

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            // This line requires LocalBinder to be defined in SensorService
            val binder = service as SensorService.LocalBinder
            sensorService = binder.getService()
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
            sensorService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        binding = ActivityAlertBinding.inflate(layoutInflater)
        setContentView(binding.root)

        latitude = intent.getDoubleExtra("LATITUDE", 0.0)
        longitude = intent.getDoubleExtra("LONGITUDE", 0.0)

        bindService(Intent(this, SensorService::class.java), serviceConnection, Context.BIND_AUTO_CREATE)

        setupUI()
        startCountdown()
    }

    private fun setupUI() {
        binding.cancelButton.setOnClickListener {
            cancelSos()
        }
    }

    private fun startCountdown() {
        countDownTimer = object : CountDownTimer(COUNTDOWN_MILLIS, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                binding.timerText.text = "Auto-send in: $seconds seconds"
            }

            override fun onFinish() {
                sendSosFinal()
            }
        }.start()
    }

    private fun cancelSos() {
        countDownTimer?.cancel()

        if (isBound) {
            sensorService?.resetSosState()
        }

        Toast.makeText(this, "SOS Cancelled!", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun sendSosFinal() {
        if (isBound) {
            sensorService?.finishSosProcess(latitude, longitude)
        }

        Toast.makeText(this, "Low severity SOS Sent.", Toast.LENGTH_LONG).show()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        if (isBound) {
            unbindService(serviceConnection)
        }
    }
}
