package com.es.multivs.data.bledevices.multivs

import java.io.IOException

/**
 * Created by Marko on 10/20/2021.
 * Etrog Systems LTD.
 */
interface ECGFileWriter {

    @Throws(IOException::class)
    fun writeToFile(data: String?, dataFormat: DataFormat?): Boolean

    @Throws(IOException::class)
    fun writeToFile(data: List<Int?>?): Boolean
}