package com.es.multivs.presentation.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.es.multivs.R
import com.es.multivs.data.database.entities.MedicationData


/**
 * Created by Marko on 12/15/2021.
 * Etrog Systems LTD.
 */
class MedicationsAdapter(
    private val medications: MutableList<MedicationData>,
    private val listener: MedicationAdapterListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    fun interface MedicationAdapterListener {
        fun onTaken(medication: MedicationData, taken: Boolean)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val v: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.medication_recycler_row, parent, false)
        return RowViewHolder(v)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = medications[position]
        if (holder is RowViewHolder){
            holder.drugName.text = item.medicationName
            holder.strength.text = item.strength
            holder.dosage.text = item.dosageForm
            holder.quantity.text = item.dosageQuantity
        }
    }

    override fun getItemCount(): Int {
        return medications.size
    }

    fun clearList() {
        medications.clear()
    }

    inner class RowViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        var drugName: TextView = view.findViewById(R.id.drug_name)
        val strength: TextView = view.findViewById(R.id.strength)
        val quantity: TextView = view.findViewById(R.id.quantity)
        val dosage: TextView = view.findViewById(R.id.dosage_form)
        private val takenCheckBox: CheckBox = view.findViewById(R.id.taken)

        init {
            takenCheckBox.setOnCheckedChangeListener { _, isTaken ->
                listener.onTaken(medications[adapterPosition], isTaken)
            }
        }
    }
}