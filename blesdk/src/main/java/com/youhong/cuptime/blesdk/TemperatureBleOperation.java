package com.youhong.cuptime.blesdk;

import android.content.Context;
import android.content.Intent;

import java.util.Calendar;

/**
 * Created by Administrator on 2017/11/3.
 */

@Deprecated
public class TemperatureBleOperation extends BaseBleOperation {
    public TemperatureBleOperation(Context context) {
        super(context);
    }

    /**
     * Callbacks
     **/

    public void setNotifyTemperatureCallback(NotifyTemperatureCallback mNotifyTemperatureCallback) {
        this.mNotifyTemperatureCallback = mNotifyTemperatureCallback;
    }

    NotifyTemperatureCallback mNotifyTemperatureCallback = null;
    GetTimeCallback mGetTimeCallback = null;
    GetHistoryTemperatureCallback mGetHistoryTemperatureCallback = null;
    GetBatteryCallback mGetBatteryCallback = null;
    GetModeCallback mGetModeCallback = null;
    GetBarrierCallback mGetBarrierCallback = null;

    @Override
    protected void Action_RX_data(Context context, Intent intent) {

        byte[] values = intent.getByteArrayExtra(LightBLEService.EXTRA_DATA);

        if (values[0] == 0x48) {

            if (mNotifyTemperatureCallback != null) {

                Calendar c = Calendar.getInstance();
                c.set(Calendar.MILLISECOND, 0);

                c.set(Calendar.YEAR, Util.ConvertBCD2Decimal(values[1]) + 2000);
                c.set(Calendar.MONTH, Util.ConvertBCD2Decimal(values[2]) - 1);
                c.set(Calendar.DAY_OF_MONTH, Util.ConvertBCD2Decimal(values[3]));
                c.set(Calendar.HOUR_OF_DAY, Util.ConvertBCD2Decimal(values[4]));
                c.set(Calendar.MINUTE, Util.ConvertBCD2Decimal(values[5]));
                c.set(Calendar.SECOND, Util.ConvertBCD2Decimal(values[6]));

                byte mode = values[7];
                int temp1 = ((values[0x9] & 0xFF) + ((values[0x8] << 8) & 0xFF00));
                float temp2 = ((float) (temp1)) / 10f;

                if (mNotifyTemperatureCallback != null) {

                    mNotifyTemperatureCallback.onNotifyTemperature(c, mode, temp2);

                }

            }
        } else if (values[0] == 0x41) {
            Calendar c = Calendar.getInstance();
            c.set(Calendar.MILLISECOND, 0);

            c.set(Calendar.YEAR, Util.ConvertBCD2Decimal(values[1]) + 2000);
            c.set(Calendar.MONTH, Util.ConvertBCD2Decimal(values[2]) - 1);
            c.set(Calendar.DAY_OF_MONTH, Util.ConvertBCD2Decimal(values[3]));
            c.set(Calendar.HOUR_OF_DAY, Util.ConvertBCD2Decimal(values[4]));
            c.set(Calendar.MINUTE, Util.ConvertBCD2Decimal(values[5]));
            c.set(Calendar.SECOND, Util.ConvertBCD2Decimal(values[6]));

            if (mGetTimeCallback != null) {

                mGetTimeCallback.onGetTime(c);
            }

        } else if (values[0] == 0x07) {


            if (values[3] == (byte) 0xAA &&
                    values[4] == (byte) 0xAA &&
                    values[5] == (byte) 0xAA &&
                    values[6] == (byte) 0xAA &&
                    values[7] == (byte) 0xAA
                    ) {

                //If mode equals to 0xAA, means this data is ending data.
                mGetHistoryTemperatureCallback.onGetTemperature(Calendar.getInstance(),
                        -1, 0f);

            } else {


                Calendar c = Calendar.getInstance();
                c.set(Calendar.MILLISECOND, 0);


                c.set(Calendar.YEAR, Util.ConvertBCD2Decimal(values[1]) + 2000);
                c.set(Calendar.MONTH, Util.ConvertBCD2Decimal(values[2]) - 1);
                c.set(Calendar.DAY_OF_MONTH, Util.ConvertBCD2Decimal(values[3]));
                c.set(Calendar.HOUR_OF_DAY, Util.ConvertBCD2Decimal(values[4]));
                c.set(Calendar.MINUTE, Util.ConvertBCD2Decimal(values[5]));
                c.set(Calendar.SECOND, Util.ConvertBCD2Decimal(values[6]));

                byte mode = values[7];

                int temp1 = ((values[0x11] & 0xFF) + ((values[0xA] << 8) & 0xFF00));
                float temp2 = ((float) (temp1)) / 10f;

                mGetHistoryTemperatureCallback.onGetTemperature(c, mode, temp2);
            }

        } else if (values[0] == 0x45) {

            if (mGetModeCallback != null) {

                mGetModeCallback.onGetMode(values[1], values[2]);

            }

        } else if (values[0] == 0x28) {

            if (mGetBarrierCallback != null) {

                int lowLimit = (values[3] & 0xFF) + ((values[2] << 8) & 0xFF00);
                int highLimit = (values[5] & 0xFF) + ((values[4] << 8) & 0xFF00);
                mGetBarrierCallback.onGetBarrier(values[1], highLimit, lowLimit);

            }
        } else if (values[0] == 0x13) {

            if (mGetBatteryCallback != null) {

                mGetBatteryCallback.onGetBattery(values[1]);
            }


        }
    }

