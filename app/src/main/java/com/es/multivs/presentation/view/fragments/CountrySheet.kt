package com.es.multivs.presentation.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.es.multivs.R
import com.es.multivs.databinding.CountryBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint

/**
 * created by Marko
 * Etrog Systems LTD. 1/9/2021.
 */
@AndroidEntryPoint
class CountrySheet() : BottomSheetDialogFragment() {


    private var _binding: CountryBottomSheetBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val ITEM = "schedule_item"

        fun newInstance() = CountrySheet()
    }

    private var listener: OnCountryClick? = null

    fun setOnClickListener(listener: OnCountryClick) {
        this.listener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = CountryBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Glide.with(requireContext())
            .load(R.drawable.israel_icon)
            .transition(DrawableTransitionOptions.withCrossFade())
            .error(R.drawable.ic_error).into(binding.israelFlag);

        Glide.with(requireContext())
            .load(R.drawable.usa_icon)
            .transition(DrawableTransitionOptions.withCrossFade())
            .error(R.drawable.ic_error).into(binding.usaFlag);


        dialog?.window?.attributes?.windowAnimations  = R.style.DialogAnimation

        dialog?.setOnShowListener {
            val d = dialog as BottomSheetDialog
            val bottomSheet =
                d.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as FrameLayout?
            val coordinatorLayout = bottomSheet!!.parent as CoordinatorLayout
            val bottomSheetBehavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(bottomSheet)
            bottomSheetBehavior.peekHeight = bottomSheet.height
            coordinatorLayout.parent.requestLayout()
        }

        binding.israelFlag.setOnClickListener {
            dismiss()
            listener?.onCountry("Israel")
            listener = null
            Toast.makeText(requireContext(), "Israel Selected", Toast.LENGTH_SHORT).show()
        }

        binding.usaFlag.setOnClickListener {
            dismiss()
            listener?.onCountry("USA")
            listener = null
            Toast.makeText(requireContext(), "USA Selected", Toast.LENGTH_SHORT).show()
        }

    }
}

fun interface OnCountryClick {
    fun onCountry(country: String)
}