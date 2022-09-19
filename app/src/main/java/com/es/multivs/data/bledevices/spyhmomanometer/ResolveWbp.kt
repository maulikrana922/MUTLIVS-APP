package com.es.multivs.data.bledevices.spyhmomanometer

import android.content.Context
import android.util.Log
import com.example.bluetoothlibrary.*
import com.example.bluetoothlibrary.Impl.ResolveWbp
import com.example.bluetoothlibrary.entity.BleData
import com.example.bluetoothlibrary.entity.SampleGattAttributes
import com.example.bluetoothlibrary.entity.SycnBp
import java.lang.Exception
import java.lang.StringBuilder
import java.math.BigInteger
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

/**
 * Created by Marko on 11/1/2021.
 * Etrog Systems LTD.
 */
class ResolveWbp : WbpData {


    private var bpDataList: ArrayList<Int> = ArrayList()
    private var currentCount = 0
    private var beforeErrorCount = 0L
    private var timerResendData: Timer? = null
    private var realDataTimer: Timer? = null
    private var stateCount = 0
    private var i = 0

    var obStr: Any = "0"
    var mesurebp = 0
    var endsys = 0
    var enddia = 0
    var endhr = 0
    var battey = 0
    var user_mode = 0
    var blestate: String? = null
    var ver: String? = null
    var time: String? = null
    var devstate: String? = null
    var guest = false
    var bps: ArrayList<SycnBp?> = ArrayList()
    var onWBPDataListener: OnWBPDataListener? = null

    companion object {
        var DROPMEASURE = -1
        var WBPMODE = -1
        var iserror = false

        fun BitToByte(byteStr: String?): Byte {
            return if (null == byteStr) {
                0
            } else {
                val len = byteStr.length
                if (len != 4 && len != 8) {
                    0
                } else {
                    val re: Int
                    re = if (len == 8) {
                        if (byteStr[0] == '0') {
                            byteStr.toInt(2)
                        } else {
                            byteStr.toInt(2) - 256
                        }
                    } else {
                        byteStr.toInt(2)
                    }
                    re.toByte()
                }
            }
        }

        fun byteToBit(b: Int): String {
            return "" + (b shr 7 and 1).toByte() + (b shr 6 and 1).toByte() + (b shr 5 and 1).toByte() + (b shr 4 and 1).toByte() + (b shr 3 and 1).toByte() + (b shr 2 and 1).toByte() + (b shr 1 and 1).toByte() + (b shr 0 and 1).toByte()
        }
    }

