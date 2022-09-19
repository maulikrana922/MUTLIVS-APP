package com.es.multivs.presentation.view.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.es.multivs.BuildConfig
import com.es.multivs.R
import com.es.multivs.data.network.NetworkConnectionManager
import com.es.multivs.data.network.TokenKeeper
import com.es.multivs.data.utils.Constants
import com.es.multivs.data.utils.autoCleared
import com.es.multivs.databinding.LogInFragmentBinding
import com.es.multivs.presentation.view.viewmodels.UserDetailsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import java.net.SocketTimeoutException
import java.util.*

/**
 * Starting from Android 11, access to the device's mac address is denied.
 * To overcome this problem, if a device runs Android 11 and above,
 * the user will be identified via aaid (Android advertising ID).
 * If below Android 11, The identification is by mac address.
 */

/**
 * created by Marko
 * Etrog Systems LTD. 1/9/2021.
 */
@AndroidEntryPoint
class LogInFragment : Fragment() {

    fun interface PostFrequencyInterface {
        fun onFrequencyReceived(frequency: Int)
    }

    private lateinit var frequencyCallback: PostFrequencyInterface

    private var binding: LogInFragmentBinding by autoCleared()
    private val userDetailsViewModel: UserDetailsViewModel by activityViewModels()

    /**
     * The url address that will be used to attempt a login request.
     * When the user selects a country, the string representation of that country
     * is saved in the database. The observer updates the value of this string
     * everytime the url in the database is saved.
     */
    private lateinit var _url: String

    /**
     * Starting from Android 11, access to the device's mac address is denied.
     * To overcome this problem, if a device runs Android 11 and above,
     * the user will be identified via aaid (Android advertising ID).
     * If below Android 11, The identification is by mac address.
     */

    private val _handler = Handler(Looper.getMainLooper())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        frequencyCallback = requireActivity() as PostFrequencyInterface
        binding = LogInFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        createClickableTextForTermsConditions()
        _url = "${BuildConfig.SERVER}member-login" // default url

