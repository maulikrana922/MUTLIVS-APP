package com.es.multivs.presentation.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.es.multivs.data.network.netmodels.ReportsMedication
import com.es.multivs.databinding.ReportsMedicationRecyclerRowBinding
import com.es.multivs.presentation.view.viewmodels.ReportsViewModel


/**
 * Created by Dinesh on 3/2/2022.
 */
class ReportsMedicationsAdapter(
    val viewModel: ReportsViewModel
) : RecyclerView.Adapter<ReportsMedicationsAdapter.MedicationsViewHolder>() {

    var medications: MutableList<ReportsMedication.Message.Medication> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MedicationsViewHolder {
        return MedicationsViewHolder(
            ReportsMedicationRecyclerRowBinding.inflate(
                LayoutInflater.from(
                    parent.context
                ), parent, false
            )
        )
    }

    inner class MedicationsViewHolder(val mBinding: ReportsMedicationRecyclerRowBinding) :
        RecyclerView.ViewHolder(mBinding.root) {
        fun bindData(
            reportsMedication: ReportsMedication.Message.Medication
        ) {
            mBinding.data = reportsMedication
            mBinding.vm = viewModel
        }
    }

    override fun onBindViewHolder(holder: MedicationsViewHolder, position: Int) {
        holder.bindData(medications[position])
    }

    override fun getItemCount(): Int {
        return medications.size
    }

}