    fun resolveBPData2(datas: ByteArray, mBLE: BluetoothLeClass) {
        try {

            var length: Int = 0
            while (length < datas.size) {
                if (datas[length] < 0) {
                    bpDataList.add(datas[length] + 256)
                } else {
                    bpDataList.add(datas[length].toInt())
                }
                ++length
            }
            length = bpDataList.size
            val tstr = StringBuilder()

            var j = 0
            while (j < length) {
                tstr.append(",").append(bpDataList[j])
                if (bpDataList[j] == 170 && j < length - 1 && bpDataList[j + 1] != 170) {
                    var n = bpDataList[j + 1]!!
                    var sum = n
                    if (j + 1 + n > length) {
                        break
                    }

                    var pID: Int
                    var k: Int = 0
                    while (k < n - 1 && j + 1 + n <= length) {
                        if (bpDataList[j + 1 + k + 1] == 170) {
                            if (bpDataList[j + 1 + k + 2] != 170) {
                                Log.e("test", "exception,the data have no 0xAA")
                                return
                            }
                            ++k
                            ++n
                        }
                        pID = bpDataList[j + 1 + k + 1]!!
                        Log.e("muk", "received add��������������������$pID")
                        Log.e("muk", "received sum��������������������$sum")
                        sum += pID
                        Log.e("muk", "received sum2��������������������$sum")
                        k++
                    }

                    if (j + 1 + n > length) {
                        return
                    }

                    val bytesTwo = mutableListOf<Int>()
                    pID = 0
                    while (pID < n - 1) {
                        bytesTwo.add(bpDataList[j + 1 + pID + 1])
                        if (bpDataList[j + 1 + pID + 1] == 170 && bpDataList[j + 1 + pID + 2] == 170) {
                            ++pID
                        }
                        pID++
                    }

                    pID = bytesTwo[0]!!
                    if (pID == 43) {
                        Log.e(
                            "nice",
                            "--------------------now is ,synchronization----------------------------------------------------"
                        )
                        if (SettingUtil.resendData) {
                            Log.e(
                                "nice",
                                "--------------------synchronization-----resenddata-----------------------------------------------"
                            )
                            SettingUtil.isHaveData = true
                        } else {
                            ++currentCount
                        }
                    }

                    val checksum = bpDataList[j + 1 + n]!!
                    if (checksum != sum % 256 && checksum + sum % 256 != 256) {
                        Log.e("muk", "check failed��data exception��������������������")
                        if (pID == 43) {
                            Log.e(
                                "nice",
                                "--------------------now is ,synchronization--------------------------------------------------"
                            )
                            if (SettingUtil.resendData) {
                                this.finishToResendData(mBLE)
                                return
                            }
                            Log.e(
                                "nice",
                                "--------------------now is ,synchronization..add-----------------------------------------------"
                            )
                            ++beforeErrorCount
                            val mydata = BleData()
                            mydata.byte1 = bytesTwo[1]!!
                            mydata.byte2 = bytesTwo[2]!!
                            mydata.byte3 = bytesTwo[3]!!
                            mydata.byte4 = bytesTwo[4]!!
                            SampleGattAttributes.listdata.add(mydata)
                        }
                    } else {
                        Log.e("muk", "received pid��������������������$pID")
                        Log.e(
                            "muk",
                            bytesTwo.size.toString() + "-<------------------------first-----pID===============����" + pID
                        )
                        var SYS: Int
                        var five: Int
                        var six: Int
                        var data: ByteArray
                        when (pID) {
                            3 -> {
                                var tempid = bytesTwo[1]!!
                                if (tempid < 0) {
                                    tempid += 256
                                }
                                Log.e(
                                    "muk",
                                    "PID��-----------�����·�������\t------46=====����$tempid"
                                )
                                Log.e("muk", "-----------------���ص�Ӧ����====����" + bytesTwo[2])
                                if (tempid == 128) {
                                    Log.e("�û��ı�", tempid.toString() + "")
                                    if (!SettingUtil.isGusetmode && WBPMODE == 0) {
                                        val timer = Timer()
                                        timer.schedule(object : TimerTask() {
                                            override fun run() {
                                                getNowDateTime(mBLE)
                                            }
                                        }, 600L)
                                    }
                                }
                                if (tempid == 107) {
                                    Log.e(
                                        "muk",
                                        "SettingUtil.isfirstLoad7-------" + SettingUtil.isfirstLoad7
                                    )
                                    if (SettingUtil.isfirstLoad7 == 0) {
                                        Log.e("sha", "Ѫѹ��״̬��������-------" + SettingUtil.bleState)
                                        if (SettingUtil.isGusetmode || SettingUtil.bleState != "0") {
                                            Log.e(
                                                "muk",
                                                "syncDataByBle-------����ģʽ��    �·��û�-------"
                                            )
                                            data = byteArrayOf(-86, 4, -128, 3, 0, 0)
                                            mBLE.writeCharacteristic_wbp(data)
                                        }
                                    }
                                    ++SettingUtil.isfirstLoad7
                                }
                                if (bytesTwo[2] == 85) {
                                    Log.e(
                                        "muk",
                                        "----------Ӧ������=====" + DROPMEASURE
                                    )
                                    if (bytesTwo[1] == 120 && DROPMEASURE == 8) {
                                        iserror = true
                                    }
                                } else if (bytesTwo[2] == 0) {
                                    Log.e("muk", "----------������=====")
                                } else {
                                    Log.e("muk", "----------��������=====")
                                }
                                Log.e("muk", "-----------------end====----------------")
                            }
                            40 -> {
                                val byte3: String = byteToBit(bytesTwo[3]!!)
                                Log.e("muk", "length-----------------" + bytesTwo.size)
                                val mode = byte3[6].toString() + "" + byte3[7]
                                if (mode == "01") {
                                    Log.e("mye", "is 01")
                                    return
                                }
                                if (mode == "00") {
                                    val tempCuff =
                                        (BitToByte(byte3[0].toString() + "") + bytesTwo[1]!!)
                                    var tempL = tempCuff.toInt()
                                    if (tempCuff < 0) {
                                        tempL = tempCuff + 256
                                    }
                                    Log.e("mye", "�� 00")
                                    Log.e(
                                        "mye",
                                        "Cuff pressure������-----------------------=$tempL"
                                    )
                                    mesurebp = tempL
                                }
                                SettingUtil.isSyncFinish = true
                            }
                            41 -> {
                                Log.e("muk", "PID is-----------------41")
                                val byteResult: String = byteToBit(bytesTwo[1]!!)
                                SettingUtil.isNowTestBpFinish = true
                                Log.e(
                                    "muk",
                                    "the test result--------------------------byteResult------����$byteResult"
                                )
                                val tempR = byteResult[3].toString() + ""
                                Log.e(
                                    "muk",
                                    "the test result--------------------------tempR------����$tempR"
                                )
                                if (tempR == "0") {
                                    Log.e(
                                        "muk",
                                        "the test result-----------------normal---------------"
                                    )
                                    iserror = false
                                    SYS = bytesTwo[3]!!
                                    if (SYS < 0) {
                                        SYS += 256
                                    }
                                    var dp = bytesTwo[4]!!
                                    var hr = bytesTwo[6]!!
                                    if (dp < 0) {
                                        dp += 256
                                    }
                                    if (hr < 0) {
                                        hr += 256
                                    }
                                    endsys = SYS
                                    enddia = dp
                                    endhr = hr
                                    guest = SettingUtil.isGusetmode
                                    val three =
                                        if (bytesTwo[7]!! < 0) bytesTwo[7]!! + 256 else bytesTwo[7]!!
                                    val four =
                                        if (bytesTwo[8]!! < 0) bytesTwo[8]!! + 256 else bytesTwo[8]!!
                                    five =
                                        if (bytesTwo[9]!! < 0) bytesTwo[9]!! + 256 else bytesTwo[9]!!
                                    six =
                                        if (bytesTwo[10]!! < 0) bytesTwo[10]!! + 256 else bytesTwo[10]!!
                                    val times = intArrayOf(three, four, five, six)
                                    time = HomeUtil.BuleToTime(times)
                                    Log.e("testing time", "" + time)
                                    Log.e("muk", "the result is ----------------SYSlow 8 bit=$SYS")
                                    Log.e("muk", "the result is -----------------DIA=$dp")
                                    Log.e("muk", "the result is -----------------PR=$hr")
                                    Log.e(
                                        "muk",
                                        "the result is ------------SettingUtil-----=" + SettingUtil.isGusetmode
                                    )
                                    if (SYS != 0 && dp != 0 && hr != 0) {
                                        obStr = "0"
                                    } else {
                                        if (SYS == 0) {
//                                            this.obStr = activity.getApplication().getString(string.ble_test_error15);
                                        } else if (dp == 0) {
//                                            this.obStr = activity.getApplication().getString(string.ble_test_error16);
                                        } else if (hr == 0) {
//                                            this.obStr = activity.getApplication().getString(string.ble_test_error17);
                                        }
                                        iserror = true
                                    }
                                } else {
                                    Log.e(
                                        "muk",
                                        "test result is -----------------wrong----bytesTwo.get(2)-----------------------" + bytesTwo[2]
                                    )
                                    iserror = true
                                    obStr = this.getErrorTip(bytesTwo)
                                }
                                Log.e(
                                    "mgf",
                                    "----------------bytesTwo------------------------$bytesTwo"
                                )
                            }
                            42 -> {
                                Log.e(
                                    "muk",
                                    "PID��-------������the wbp state��������������������������������������������----------42"
                                )
                                SYS = bytesTwo[1]!!
                                Log.e("WT", ".BTBatteryCopy...=$SYS")
                                if (SYS < 0) {
                                    SYS += 256
                                    SettingUtil.BTBattery = (bytesTwo[1]!! + 256) % 16
                                } else {
                                    SettingUtil.BTBattery = bytesTwo[1]!! % 16
                                }
                                Log.e("muk", "BTBattery:::" + SettingUtil.BTBattery)
                                val tempB: String = setValue(Integer.toBinaryString(SYS))
                                Log.e("muk", "byte(1):::$tempB")
                                val temp1 = tempB[3].toString() + ""
                                SettingUtil.bleState = temp1
                                val temp2 = tempB[5].toString() + "" + tempB[6] + "" + tempB[7] + ""
                                val temp3 = tempB[2].toString() + "" + tempB[1] + "" + tempB[0]
                                Log.e("lgc", "the state of the device:::$temp1")
                                Log.e(
                                    "lgc",
                                    "the work mode of the device:::" + Integer.valueOf(temp2, 2)
                                )
                                if (temp1 == "1") {
                                    SettingUtil.isSyncFinish = false
                                } else {
                                    SettingUtil.isSyncFinish = true
                                    iserror = false
                                }
                                Log.e("lgc", "isSyncFinish:::" + SettingUtil.isSyncFinish)
                                five = bytesTwo[2]!!
                                SettingUtil.tempVersion =
                                    Integer.toString(five / 16) + "." + Integer.toString(five % 16)
                                six = if (bytesTwo[4]!! < 0) bytesTwo[4]!! + 256 else bytesTwo[4]!!
                                val four =
                                    if (bytesTwo[5]!! < 0) bytesTwo[5]!! + 256 else bytesTwo[5]!!
                                five = if (bytesTwo[6]!! < 0) bytesTwo[6]!! + 256 else bytesTwo[6]!!
                                six = if (bytesTwo[7]!! < 0) bytesTwo[7]!! + 256 else bytesTwo[7]!!
                                val times = intArrayOf(six, four, five, six)
                                time = HomeUtil.BuleToTime(times)
                                Log.e("WT", "BTBattery....." + SettingUtil.BTBattery)
                                Log.e(
                                    "WT",
                                    "SettingUtil.tempVersion....." + SettingUtil.tempVersion
                                )
                                val t_data4 = byteArrayOf(-86, 4, 3, 42, 0, 0)
                                mBLE.writeCharacteristic_wbp(t_data4)
                                if (SettingUtil.isfirstLoad5 == 0) {
                                    Log.e("lgc", "..1...")
                                    val timer = Timer()
                                    timer.schedule(object : TimerTask() {
                                        override fun run() {
                                            getNowDateTime(mBLE)
                                        }
                                    }, 800L)
                                }
                                ++SettingUtil.isfirstLoad5
                                devstate = temp3
                                battey = SettingUtil.BTBattery
                                blestate = temp1
                                ver = SettingUtil.tempVersion
                            }
                            43 -> {
                                Log.e(
                                    "muk",
                                    "PID��-------sync��������������������������������������������������----------43"
                                )
                                Log.e(
                                    "muk",
                                    stateCount.toString() + "+++++stateCount------------------------currentCount++++" + currentCount
                                )
                                val byteData: String =
                                    byteToBit(bytesTwo[5]!!)
                                val tempError = byteData[3].toString() + ""
                                Log.e(
                                    "muk",
                                    "sync--------------------------tempError------����$tempError"
                                )
                                Log.e("muk", "bytesTwo.get(5)--------------" + bytesTwo[5])
                                Log.e("muk", "great---------------")
                                iserror = false
                                SYS = bytesTwo[6]!!
                                var dp = bytesTwo[7]!!
                                var hr = bytesTwo[9]!!
                                if (SYS < 0) {
                                    SYS += 256
                                }
                                if (dp < 0) {
                                    dp += 256
                                }
                                if (hr < 0) {
                                    hr += 256
                                }
                                val MeasureTime: String = getBleTestTime(
                                    bytesTwo[10]!!,
                                    bytesTwo[11]!!, bytesTwo[12]!!, bytesTwo[13]!!
                                )
                                val sycnBp = SycnBp()
                                sycnBp.dia = dp
                                sycnBp.sys = SYS
                                sycnBp.hr = hr
                                sycnBp.time = MeasureTime
                                Log.e(
                                    "muk",
                                    "sync������ the result of SYS-----------------SYSlow8=$SYS"
                                )
                                Log.e("muk", "sync������ -----------------DIA=$dp")
                                Log.e("muk", "sync������ the result of-----------------PR=$hr")
                                Log.e(
                                    "muk",
                                    "sync������ the result of-----------------MeasureTime=$MeasureTime"
                                )
                                bps.add(sycnBp)
                                val str1 = byteData[1].toString() + ""
                                val str2 = byteData[2].toString() + ""
                                val str3 = str1 + str2
                                val user = Integer.valueOf(str3, 2)
                                Log.e("muk", ",,,,,,,,,,,,,,,,,,,,,,user,$user")
                                val FamilyId = true
                                if (SYS != 0 && dp != 0 && hr != 0) {
                                    if (SettingUtil.isGusetmode) {
                                        SettingUtil.Sbp = SYS
                                        SettingUtil.Dbp = bytesTwo[7]!!
                                        SettingUtil.Hr = bytesTwo[9]!!
                                    } else if (user != 1 && user != 2) {
                                        SettingUtil.Sbp = SYS
                                        SettingUtil.Dbp = dp
                                        SettingUtil.Hr = hr
                                        SettingUtil.guestBpTime = MeasureTime
                                    } else {
                                        Log.e(
                                            "muk",
                                            "--------------------------------------sync-----------userID------------------------------------$user"
                                        )
                                        val var59 = MyDateUtil.getDateFormatToString(null)
                                    }
                                }
                                if (stateCount == currentCount) {
                                    Log.e(
                                        "muk",
                                        "--------------------------------------finish sync-------nice------------------------------------beforeErrorCount++++" + beforeErrorCount
                                    )
                                    if (beforeErrorCount != 0L) {
                                        Log.e(
                                            "muk",
                                            "--------------------------------------ͬ��-���ڴ����ط���ʷ����--------����-------------------------------------"
                                        )
                                        if (SettingUtil.resendData) {
                                            if (SampleGattAttributes.listdata.size > 0) {
                                                SampleGattAttributes.listdata.removeAt(0)
                                            }
                                            if (SampleGattAttributes.listdata.size > 0) {
                                                Log.e(
                                                    "muk",
                                                    "--------------------------------------ͬ��-����������ʷ����--------------����--------------------------------"
                                                )
                                              reSendData(mBLE)
                                            } else {
                                                syncFinish(mBLE)
                                            }
                                        } else {
                                            reSendData(mBLE)
                                        }
                                    } else {
                                        syncFinish(mBLE)
                                    }
                                }
                            }
                            44 -> {
                                Log.e("muk", "PID��------Ѫѹͬ�����ݿ�ʼ״̬��-----------44")
                                Log.e("muk", "PID��-----------\t1------44=====" + bytesTwo[1])
                                Log.e("muk", "PID��----------2-------44=====" + bytesTwo[2])
                                Log.e("muk", "PID��-----------\t3------44=====" + bytesTwo[3])
                                Log.e("muk", "PID��----------4-------44=====" + bytesTwo[4])
                                val a1 = bytesTwo[3]!!
                                val a2 = bytesTwo[4]!!
                                stateCount = a1 + a2
                                Log.e("stateCount", "����ʲô.......����" + stateCount)
                                val t1: String = this.setValue(Integer.toBinaryString(a1))
                                val t2: String = this.setValue(Integer.toBinaryString(a2))
                                Log.e("muk", "PID��--------------t1--������=====$t1")
                                Log.e("muk", "PID��---------------t2-������=====$t2")
                                Log.e(
                                    "muk",
                                    "PID��---------------stateCount-������== ���ݰ�������Ϊ===" + stateCount
                                )
                                val t_data2 = byteArrayOf(-86, 4, 3, 44, 0, 0)
                                mBLE.writeCharacteristic_wbp(t_data2)
                            }
                            45 -> {
                                val Data_blockCount = bytesTwo[1]!! + bytesTwo[2]!!
                                if (Data_blockCount > 0) {
                                    if (SettingUtil.bleState == "0") {
                                        if (SettingUtil.isfirstLoad6 == 0) {
                                            val timer = Timer()
                                            timer.schedule(object : TimerTask() {
                                                override fun run() {
                                                    Log.e("muk", "=====��--------����ʼ������ʷ����")
                                                    mBLE.writeCharacteristic_wbp(
                                                        SampleGattAttributes.data4
                                                    )
                                                }
                                            }, 600L)
                                        }
                                        ++SettingUtil.isfirstLoad6
                                        mBLE.writeCharacteristic_wbp(SampleGattAttributes.data4)
                                    }
                                } else {
                                    SettingUtil.isSyncFinish = true
                                    Log.e(
                                        "muk",
                                        "=====Ѫѹ�洢״̬��(ID:45)--------stateCount++++" + stateCount
                                    )
                                    if (stateCount != 0) {
                                        if (SettingUtil.isfirstLoad10 == 0) {
                                            Log.e(
                                                "muk",
                                                "=====Ѫѹ�洢״̬��(ID:45)--------ͬ�����++++-----------"
                                            )
                                        }
                                        ++SettingUtil.isfirstLoad10
                                    } else {
                                        Log.e(
                                            "muk",
                                            "=====Ѫѹ�洢״̬��(ID:45)--------û�����ݰ�++++----------"
                                        )
                                    }
                                }
                            }
                            49 -> {
                                Log.e("muk", "PID��-----------------49")
                                Log.e(
                                    "muk",
                                    "WBPMODE-----------------::" + WBPMODE
                                )
                                Log.e("muk", "����ģʽ----------------::" + SettingUtil.isGusetmode)
                                if (!SettingUtil.isGusetmode && (!SettingUtil.isTest || SettingUtil.isFirstopenBle)) {
                                    SettingUtil.isFirstopenBle = false
                                    user_mode = bytesTwo[1]!!
                                    Log.e("user_mode::", "" + user_mode)
                                    data = byteArrayOf(-86, 4, 3, 49, 0, 0)
                                    mBLE.writeCharacteristic_wbp(data)
                                }
                            }
                        }
                    }
                }
                j++
            }

            Log.e("shani", "����������data��������������������$tstr")
            bpDataList.clear()
            onWBPDataListener!!.onMeasurementBp(mesurebp)
            onWBPDataListener!!.onMeasurementfin(endsys, enddia, endhr, guest)
            onWBPDataListener!!.onErroer(obStr)
            onWBPDataListener!!.onState(battey, blestate, ver, devstate)
            onWBPDataListener!!.onSycnBp(bps)
            onWBPDataListener!!.onTime(time)
            onWBPDataListener!!.onUser(user_mode)
        } catch (var48: Exception) {
            var48.printStackTrace()
            bpDataList.clear()
            var48.printStackTrace()
        }
    }

