package com.es.multivs.presentation.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.es.multivs.data.network.netmodels.ReportsMeasurements
import com.es.multivs.databinding.ReportsMeasurementRecyclerRowBinding
import com.es.multivs.presentation.view.viewmodels.ReportsViewModel


/**
 * Created by Dinesh on 3/2/2022.
 */
class ReportsMeasurementsAdapter(val viewModel: ReportsViewModel) :
    RecyclerView.Adapter<ReportsMeasurementsAdapter.MeasurementsViewHolder>() {
    var measurements: MutableList<ReportsMeasurements.Message> = mutableListOf()

    inner class MeasurementsViewHolder(val mBinding: ReportsMeasurementRecyclerRowBinding) :
        RecyclerView.ViewHolder(mBinding.root) {
        fun bindData(
            reportsMeasurements: ReportsMeasurements.Message
        ) {
            mBinding.data = reportsMeasurements
            mBinding.vm = viewModel
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MeasurementsViewHolder {
        return MeasurementsViewHolder(
            ReportsMeasurementRecyclerRowBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: MeasurementsViewHolder, position: Int) {
        holder.bindData(measurements[position])
    }

    override fun getItemCount(): Int {
        return measurements.size
    }

}