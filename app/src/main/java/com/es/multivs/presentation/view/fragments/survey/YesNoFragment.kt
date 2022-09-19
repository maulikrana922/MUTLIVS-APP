package com.es.multivs.presentation.view.fragments.survey

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.fragment.app.Fragment
import com.es.multivs.data.models.Question
import com.es.multivs.data.utils.autoCleared
import com.es.multivs.data.utils.getButtons
import com.es.multivs.databinding.YesNoQuestionBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * Created by Marko on 1/10/2022.
 * Etrog Systems LTD.
 */

@AndroidEntryPoint
class YesNoFragment : Fragment() {

    companion object {
        fun newInstance(question: Question, surveyName: String): YesNoFragment {
            val frag = YesNoFragment()
            val bundle = Bundle()
            bundle.putString("survey_name", surveyName)
            bundle.putSerializable("question", question)
            frag.arguments = bundle
            return frag
        }

        const val TAG = "Surveys"
    }

    private var binding: YesNoQuestionBinding by autoCleared()
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
        binding = YesNoQuestionBinding.inflate(inflater, container, false)
        Log.d(TAG, "onCreateView")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews()
        handleExistingAnswer()
    }

    private fun initViews() {

        binding.surveyName.text = surveyName
        binding.question.text = question.questionText

        binding.radioGroup.setOnCheckedChangeListener { radioGroup, i ->
            Log.d("SurveyViewModel", "onViewCreated: $i")



            val radioBtn = radioGroup.findViewById<RadioButton>(radioGroup.checkedRadioButtonId)
            question.answer = radioBtn.text.toString()
            question.isAnswered = true
            surveyListener?.handleAnswer(question)
        }
    }

    private fun handleExistingAnswer() {
        if (!question.answer.isNullOrBlank()){
            val radioBtn = binding.radioGroup.getButtons().find { it.text.toString() == question.answer }
            radioBtn?.isChecked = true
        }
    }

    override fun onResume() {
        super.onResume()
        surveyListener = parentFragment as SurveyFragment
    }
}