package com.es.multivs.data.repository

import java.lang.Exception

/**
 * Created by Marko on 10/21/2021.
 * Etrog Systems LTD.
 */
class ResultPostException(val msg: String, e:Exception) : Exception()

class ResultFailedException(val msg: String) : Exception()