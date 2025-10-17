package com.example.vitaguard

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.vitaguard.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val PREF_THEME_KEY = "is_light_theme"

    override fun onCreate(savedInstanceState: Bundle?) {
        // CRITICAL: Load and apply theme state before super.onCreate()
        val isLight = getSharedPreferences(PREF_THEME_KEY, Context.MODE_PRIVATE).getBoolean(PREF_THEME_KEY, false)
        if (isLight) {
            setTheme(R.style.Theme_VitaGuard_Light)
        } else {
            setTheme(R.style.Theme_VitaGuard_Dark)
        }

        super.onCreate(savedInstanceState)

        try {
            binding = ActivitySettingsBinding.inflate(layoutInflater)
            setContentView(binding.root)
        } catch (e: Exception) {
            android.util.Log.e("SettingsActivity", "Layout inflation failed: ${e.message}")
            Toast.makeText(this, "Error loading Settings UI.", Toast.LENGTH_LONG).show()
            return
        }

        supportActionBar?.title = "App Settings"

        // 1. Initial State: Set the switch state
        binding.themeSwitch.isChecked = isLight

        // 2. Theme Toggle Listener
        binding.themeSwitch.setOnCheckedChangeListener { _, isChecked ->
            saveThemeState(isChecked)
        }
    }

    private fun saveThemeState(isLight: Boolean) {
        // Save the new preference (true = Light, false = Dark)
        getSharedPreferences(PREF_THEME_KEY, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(PREF_THEME_KEY, isLight)
            .apply()

        Toast.makeText(this, "Theme changed. Restarting...", Toast.LENGTH_SHORT).show()
        // Recreate the activity to apply the new theme immediately
        recreate()
    }
}
