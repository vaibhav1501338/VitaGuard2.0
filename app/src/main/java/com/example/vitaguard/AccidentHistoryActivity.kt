package com.example.vitaguard

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.vitaguard.databinding.ActivityAccidentHistoryBinding
import com.google.firebase.firestore.FirebaseFirestore

class AccidentHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAccidentHistoryBinding
    private val db = FirebaseFirestore.getInstance()
    private val accidentList = mutableListOf<Accident>()
    private lateinit var accidentAdapter: AccidentAdapter
    private val PREF_THEME_KEY = "is_light_theme" // Theme key constant

    override fun onCreate(savedInstanceState: Bundle?) {
        // CRITICAL: Load and apply theme state before super.onCreate()
        val isLight = getSharedPreferences(PREF_THEME_KEY, Context.MODE_PRIVATE).getBoolean(PREF_THEME_KEY, false)
        if (isLight) {
            setTheme(R.style.Theme_VitaGuard_Light)
        } else {
            setTheme(R.style.Theme_VitaGuard_Dark)
        }

        super.onCreate(savedInstanceState)
        binding = ActivityAccidentHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        accidentAdapter = AccidentAdapter(accidentList)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = accidentAdapter

        loadAccidentHistory()

        binding.btnClearHistory.setOnClickListener {
            deleteAccidentHistory()
        }
    }

    private fun loadAccidentHistory() {
        db.collection("accidents").get().addOnSuccessListener { result ->
            accidentList.clear()
            for (document in result) {
                // Assuming Accident data class has default constructors for Firebase
                val accident = document.toObject(Accident::class.java)
                accidentList.add(accident)
            }
            accidentAdapter.notifyDataSetChanged()
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to load history.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteAccidentHistory() {
        db.collection("accidents").get().addOnSuccessListener { result ->
            val batch = db.batch()
            for (document in result) {
                batch.delete(document.reference)
            }
            batch.commit().addOnSuccessListener {
                accidentList.clear()
                accidentAdapter.notifyDataSetChanged()
                Toast.makeText(this, "Accident history cleared.", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener {
                Toast.makeText(this, "Failed to clear history.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