    override fun resolveALiBPData(var1: ByteArray?, var2: Context?) {
        Log.e("���ݰ�����", var1?.size.toString() + "")
        if (var1?.size!! > 0) {
            when (var1.size) {
                1 -> {
                    val battery: Int = byte2Int(var1[0])
                    Log.e("����Ϊ", battery.toString() + "")
                    if (battery in 1..25) {
                        SettingUtil.BTBattery = 0
                    } else if (battery in 26..50) {
                        SettingUtil.BTBattery = 1
                    } else if (battery in 51..75) {
                        SettingUtil.BTBattery = 2
                    } else {
                        SettingUtil.BTBattery = 4
                    }
                }
                12 -> this.resolveRealTimeData(var1)
                19 -> {
                    if (realDataTimer != null) {
                        realDataTimer!!.cancel()
                        realDataTimer = null
                    }
                    this.resolveBleFinalData(var1, var2!!)
                }
            }
        }
    }

    private fun resolveRealTimeData(datas: ByteArray) {
        val Pr_low = byte2Int(datas[7])
        val Pr_high = byte2Int(datas[8])
        val user = byte2Int(datas[9])
        val PrValue = (Pr_high shl 8) + Pr_low
        val var10000 = byteArrayOf(datas[11], datas[10])
        if (WBPMODE == 1 && !SettingUtil.isGusetmode && (!SettingUtil.isTest || SettingUtil.isFirstopenBle)) {
            SettingUtil.isFirstopenBle = false
            Log.e("���ڲ������û�", "" + user)
        }
        if ((datas[11].toInt() shl 8) + datas[10] != 0) {
            Log.e("����", "�������̳���")
        } else {
            if ((datas[11].toInt() shl 8) + datas[10] == 0) {
                val tempL_low = byte2Int(datas[1])
                val tempL_high = byte2Int(datas[2])
                val tempLValue = (tempL_high shl 8) + tempL_low
                Log.e("mye", "���ѹ----$tempLValue, ���ڲ������û�: $user")
                SettingUtil.isNowTestBp = false
                SettingUtil.isNowTestBpFinish = false
            }
            SettingUtil.isSyncFinish = true
            realDataTimer = Timer()
            val task: TimerTask = object : TimerTask() {
                override fun run() {
                    Log.e("��������", "------")
                    SettingUtil.isSyncFinish = true
                    ResolveWbp.iserror = false
                }
            }
            realDataTimer!!.schedule(task, 1000L)
        }
    }

