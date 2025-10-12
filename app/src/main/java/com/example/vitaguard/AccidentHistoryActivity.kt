package com.example.vitaguard

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

    override fun onCreate(savedInstanceState: Bundle?) {
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
