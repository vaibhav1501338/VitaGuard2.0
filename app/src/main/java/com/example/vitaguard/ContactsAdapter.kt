package com.example.vitaguard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ContactsAdapter(
    private val contacts: List<String>,
    private val onDeleteClicked: (Int) -> Unit
) : RecyclerView.Adapter<ContactsAdapter.ContactViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        // Ensure you have generated item_contact.xml
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_contact, parent, false)
        return ContactViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val number = contacts[position]
        holder.bind(number, position, onDeleteClicked)
    }

    override fun getItemCount(): Int {
        return contacts.size
    }

    class ContactViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val numberTextView: TextView = itemView.findViewById(R.id.contactNumberTextView)
        private val deleteButton: Button = itemView.findViewById(R.id.deleteButton)

        fun bind(number: String, position: Int, onDeleteClicked: (Int) -> Unit) {
            numberTextView.text = number

            deleteButton.setOnClickListener {
                onDeleteClicked(position)
            }
        }
    }
}