    private fun byte2Int(data: Byte): Int {
        return if (data < 0) data + 256 else data.toInt()
    }

    private fun resolveBleFinalData(datas: ByteArray, context: Context) {
        Log.e("muk", "PID��-----------------41")
        SettingUtil.isNowTestBpFinish = true
        val Sys_Low = byte2Int(datas[1])
        val Sys_high = byte2Int(datas[2])
        val Dia_low = byte2Int(datas[3])
        val Dia_high = byte2Int(datas[4])
        val Mean_low = byte2Int(datas[5])
        val Mean_high = byte2Int(datas[6])
        val Year_low = byte2Int(datas[7])
        val Year_high = byte2Int(datas[8])
        val mon = byte2Int(datas[9])
        val day = byte2Int(datas[10])
        val Hour = byte2Int(datas[11])
        val Min = byte2Int(datas[12])
        val Sec = byte2Int(datas[13])
        val Pr_low = byte2Int(datas[14])
        val Pr_high = byte2Int(datas[15])
        byte2Int(datas[16])
        val SysValue = (Sys_high shl 8) + Sys_Low
        val DiaValue = (Dia_high shl 8) + Dia_low
        val MeanValue = (Mean_high shl 8) + Mean_low
        val PrValue = (Pr_high shl 8) + Pr_low
        val dateStr: String =
            (Year_high shl 8).toString() + Year_low.toString() + "-" + (if (mon > 9) mon else "0$mon") + "-" + (if (day > 9) day else "0$day") + " " + (if (Hour > 9) Hour else "0$Hour") + ":" + (if (Min > 9) Min else "0$Min") + ":" + if (Sec > 9) Sec else "0$Sec"
        Log.e(
            "�������:",
            "�û�: , sys: $SysValue, dia: $DiaValue, mean: $MeanValue, pr: $PrValue, time: $dateStr"
        )
        val status = byteArrayOf(datas[18], datas[17])
        Log.e(
            "����״̬",
            StringBuffer(StringUtill.hexString2binaryString(StringUtill.bytesToHexString(status)))
                .reverse().toString() + ""
        )
        iserror = false
        if ((datas[18].toInt() shl 8) + datas[17] == 0) {
            if (SysValue != 0 && DiaValue != 0 && PrValue != 0) {
                if (SettingUtil.isGusetmode && !SettingUtil.isGusetmode) {
                    if (SettingUtil.testType == 0) {
                        val var25 = MyDateUtil.getDateFormatToString(null)
                    }
                } else if (SettingUtil.isGusetmode) {
                    SettingUtil.Sbp = SysValue
                    SettingUtil.Dbp = DiaValue
                    SettingUtil.Hr = PrValue
                }
            } else {
               iserror = true
            }
        } else {
            iserror = true
            obStr = getALiErrorTip(status, context)
        }
        Log.e("mgf", "----------------������ʾr------------2------------" + obStr)
    }

