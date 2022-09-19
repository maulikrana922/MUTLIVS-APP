package com.es.multivs.data.utils

import android.text.Editable
import android.text.TextWatcher

/**
 * Created by Marko on 11/2/2021.
 * Etrog Systems LTD.
 */
class InputChangeCollector(private val callback:InputWatcherCallback) {

    fun interface InputWatcherCallback{
        fun onInputChanged(string: String)
    }

    val inputCollector = object: TextWatcher{
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

        }

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            callback.onInputChanged(p0.toString())
        }

        override fun afterTextChanged(p0: Editable?) {

        }

    }
}