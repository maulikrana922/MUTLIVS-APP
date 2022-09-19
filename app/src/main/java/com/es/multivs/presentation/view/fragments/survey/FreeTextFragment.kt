package com.es.multivs.presentation.view.fragments.survey

import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import com.es.multivs.data.models.Question
import com.es.multivs.data.utils.InputChangeCollector
import com.es.multivs.data.utils.autoCleared
import com.es.multivs.databinding.FreeTextQuestionBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * Created by Marko on 1/10/2022.
 * Etrog Systems LTD.
 */

@AndroidEntryPoint
class FreeTextFragment : Fragment() {

    companion object {
        fun newInstance(question: Question, surveyName: String): FreeTextFragment {
            val frag = FreeTextFragment()
            val bundle = Bundle()
            bundle.putString("survey_name", surveyName)
            bundle.putSerializable("question", question)
            frag.arguments = bundle
            return frag
        }

        const val TAG = "Surveys"
    }

    private var binding: FreeTextQuestionBinding by autoCleared()
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
        binding = FreeTextQuestionBinding.inflate(inflater, container, false)
        Log.d(TAG, "onCreateView")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews()
        handleExistingAnswer()
    }

    private fun handleExistingAnswer() {
        if (!question.answer.isNullOrBlank()) {
            binding.freeAnswerEt.setText(question.answer)
        }
    }

    private fun initViews() {
        binding.surveyName.text = surveyName
        binding.question.text = question.questionText

        binding.freeAnswerEt.imeOptions = EditorInfo.IME_ACTION_DONE
        binding.freeAnswerEt.setRawInputType(InputType.TYPE_CLASS_TEXT)


        val collector = InputChangeCollector {

            question.isAnswered = it.isNotBlank()

            if (question.isAnswered) {
                question.answer = it
            } else {
                question.answer = null
            }

            surveyListener?.handleAnswer(question )
        }

        binding.freeAnswerEt.addTextChangedListener(collector.inputCollector)
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