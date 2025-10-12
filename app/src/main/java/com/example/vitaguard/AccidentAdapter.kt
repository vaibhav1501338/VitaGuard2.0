package com.example.vitaguard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class AccidentAdapter(private val accidents: List<Accident>) : RecyclerView.Adapter<AccidentAdapter.AccidentViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccidentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_accident, parent, false)
        return AccidentViewHolder(view)
    }

    override fun onBindViewHolder(holder: AccidentViewHolder, position: Int) {
        val accident = accidents[position]
        holder.bind(accident)
    }

    override fun getItemCount(): Int {
        return accidents.size
    }

    class AccidentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val timestampTextView: TextView = itemView.findViewById(R.id.timestampTextView)
        private val locationTextView: TextView = itemView.findViewById(R.id.locationTextView)

        fun bind(accident: Accident) {
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
            val formattedDate = sdf.format(Date(accident.timestamp))
            timestampTextView.text = formattedDate
            locationTextView.text = "Location: ${accident.latitude}, ${accident.longitude}"
        }
    }
}
