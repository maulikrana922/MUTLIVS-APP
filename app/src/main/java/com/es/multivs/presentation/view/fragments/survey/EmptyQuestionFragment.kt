package com.es.multivs.presentation.view.fragments.survey

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.es.multivs.data.models.Question
import dagger.hilt.android.AndroidEntryPoint

/**
 * Created by Marko on 1/10/2022.
 * Etrog Systems LTD.
 */

@AndroidEntryPoint
class EmptyQuestionFragment: Fragment() {

    companion object {
        fun newInstance(question: Question): EmptyQuestionFragment {
            val frag = EmptyQuestionFragment()
            val bundle = Bundle()
            bundle.putSerializable("question", question)
            frag.arguments = bundle
            return frag
        }
    }
}