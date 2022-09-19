package com.es.multivs.data.bledevices.multivs

import android.content.Context
import android.util.Log
import java.io.FileOutputStream
import java.lang.StringBuilder
import java.util.*

/**
 * Created by Marko on 10/20/2021.
 * Etrog Systems LTD.
 */
class EcgFileWriterByLength (
    private val fileLength: Int,
    context: Context,
    private val fileName: String,
) : ECGFileWriter {

    private var dataIterationsCount = 0
    private var fos: FileOutputStream? = null
    private var isFileFinished: Boolean = false

    companion object {
        fun intToByteArray(value: Int): ByteArray {
            return byteArrayOf(
                (value ushr 24).toByte(),
                (value ushr 16).toByte(),
                (value ushr 8).toByte(),
                value.toByte()
            )
        }
    }

    override fun writeToFile(data: String?, dataFormat: DataFormat?): Boolean {
        if (!isFileFinished) if (dataIterationsCount == 0) Log.i(
            "FileTime",
            "start: " + Calendar.getInstance().time.toString()
        )
        when (dataFormat) {
            DataFormat.INT_FORMAT -> {
                val ecgDataBytesStrings = data!!.split(",").toTypedArray()
                var i = 0
                while (i < ecgDataBytesStrings.size) {
                    val intValue = ecgDataBytesStrings[i].toFloat().toInt()
                    fos!!.write(intValue.toString().toByteArray())
                    if (!(dataIterationsCount == fileLength && i == ecgDataBytesStrings.size - 1)) fos!!.write(
                        ",".toByteArray()
                    )
                    i++
                }
            }
        }

        dataIterationsCount++
        if (dataIterationsCount == fileLength) isFileFinished = true
        if (isFileFinished) {
            Log.i("FileTime", "end: " + Calendar.getInstance().time.toString())
            fos!!.flush()
            fos!!.close()
        }


        return isFileFinished
    }

    override fun writeToFile(data: List<Int?>?): Boolean {
        val stringBuilder = StringBuilder()
        for (d in data!!) {
            stringBuilder.append(d)
            stringBuilder.append(",")
        }
        stringBuilder.setLength(stringBuilder.length - 1)
        return writeToFile(stringBuilder.toString(), DataFormat.INT_FORMAT)
    }

    init {
        fos = context.openFileOutput(fileName, Context.MODE_PRIVATE)
    }
}