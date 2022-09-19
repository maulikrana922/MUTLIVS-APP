package com.es.multivs.presentation.view.fragments

/**
 * Created by Marko on 10/21/2021.
 * Etrog Systems LTD.
 */
interface SummaryListener {
    fun onExitFromSummary(isSaved: Boolean, message: String = "")
}