    public void setTime(Calendar c) {
        byte[] values = new byte[16];

        values[0] = 0x1;
        byte year = Util.ConvertDecimal2BCD((byte) (c.get(Calendar.YEAR) - 2000));
        byte month = Util.ConvertDecimal2BCD((byte) (c.get(Calendar.MONTH) + 1));
        byte day = Util.ConvertDecimal2BCD((byte) (c.get(Calendar.DAY_OF_MONTH) + 1));
        byte hour = Util.ConvertDecimal2BCD((byte) (c.get(Calendar.HOUR_OF_DAY)));
        byte mins = Util.ConvertDecimal2BCD((byte) (c.get(Calendar.MINUTE)));
        byte seconds = Util.ConvertDecimal2BCD((byte) (c.get(Calendar.SECOND)));

        values[1] = year;
        values[2] = month;
        values[3] = day;
        values[4] = hour;
        values[5] = mins;
        values[6] = seconds;
        values[15] = crc(values);

        super.writeTx(values);
    }

    public void getTime(GetTimeCallback callback) {
        mGetTimeCallback = callback;
        byte[] values = new byte[16];
        values[0] = 0x41;
        values[15] = crc(values);
        super.writeTx(values);
    }

    public void getHistoryTemperature(GetHistoryTemperatureCallback callback, Calendar startTime) {

        mGetHistoryTemperatureCallback = callback;

        byte[] values = new byte[16];

        values[0] = 0x7;
        byte year = Util.ConvertDecimal2BCD((byte) (startTime.get(Calendar.YEAR) - 2000));
        byte month = Util.ConvertDecimal2BCD((byte) (startTime.get(Calendar.MONTH) + 1));
        byte day = Util.ConvertDecimal2BCD((byte) (startTime.get(Calendar.DAY_OF_MONTH) + 1));
        byte hour = Util.ConvertDecimal2BCD((byte) (startTime.get(Calendar.HOUR_OF_DAY)));
        byte mins = Util.ConvertDecimal2BCD((byte) (startTime.get(Calendar.MINUTE)));
        byte seconds = Util.ConvertDecimal2BCD((byte) (startTime.get(Calendar.SECOND)));

        values[1] = year;
        values[2] = month;
        values[3] = day;
        values[4] = hour;
        values[5] = mins;
        values[6] = seconds;

        values[15] = crc(values);
        super.writeTx(values);

    }

    public void getBattery(GetBatteryCallback callback) {

        mGetBatteryCallback = callback;

        byte[] values = new byte[16];
        values[0] = 0x13;
        values[15] = crc(values);
        super.writeTx(values);

    }

    public void factoryReset() {

        byte[] values = new byte[16];
        values[0] = 0x12;

        values[15] = crc(values);
        super.writeTx(values);
    }

    public void mcuReset() {

        byte[] values = new byte[16];
        values[0] = 0x2E;
        values[15] = crc(values);
        super.writeTx(values);
    }

    public void setMode(byte power, byte mode) {
        byte[] values = new byte[16];
        values[0] = 0x25;
        values[1] = power;
        values[2] = mode;
        values[15] = crc(values);
        super.writeTx(values);
    }

    public void getMode(GetModeCallback callback) {

        mGetModeCallback = callback;

        byte[] values = new byte[16];
        values[0] = 0x45;
        values[15] = 0x45;
        super.writeTx(values);

    }

    /*AA  =  01,体温模式
      AA  =  02,液温模式
     */

    public void setBarrier(byte mode, int low, int high) {

        byte[] values = new byte[16];
        values[0] = 0x08;
        values[1] = mode;
        values[2] = (byte) ((low >> 8) & 0xFF);
        values[3] = (byte) (low & 0xFF);
        values[4] = (byte) ((low >> 8) & 0xFF);
        values[5] = (byte) (low & 0xFF);
        values[15] = crc(values);
        super.writeTx(values);
    }

    public void getBarrier(GetBarrierCallback callback, byte mode) {

        mGetBarrierCallback = callback;

        byte[] values = new byte[16];
        values[0] = 0x08;
        values[1] = mode;
        values[15] = crc(values);
        super.writeTx(values);
    }

    private byte crc(byte[] values) {

        byte result = 0x0;

        for (int i = 0; i < values.length - 1; i++) {

            result += values[i];

        }
        return result;
    }

    public interface GetTimeCallback {

        void onGetTime(Calendar c);

    }

    public interface NotifyTemperatureCallback {
        // mode ,1:Temperature Mode， 2：fluid temperature
        void onNotifyTemperature(Calendar c, int mode, float temperature);

    }

    public interface GetHistoryTemperatureCallback {

        void onGetTemperature(Calendar c, int mode, float temperature);
    }

    public interface GetModeCallback {
        /**
         * AA为工作状态  AA=0 关机  AA=1 开机
         * BB为功能模式，总共2种模式：
         * BB=  01，体温模式
         * BB=  02,液温模式
         **/
        void onGetMode(byte power, byte mode);
    }

    public interface GetBatteryCallback {
        /* 0代表0%电量，
           1代表20%电量
           2代表40%电量，
           3代表60%电量
           4代表80%电量，
           5代表100%电量*/
        void onGetBattery(int level);
    }


    public interface GetBarrierCallback {

        void onGetBarrier(int mode, int highLimit, int lowLimit);
    }
}