    private fun getALiErrorTip(status: ByteArray, context: Context): Any {
        val errBinStr = StringUtill.hexString2binaryString(StringUtill.bytesToHexString(status))
        val binStr = StringBuffer(errBinStr).reverse().toString()
        val obStr: Any = "0"
        if (binStr.startsWith("1")) {
//            obStr = context.getString(string.ble_test_error7);
        } else if (binStr.startsWith("1", 1)) {
//            obStr = context.getString(string.ble_test_error2);
        } else if (binStr.startsWith("1", 2)) {
//            obStr = context.getString(string.ble_test_error18);
        } else if (binStr.startsWith("01", 3)) {
//            obStr = context.getString(string.ble_test_error19);
        } else if (binStr.startsWith("10", 3)) {
//            obStr = context.getString(string.ble_test_error20);
        } else {
//            obStr = context.getString(string.ble_test_error14);
        }
        return obStr
    }

    override fun setWBPDataListener(var1: OnWBPDataListener?) {
        onWBPDataListener = var1
    }

    override fun getNowDateTime(var1: BluetoothLeClass?) {
        Log.e("lgc", "�·�ʱ��2....")

        try {
            val nowdata = MyDateUtil.getDateFormatToString(null)
            val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            val now = df.parse(nowdata)
            val date = df.parse("2000-01-01 00:00:00")
            val l = now.time - date.time
            val day = l / 86400000L
            val hour = l / 3600000L - day * 24L
            val min = l / 60000L - day * 24L * 60L - hour * 60L
            val s = l / 1000L - day * 24L * 60L * 60L - hour * 60L * 60L - min * 60L
            val alltime = (day * 24L * 60L * 60L + hour * 60L * 60L + min * 60L + s).toInt()
            Log.e("muk", "..��ǰ��ʱ�� ��alltime��..+$alltime")
            val str: String = print(alltime)
            var str1 = ""
            var str2 = ""
            var str3 = ""
            var str4 = ""
            var i: Int
            i = 0
            while (i < str.length) {
                if (i < 8) {
                    str1 = str1 + str[i] + ""
                } else if (i < 16 && i >= 8) {
                    str2 = str2 + str[i] + ""
                } else if (i < 24 && i >= 16) {
                    str3 = str3 + str[i] + ""
                } else {
                    str4 = str4 + str[i] + ""
                }
                ++i
            }
            i = Integer.valueOf(str4, 2)
            val temp4 = i.toByte()
            val b = Integer.valueOf(str3, 2)
            val temp3 = b.toByte()
            val c = Integer.valueOf(str2, 2)
            val temp2 = c.toByte()
            val d = Integer.valueOf(str1, 2)
            val temp1 = d.toByte()
            Log.e("muk", "..��ǰ��ʱ�� ��tempB4��..+$temp4-$temp3-$temp2-$temp1")
            val data6 = byteArrayOf(-86, 8, 107, 0, temp4, temp3, temp2, temp1, 0, 0)
            var1?.writeCharacteristic_wbp(data6)
        } catch (var31: Exception) {
            var31.printStackTrace()
        }

    }

