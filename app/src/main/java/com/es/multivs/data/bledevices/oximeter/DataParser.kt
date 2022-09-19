package com.es.multivs.data.bledevices.oximeter

import java.util.concurrent.LinkedBlockingQueue

/**
 * Created by Marko on 11/2/2021.
 * Etrog Systems LTD.
 */
class DataParser(
    private val listener: onPackageReceivedListener
) {

    interface onPackageReceivedListener {
        fun onOxiParamsChanged(params: OxiParams)
        fun onPlethWaveReceived(amp: Int)
    }

    //Const
    private val bufferQueue = LinkedBlockingQueue<Int>(256)
    private var isStop = false

    private val mOxiParams = OxiParams()
    private var parseThread: Thread? = null

    fun start() {
        //Parse Runnable
        val mParseRunnable = ParseRunnable()
        if (parseThread == null) {
            parseThread = Thread(mParseRunnable)
        }
        parseThread!!.start()
        //    new Thread(mParseRunnable).start();
    }

    fun stop() {
        isStop = true
        if (parseThread != null && !parseThread!!.isInterrupted) {
            parseThread!!.interrupt()
            //   parseThread = null;
        }
    }

    /**
     * Add the data received from USB or Bluetooth
     * @param dat byte array
     */
    fun add(dat: ByteArray) {
        for (b in dat) {
            try {
                bufferQueue.put(toUnsignedInt(b))
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }

        //Log.i(TAG, "add: "+ bufferQueue.size());
    }

    private fun toUnsignedInt(x: Byte): Int {
        return x.toInt() and 0xff
    }

    /**
     * ParseRunnable
     */
    inner class ParseRunnable : Runnable {
        var dat = 0
        lateinit var packageData: IntArray
        override fun run() {
            while (!isStop) {
                dat = getData()
                packageData = IntArray(5)
                if (dat and 0x80 > 0) {  //search package head
                    packageData[0] = dat
                    for (i in 1 until packageData.size) {
                        dat = getData()
                        if (dat and 0x80 == 0) {
                            packageData[i] = dat
                        }
                    }
                    val spo2 = packageData[4]
                    val pulseRate = packageData[3] or (packageData[2] and 0x40 shl 1)
                    val pi = packageData[0] and 0x0f
                    if (spo2 != mOxiParams.spo2 || pulseRate != mOxiParams.pulseRate || pi != mOxiParams.pi) {
                        mOxiParams.update(spo2, pulseRate, pi)
                        listener.onOxiParamsChanged(mOxiParams)
                    }
                    listener.onPlethWaveReceived(packageData[1])
                }
            }
        }
    }

    /**
     * Get Dat from Queue
     */
    private fun getData(): Int {
        var dat = 0
        try {
            dat = bufferQueue.take()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        return dat
    }

    inner class OxiParams {
        var spo2 = 0
        var pulseRate = 0
        var pi = 0

        fun update(spo2: Int, pulseRate: Int, pi: Int) {
            this.spo2 = spo2
            this.pulseRate = pulseRate
            this.pi = pi
        }
    }
}