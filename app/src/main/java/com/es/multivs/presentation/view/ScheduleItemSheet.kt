package com.es.multivs.presentation.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.os.bundleOf
import com.es.multivs.R
import com.es.multivs.data.models.ScheduleItem
import com.es.multivs.databinding.BottomsheetFragmentBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.android.AndroidEntryPoint
import com.es.multivs.data.utils.autoCleared


/*
* created by Marko
Etrog Systems LTD. 23/8/2021.
*
*/
@AndroidEntryPoint
class ScheduleItemSheet() : BottomSheetDialogFragment() {


    private var binding : BottomsheetFragmentBinding by autoCleared()


    companion object {
        private const val ITEM = "schedule_item"

        fun newInstance(item: ScheduleItem) = ScheduleItemSheet().apply {
            arguments = bundleOf(
                Pair(ITEM, item)
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = BottomsheetFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        dialog?.window?.attributes?.windowAnimations = R.style.DialogAnimation

        dialog?.setOnShowListener {
            val d = dialog as BottomSheetDialog
            val bottomSheet =
                d.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as FrameLayout?
            val coordinatorLayout = bottomSheet!!.parent as CoordinatorLayout
            val bottomSheetBehavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(bottomSheet)

            bottomSheetBehavior.peekHeight = bottomSheet.height


            coordinatorLayout.parent.requestLayout()
        }

        val item = requireArguments().get(ITEM) as ScheduleItem
        isCancelable = true
        binding.scheduleTitle.text = item.getTitle()
        binding.scheduleTime.text = item.getTime()
        binding.scheduleContent.text = item.showContent()
        if (item.isItemActive()) {
            binding.active.visibility = View.VISIBLE
        } else {
            binding.active.visibility = View.GONE
        }

        binding.okBtn.setOnClickListener {
            dismiss()
        }
    }
}