    override fun SendForAll(var1: BluetoothLeClass?) {
        var1?.writeCharacteristic_wbp(SampleGattAttributes.data7)
    }

    override fun onSingleCommand(var1: BluetoothLeClass?) {
        var1?.writeCharacteristic_wbp(SampleGattAttributes.data)
    }

    override fun onStopBleCommand(var1: BluetoothLeClass?) {
        var1?.writeCharacteristic_wbp(SampleGattAttributes.data2)
    }

    override fun sendUserInfoToBle(var1: BluetoothLeClass?) {
        if (var1 != null) {
            Log.e("muk", "�·�����...�û�:::" + SettingUtil.userModeSelect)
            val data: ByteArray
            if (SettingUtil.userModeSelect == 1) {
                data = byteArrayOf(-86, 4, -128, 1, 0, 0)
                var1.writeCharacteristic_wbp(data)
            } else if (SettingUtil.userModeSelect == 2) {
                data = byteArrayOf(-86, 4, -128, 2, 0, 0)
                var1.writeCharacteristic_wbp(data)
            }
        }
    }

    private fun finishToResendData(mBLE: BluetoothLeClass) {
        SettingUtil.resendData = false
        SettingUtil.isHaveData = false
        currentCount = 0
        beforeErrorCount = 0L
        if (timerResendData != null) {
            timerResendData!!.cancel()
            timerResendData = null
        }
        Log.e(
            "muk",
            "--------------------������ͬ��---�����ط�---�����ˡ�-------------------------------------------------"
        )
        val data6 = byteArrayOf(-86, 4, 127, 1, 0, 0)
        mBLE.writeCharacteristic_wbp(data6)
    }