        /**
         * Call this first to init userDetailsViewModel on main thread
         */
        userDetailsViewModel.isLoggingIn().observe(viewLifecycleOwner) { doingSomething ->
            if (doingSomething) {
                binding.progressBar.visibility = View.VISIBLE
                binding.loginBtn.text = ""
            } else {
                binding.progressBar.visibility = View.GONE
                binding.loginBtn.text = getString(R.string.login)
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {

            userDetailsViewModel.getUserCredentials().let { creds ->
                creds?.let {
                    if (it.username.isNotEmpty() && it.password.isNotEmpty()) {
                        _handler.post {
                            binding.usernameLogin.setText(it.username)
                            binding.passwordLogin.setText(it.password)
                        }
                    }
                }
            }

            userDetailsViewModel.getUserCountry().let { country ->
                country?.let {
                    _handler.post {
                        if (it.isEmpty() || it == getString(R.string.usa)) {
                            binding.chooseCountryBtn.text = getString(R.string.usa)
                            _url = Constants.BASE_URL.plus("member-login")
                        } else {
                            binding.chooseCountryBtn.text = it
                            _url = getString(R.string.israel_login_url)
                        }
                    }
                }
            }
        }

        userDetailsViewModel.insertGatewayData()

        initViews()

    }

    private fun initViews() {

        binding.loginBtn.setOnClickListener {
            tryToConnect()
        }

        binding.tvVersion.text = "Ver: ${BuildConfig.VERSION_NAME}"
        Glide.with(requireContext()).load(R.drawable.multivs_logo)
            .transition(withCrossFade()).error(R.drawable.ic_error)
            .into(binding.logo)
        Glide.with(requireContext()).load(R.drawable.img_login_bottom_layer)
            .transition(withCrossFade())
            .error(R.drawable.ic_error).into(binding.imageView5)
        Glide.with(requireContext()).load(R.drawable.img_login_top_layer)
            .transition(withCrossFade())
            .error(R.drawable.ic_error).into(binding.imageView4);

        binding.chooseCountryBtn.setOnClickListener {

            val countrySheet = CountrySheet.newInstance()
            countrySheet.isCancelable = true
            countrySheet.show(childFragmentManager, "selectCountrySheet")
            countrySheet.setOnClickListener { country ->
                setCountryServer(country)
                binding.chooseCountryBtn.text = country
            }
        }

        binding.textView6.text = "© ${getYear()} ${getString(R.string._2020_etrog_systems_ltd)}"
    }

    private fun setCountryServer(givenCountryString: String) {

        val israelString = resources.getString(R.string.israel)
        val usaString = resources.getString(R.string.usa)

        if (usaString == givenCountryString) {
            _url = Constants.BASE_URL.plus("member-login")
        } else if (israelString == givenCountryString) {
            _url = getString(R.string.israel_login_url)
        }
    }

    private fun tryToConnect() {

        val username = binding.usernameLogin.text.toString().trim()
        val password = binding.passwordLogin.text.toString().trim()

        if (username == "" || password == "") {

            onBadCredentials()

        } else if (!NetworkConnectionManager(requireActivity()).isNetworkAvailable) {

            onNoInternetConnection()

        } else {
            lifecycleScope.launch(Dispatchers.IO) {
                val identifier = userDetailsViewModel.resolveIdentifier()
                try {
                    // withTimeout(15000) {
                    val response: UserDetailsViewModel.LoginStatus =
                        userDetailsViewModel.attemptLogin(_url, username, password, identifier)
                    handleResponse(response, username, password)
                    // }
                } catch (e: TimeoutCancellationException) {
                    _handler.post {
                        handleResponse(
                            UserDetailsViewModel.LoginStatus.Failed("Login Error"),
                            username,
                            password
                        )
                    }
                } catch (e: SocketTimeoutException) {
                    _handler.post {
                        handleResponse(
                            UserDetailsViewModel.LoginStatus.Failed("Login Error"),
                            username,
                            password
                        )
                    }
                } catch (e: Exception) {
                    _handler.post {
                        handleResponse(
                            UserDetailsViewModel.LoginStatus.Failed("Login Error"),
                            username,
                            password
                        )
                    }
                    Log.e("_SCANNING", "tryToConnect: ${e.message}")
                }
            }
        }
    }

    private fun handleResponse(
        response: UserDetailsViewModel.LoginStatus,
        username: String,
        password: String
    ) {

        when (response) {
            is UserDetailsViewModel.LoginStatus.SuccessWithToken -> {
                TokenKeeper.instance?.token = response.token
                onLoginSuccessful(username, password)
            }
            is UserDetailsViewModel.LoginStatus.Failed -> {
                _handler.post {
                    showAlert(response.message)
                }
            }
            UserDetailsViewModel.LoginStatus.Success -> {
                onLoginSuccessful(username, password)
            }
            else -> showAlert("Unknown error")
        }
    }

    private fun showAlert(message: String) {
        val alertDialogFrag = AlertDialogFragment.newInstance().apply {
            arguments = bundleOf(
                Pair(AlertDialogFragment.TITLE, "Connection Failed"),
                Pair(AlertDialogFragment.BODY, message)
            )
        }
        alertDialogFrag.isCancelable = true
        alertDialogFrag.show(childFragmentManager, "onBadCredentials")
    }

    private fun onLoginSuccessful(username: String, password: String) {
        lifecycleScope.launch {
            when (_url) {
                Constants.BASE_URL.plus("member-login") -> {
                    userDetailsViewModel.setBaseURLAndCountry(
                        Constants.BASE_URL, getString(R.string.usa)
                    )
                }
                getString(R.string.israel_login_url) -> {
                    userDetailsViewModel.setBaseURLAndCountry(
                        getString(R.string.isr_base_url), getString(R.string.israel)
                    )
                }
            }

            userDetailsViewModel.setUserCredentials(username, password)
            userDetailsViewModel.loadUserDetails()
            val frequency = userDetailsViewModel.getPeriodicPostFrequency()
            frequencyCallback.onFrequencyReceived(frequency)

            val navHostFragment =
                requireActivity().supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            val navController = Objects.requireNonNull(navHostFragment).navController
            navController.navigate(R.id.action_logIn_to_viewPager)
        }


    }

    private fun onNoInternetConnection() {

        val alertDialogFrag = AlertDialogFragment.newInstance().apply {
            arguments = bundleOf(
                Pair(AlertDialogFragment.TITLE, "Login Error"),
                Pair(AlertDialogFragment.BODY, "The device is not connected to the Internet.")
            )
        }

        alertDialogFrag.isCancelable = true
        alertDialogFrag.show(childFragmentManager, "NoInternetConnection")
    }

    private fun onBadCredentials() {

        val alertDialogFrag = AlertDialogFragment.newInstance().apply {
            arguments = bundleOf(
                Pair(AlertDialogFragment.TITLE, "Connection Failed"),
                Pair(AlertDialogFragment.BODY, "Wrong user credentials.")
            )
        }

        alertDialogFrag.isCancelable = true
        alertDialogFrag.show(childFragmentManager, "onBadCredentials")
    }


    private fun createClickableTextForTermsConditions() {
        val termsAndConditionsText =
            "By clicking \"Login\" I indicate that I have read and agree to the Terms and Conditions"
        val mSpannableString = SpannableString(termsAndConditionsText)
        val mClickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                TermsAndConditionFragment.newInstance()
                    .show(childFragmentManager, TermsAndConditionFragment.TAG)
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.color = ContextCompat.getColor(requireActivity(), R.color.colorPrimary)
            }
        }
        mSpannableString.setSpan(mClickableSpan, 65, 85, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        binding.termsAndConditionsTv.text = mSpannableString
        binding.termsAndConditionsTv.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun getYear(): Int {
        val cal = Calendar.getInstance()
        return cal[Calendar.YEAR]
    }
}