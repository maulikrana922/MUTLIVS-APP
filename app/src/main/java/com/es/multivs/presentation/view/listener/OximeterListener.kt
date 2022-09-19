package com.es.multivs.presentation.view.listener

/**
 * Created by Dinesh on 10/21/2021.
 * Etrog Systems LTD.
 */
interface OximeterListener {
    fun onExitFromOximeter(isSaved: Boolean, message: String = "")
}