    private fun getErrorTip(bytesTwo: MutableList<Int>): Any {
        val obStr: Any = "0"
        if (bytesTwo[2] != 7 && bytesTwo[2] != 14) {
            if (bytesTwo[2] != 6 && bytesTwo[2] != 20) {
                if (bytesTwo[2] != 2 && bytesTwo[2] != 8 && bytesTwo[2] != 10 && bytesTwo[2] != 12 && bytesTwo[2] != 15) {
                    if (bytesTwo[2] != 11 && bytesTwo[2] != 13) {
                        if (bytesTwo[2] != 9 && bytesTwo[2] != 19) {
//                            obStr = activity.getApplication().getString(string.ble_test_error14);
                        } else {
//                            obStr = activity.getApplication().getString(string.ble_test_error5);
                        }
                    } else {
//                        obStr = activity.getApplication().getString(string.ble_test_error7);
                    }
                } else {
//                    obStr = activity.getApplication().getString(string.ble_test_error6);
                }
            } else {
//                obStr = activity.getApplication().getString(string.ble_test_error2);
            }
        } else {
//            obStr = activity.getApplication().getString(string.ble_test_error3);
        }
        return obStr
    }

    private fun setValue(data: String): String {
        val len = data.length
        val tempDate: String
        Log.e("muk", len.toString() + "data...." + data)
        tempDate = when (len) {
            1 -> "0000000$data"
            2 -> "000000$data"
            3 -> "00000$data"
            4 -> "0000$data"
            5 -> "000$data"
            6 -> "00$data"
            7 -> "0$data"
            else -> data
        }
        return tempDate
    }

