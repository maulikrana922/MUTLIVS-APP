package com.es.multivs.presentation.view.fragments.survey

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.fragment.app.Fragment
import com.es.multivs.R
import com.es.multivs.data.models.Question
import com.es.multivs.data.utils.autoCleared
import com.es.multivs.data.utils.getButtons
import com.es.multivs.databinding.RatingQuestionBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * Created by Marko on 1/10/2022.
 * Etrog Systems LTD.
 */

@AndroidEntryPoint
class RatingFragment: Fragment(), View.OnClickListener {

    companion object {
        fun newInstance(question: Question, surveyName: String): RatingFragment {
            val frag = RatingFragment()
            val bundle = Bundle()
            bundle.putSerializable("question", question)
            bundle.putString("survey_name", surveyName)
            frag.arguments = bundle
            return frag
        }

        const val TAG = "Surveys"
    }

    private var binding: RatingQuestionBinding by autoCleared()
    private var surveyListener: SurveyListener? = null
    private lateinit var question: Question
    private lateinit var surveyName: String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        question = arguments?.getSerializable("question") as Question
        surveyName = arguments?.getString("survey_name") as String
        binding = RatingQuestionBinding.inflate(inflater, container, false)
        Log.d(TAG, "onCreateView")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews()
        handleExistingAnswer()
    }

    /**
     * [initViews] must be called before handling existing answers
     * in order to build the radio buttons first.
     */
    private fun initViews() {

        binding.surveyName.text = surveyName
        binding.question.text = question.questionText

        question.answerText?.forEach {

            val rb = LayoutInflater.from(requireContext())
                .inflate(R.layout.rating_radio_button, binding.ratingRadioGroup, false) as RadioButton

            rb.isChecked = false
            rb.layoutParams = RadioGroup.LayoutParams(
                RadioGroup.LayoutParams.MATCH_PARENT,
                RadioGroup.LayoutParams.MATCH_PARENT,
                1f
            )
            rb.text = it
            rb.setOnClickListener(this)

            binding.ratingRadioGroup.addView(rb)
        }
    }

    private fun handleExistingAnswer() {
        if (!question.answer.isNullOrBlank()){
            val radioBtn = binding.ratingRadioGroup.getButtons().find { it.text.toString() == question.answer }
            radioBtn?.isChecked = true
        }
    }

    override fun onResume() {
        super.onResume()
        surveyListener = parentFragment as SurveyFragment
        Log.d(TAG, "onResume")
    }

    override fun onPause() {
        super.onPause()
        surveyListener = null
        Log.d(TAG, "onPause")
    }

    override fun onClick(view: View) {
        question.answer = (view as RadioButton).text.toString()
        question.isAnswered = true
        surveyListener?.handleAnswer(question)
    }
}