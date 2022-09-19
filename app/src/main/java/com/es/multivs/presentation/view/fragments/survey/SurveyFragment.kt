package com.es.multivs.presentation.view.fragments.survey

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.es.multivs.R
import com.es.multivs.data.database.entities.SurveyScheduleItem
import com.es.multivs.data.models.Question
import com.es.multivs.data.utils.AppUtils
import com.es.multivs.data.utils.ScheduleUpdatesManager
import com.es.multivs.data.utils.SurveyUtils
import com.es.multivs.data.utils.autoCleared
import com.es.multivs.databinding.SurveyFragment2Binding
import com.es.multivs.presentation.view.fragments.AlertSurveyFragment
import com.es.multivs.presentation.view.viewmodels.SurveyViewModel
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SurveyFragment : Fragment(), SurveyListener {

    @Inject
    lateinit var updates: ScheduleUpdatesManager

    private val viewModel: SurveyViewModel by viewModels()
    private var binding: SurveyFragment2Binding by autoCleared()

    private var uploadAlert: AlertDialog? = null

    private var surveyItem: SurveyScheduleItem? = null
    private lateinit var questionList: ArrayList<Question>
    private var currentIndex = 0


    companion object {
        fun newInstance() = SurveyFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = SurveyFragment2Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.nextBtn.setOnClickListener {
            onNext()
        }

        binding.backBtn.setOnClickListener {
            onPrev()
        }

        viewModel.surveyUpload.observe(viewLifecycleOwner, { response ->

            uploadAlert?.let {
                if (it.isShowing) {
                    it.dismiss()
                }
            }

            if (response.success) {
                updates.surveyComplete(surveyItem!!.timestamp)
            } else {
                AppUtils.showAlertFragment(
                    parentFragmentManager,
                    getString(R.string.surveys),
                    response.message
                )
            }


            val viewPager2: ViewPager2 = requireActivity().findViewById(R.id.viewpager2)
            viewPager2.setCurrentItem(0, false)
            val tabLayout: TabLayout = requireActivity().findViewById(R.id.tab_layout)
            tabLayout.visibility = View.VISIBLE

        })

        initViews()
    }

    override fun onResume() {
        super.onResume()
        currentIndex = 0
        fetchSurvey()
        surveyCounter = 1
    }

    override fun onPause() {
        super.onPause()
        viewModel.clearAnswers()
        childFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
//        AppUtils.dismissSnackbar()
    }

    private fun initViews() {

    }

    private var surveyCounter = 1

    private fun fetchSurvey() {
        lifecycleScope.launch {
            surveyItem = viewModel.getSurveyFromDatabaseByDay()

            surveyItem?.let { survey ->

                binding.surveyLayout2.transitionToStart()

                binding.backBtn.visibility = View.GONE
                questionList = survey.questionList

                binding.surveyCounter.text = "$surveyCounter / ${questionList.size}"

                val firstQuestion = questionList[0]

                val transaction = SurveyUtils.getSurveyTransaction(childFragmentManager)
                val currentTag =
                    "${firstQuestion.qID}_${firstQuestion.questionType}_$currentIndex"
                val frag = SurveyUtils.surveyFragmentFactory(firstQuestion, surveyItem!!.surveyName)
                transaction.add(R.id.survey_container, frag, currentTag)
                    .addToBackStack(currentTag).commit()
            }

            if (surveyItem == null) {
                binding.surveyLayout2.transitionToEnd()
            }
        }
    }


    private fun postAnswers() {
        showUploadingAlert()
        lifecycleScope.launch {
            viewModel.postAnswers()
        }
    }

    private fun showUploadingAlert() {
        uploadAlert = AlertDialog.Builder(requireContext()).create()
        uploadAlert?.apply {
            val layoutInflater = LayoutInflater.from(requireActivity())
            val promptView: View =
                layoutInflater.inflate(R.layout.refresh_layout, binding.root, false)
            setView(promptView)

            val title = promptView.findViewById<TextView>(R.id.text_loading)
            title.text = getString(R.string.uploading_survey)

            setCancelable(false)
            show()
        }
    }

    override fun handleAnswer(answeredQuestion: Question) {
        if (answeredQuestion.isAnswered) {
            viewModel.handleAnswer(answeredQuestion)
        } else {
            viewModel.removeAnswer(answeredQuestion)
        }
    }


    private fun onPrev() {

        currentIndex -= 1
        if (currentIndex >= 0) {

            if (currentIndex == 0) {
                binding.backBtn.visibility = View.GONE
            }

            val question = questionList[currentIndex]
            val frag = SurveyUtils.surveyFragmentFactory(question, surveyItem!!.surveyName)

            childFragmentManager.popBackStack()

            val transaction = SurveyUtils.getSurveyTransaction(childFragmentManager)

            val currentTag = "${question.qID}_${question.questionType}_$currentIndex"
            transaction.add(R.id.survey_container, frag, currentTag)
                .addToBackStack(currentTag).commit()

            surveyCounter -= 1
            binding.surveyCounter.text = "$surveyCounter / ${questionList.size}"

        } else {
            currentIndex = 0
        }
    }

    private fun onNext() {

        if (questionList[currentIndex].required == "True") {
            val answeredQuestion = viewModel.getAnsweredQuestion(questionList[currentIndex].qID)
            if (answeredQuestion == null) {
                AppUtils.makeErrorSnackbarNoAction(
                    binding.nextBtn,
                    "This question cannot be skipped."
                )
                return
            }
        }

        currentIndex += 1
        if (currentIndex < questionList.size) {

            binding.backBtn.visibility = View.VISIBLE

            val questionID = questionList[currentIndex].qID
            var question = viewModel.getAnsweredQuestion(questionID)

            if (question == null) {
                question = questionList[currentIndex]
            }


            val frag = SurveyUtils.surveyFragmentFactory(question, surveyItem!!.surveyName)

            childFragmentManager.popBackStack()

            val transaction = SurveyUtils.getSurveyTransaction(childFragmentManager)

            val currentTag = "${question.qID}_${question.questionType}_$currentIndex"
            transaction.add(R.id.survey_container, frag, currentTag)
                .addToBackStack(currentTag).commit()

            surveyCounter += 1
            binding.surveyCounter.text = "$surveyCounter / ${questionList.size}"

        } else {
            currentIndex = questionList.size - 1
            val surveyDialog = AlertSurveyFragment.newInstance()
            surveyDialog.listen {
                when (it) {
                    getString(R.string.yes) -> {
                        postAnswers()
                    }
                    getString(R.string.no) -> {
                        // do nothing
                    }
                }
            }
            surveyDialog.show(childFragmentManager, "onBadCredentials")
            //TODO: submit page
        }
    }


}