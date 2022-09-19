package com.es.multivs.presentation.view.fragments

import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.es.multivs.BuildConfig
import com.es.multivs.R
import com.es.multivs.data.utils.AppUtils
import com.es.multivs.data.utils.autoCleared
import com.es.multivs.databinding.FragmentAboutBinding
import com.es.multivs.presentation.view.viewmodels.MultiVsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.*


@AndroidEntryPoint
class AboutFragment : Fragment() {
    companion object {
        fun newInstance() = AboutFragment()
    }

    private var binding: FragmentAboutBinding by autoCleared()
    private val viewModel: MultiVsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        //return inflater.inflate(R.layout.fragment_about, container, false)
        binding = FragmentAboutBinding.inflate(inflater, container, false)
        initData()
        return binding.root
    }

    private fun initData() {

        binding.tvAbout.text = Html.fromHtml(
            "<b>About the MULTIVS APP:</b> <br><br> " +
                    "This application and tablet serving as the gateway that connects the wireless wearable monitor with the ETROG Care cloud platform. It enables patients to monitor their vital signs, perform spot and scheduled measurements.<br> " +
                    "<br><b>About ETROG Systems:</b> <br><br>ETROG SYSTEMS is an R&D and medical devices manufacturer based in K. Gat , Israel.We introduce a revolutionary breakthrough solution integrating hardware and software platform technology which enables remote patient and cardiac monitoring in a unique all-in-one, multi-parameter wearable device.<br> <br> " +
                    "<b>For Support : </b>Call 845‐88ETROG 38874 <br>"
        )
        lifecycleScope.launch {
            binding.tvUserid.text = Html.fromHtml("<b>UserID:</b> ${viewModel.getUserId()}")
            binding.tvVersion.text =
                Html.fromHtml("<b>App Ver & Build:</b> ${BuildConfig.VERSION_NAME}(${getString(R.string.build_name)})")
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                val identifier = AppUtils.getMacAddress()
                binding.tvAdvertiseId.text = Html.fromHtml("<b>Mac of Tablet:</b> $identifier")
            } else {
                binding.tvAdvertiseId.text =
                    Html.fromHtml("<b>Advertising ID of Tablet:</b> ${viewModel.getAdvertisingId()}")
            }
        }
    }

}