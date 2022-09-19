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
import com.es.multivs.databinding.MultipleQuestionBinding
import dagger.hilt.android.AndroidEntryPoint
import java.lang.IndexOutOfBoundsException

/**
 * Created by Marko on 1/10/2022.
 * Etrog Systems LTD.
 */

@AndroidEntryPoint
class MultipleFragment : Fragment() {

    companion object {
        fun newInstance(question: Question, surveyName: String): MultipleFragment {
            val frag = MultipleFragment()
            val bundle = Bundle()
            bundle.putString("survey_name", surveyName)
            bundle.putSerializable("question", question)
            frag.arguments = bundle
            return frag
        }

        const val TAG = "Surveys"
    }

    private var binding: MultipleQuestionBinding by autoCleared()
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
        binding = MultipleQuestionBinding.inflate(inflater, container, false)
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

        try {
            question.answerText?.let {
                binding.radioBtn1.text = it[0]
                binding.radioBtn1.visibility = View.VISIBLE

                binding.radioBtn2.text = it[1]
                binding.radioBtn2.visibility = View.VISIBLE

                binding.radioBtn3.text = it[2]
                binding.radioBtn3.visibility = View.VISIBLE

                binding.radioBtn4.text = it[3]
                binding.radioBtn4.visibility = View.VISIBLE

                binding.radioBtn5.text = it[4]
                binding.radioBtn5.visibility = View.VISIBLE
            }

            /**
             * In case there are less than 5 answers in the question.
             * Not optimal but will do for now.
             */
        } catch (e: IndexOutOfBoundsException) {
            Log.d("_SURVEY", "No more answers left in this question, ${e.message}")
        }


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
        Log.d(TAG, "onResume")
    }

    override fun onPause() {
        super.onPause()
        surveyListener = null
        Log.d(TAG, "onPause")
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
    }
}