    private fun getBleTestTime(b1: Int, b2: Int, b3: Int, b4: Int): String {
        var strNowTime: String
        try {
            var a1 = b1
            var a2 = b2
            var a3 = b3
            var a4 = b4
            if (b1 < 0) {
                a1 = b1 + 256
            }
            if (b2 < 0) {
                a2 = b2 + 256
            }
            if (b3 < 0) {
                a3 = b3 + 256
            }
            if (b4 < 0) {
                a4 = b4 + 256
            }
            val t1 = this.setValue(Integer.toBinaryString(a1))
            val t2 = this.setValue(Integer.toBinaryString(a2))
            val t3 = this.setValue(Integer.toBinaryString(a3))
            val t4 = this.setValue(Integer.toBinaryString(a4))
            val temp4 = t4 + t3 + t2 + t1
            val src1 = BigInteger(temp4, 2)
            val millSecond = src1.toString()
            val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            val date = df.parse("2000-01-01 00:00:00")
            val datet = date.time
            val time = millSecond.toLong()
            val tempt = time * 1000L
            val nowTime = tempt + datet
            val d = Date(nowTime)
            strNowTime = df.format(d)
            Log.e("muk", "now time������" + df.format(d))
        } catch (var28: Exception) {
            var28.printStackTrace()
            strNowTime = MyDateUtil.getDateFormatToString(null)
        }
        return strNowTime
    }

    private fun reSendData(mBLE: BluetoothLeClass) {
        i = 0
        timerResendData = Timer()
        timerResendData!!.schedule(object : TimerTask() {
            override fun run() {
                if (i == 3 && !SettingUtil.isHaveData) {
                    finishToResendData(mBLE)
                    SettingUtil.isHaveData = false
                    timerResendData!!.cancel()
                    timerResendData = null
                } else {
                    SettingUtil.resendData = true
                    mBLE.writeCharacteristic_wbp(
                        SampleGattAttributes.resendBleData(
                            SampleGattAttributes.listdata[0].byte1,
                            SampleGattAttributes.listdata[0].byte2,
                            SampleGattAttributes.listdata[0].byte3,
                            SampleGattAttributes.listdata[0].byte4
                        )
                    )
                }
                i++
            }
        }, 0L, 1000L)
    }

    private fun syncFinish(mBLE: BluetoothLeClass) {
        Log.e(
            "muk",
            "--------------------------------------ͬ��-û�д���,���ͽ���-------------nice---------------------------------"
        )
        SettingUtil.resendData = false
        SettingUtil.isHaveData = false
        currentCount = 0
        beforeErrorCount = 0L
        mBLE.writeCharacteristic_wbp(SampleGattAttributes.data6)
        val timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                Log.e("muk", "syncDataByBle-------ͬ�����ݿ�ʼ-��ѯѪѹ��״̬-------")
                mBLE.writeCharacteristic_wbp(SampleGattAttributes.data7)
            }
        }, 1000L)
    }

    private fun print(i: Int): String {
        var str = ""
        for (j in 31 downTo 0) {
            str = if (1 shl j and i != 0) {
                str + "1"
            } else {
                str + "0"
            }
        }
        return str
    }

    interface OnWBPDataListener {
        fun onMeasurementBp(var1: Int)
        fun onMeasurementfin(var1: Int, var2: Int, var3: Int, var4: Boolean?)
        fun onErroer(var1: Any?)
        fun onState(var1: Int, var2: String?, var3: String?, var4: String?)
        fun onSycnBp(var1: ArrayList<SycnBp?>?)
        fun onTime(var1: String?)
        fun onUser(var1: Int)
    }
}