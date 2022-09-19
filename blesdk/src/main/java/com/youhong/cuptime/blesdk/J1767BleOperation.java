package com.youhong.cuptime.blesdk;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Arrays;
import java.util.Calendar;

/**
 * Created by Administrator on 2018/1/3 0003.
 */

public class J1767BleOperation extends BaseBleOperation {

    public J1767BleOperation(Context context) {
        super(context);
    }

    private static final int SET_TIME = 0x1;
    private static final int GET_TIME = 0x41;
    private static final int GET_USER_INFO = 0x42;
    private static final int SET_USER_INFO = 0x02;
    private static final int GET_VERSION = 0x27;
    private static final int ENABLE_RT_STEP = 0x9;
    private static final int DISABLE_RT_STEP = 0xA;
    private static final int SET_DEVICE_ID = 0x5;
    private static final int GET_BATTERY = 0x13;
    private static final int GET_MAC = 0x22;
    private static final int RESTART_DEVICE = 0x2E;
    private static final int ENABLE_LOW_BATTERY_REPORT = 0x1A;
    private static final int ENABLE_HR_REPORT_WHILE_ECG_TEST = 0x11;
    private static final int ENABLE_RT_ECG_AND_PPG = 0x16;
    private static final int ENABLE_RT_ECG_AND_PPG_STATUS = 0x96;
    private static final int SET_AUTOMATIC_ECG_AND_PPG_TMPER_BATTERY_MEASUREMENT = 0x37;
    private static final int GET_AUTOMATIC_ECG_AND_PPG_TMPER_BATTERY_MEASUREMENT = 0x39;
    private static final int GET_AUTOMATIC_ECG_AND_PPG_HISTORY_DATA_SECTION_HEADER = 0x30;
    private static final int GET_AUTOMATIC_ECG_AND_PPG_HISTORY_DATA_SECTION_DATA = 0x31;
    private static final int AUTOMATIC_ECG_AND_PPG_MEASUREMENT_FINISH_REPORT = 0x57;
    private static final int DELETE_AUTOMATIC_ECG_AND_PPG_HISTORY_DATA = 0x49;

    private static final int CHECK_BODY_POSITION = 0x51;
    private static final int GET_BODY_POSITION = 0x53;
    private static final int CHECK_IS_BODY_TOUCHED = 0x54;
    private static final int BODY_TOUCH_REPORT = 0x56;
    private static final int GET_TOTAL_SPORT_DATA_ON_SPECIFIC_DAY = 0x61;
    private static final int GET_DETAIL_SPORT_DATA_ON_SPECIFIC_DAY = 0x62;
    public static final int ENABLE_RT_TEMPERATURE_MESSUREMENT = 0x1C;
    public static final int XYZ_DATA = 0x17;

    @Override
    protected void Action_RX_data(Context context, Intent intent) {

        final byte[] data = intent.getByteArrayExtra(LightBLEService.EXTRA_DATA);
       // Log.e("data", Arrays.toString(data));

//        if (data[0] == (byte) XYZ_DATA) {
//            final int xyz_data = (data[1] & 0xFF) + (data[3] & 0xFF) + (data[5] & 0xFF);
//            Log.e("data", String.valueOf(xyz_data));
//        }
         if (data[0] == (byte) SET_TIME) {

            if (setTimeCallback != null) {

                super.handler.post(new Runnable() {
                    @Override
                    public void run() {

                        setTimeCallback.onCallback();

                    }
                });
            }
        } else if (data[0] == (byte) GET_TIME) {

            int year = 0, month = 0, day = 0, hour = 0, minute = 0, second = 0;
            year = Util.ConvertBCD2Decimal(data[1]) + 2010;
            month = Util.ConvertBCD2Decimal(data[2]) - 1;
            day = Util.ConvertBCD2Decimal(data[3]);
            hour = Util.ConvertBCD2Decimal(data[4]);
            minute = Util.ConvertBCD2Decimal(data[5]);
            second = Util.ConvertBCD2Decimal(data[6]);

            final Calendar c = Calendar.getInstance();
            c.set(year, month, day, hour, minute, second);

            if (getTimeCallback != null) {
                super.handler.post(new Runnable() {
                    @Override
                    public void run() {
                        getTimeCallback.onCallback(c);
                    }
                });
            }

        } else if (data[0] == (byte) GET_USER_INFO) {

            final int gender = data[1] & 0xFF;
            final int age = data[2] & 0xFF;
            final int height = data[3] & 0xFF;
            final int weight = data[4] & 0xFF;
            final int stepWidth = data[5] & 0xFF;

            final byte id1 = data[6];
            final byte id2 = data[7];
            final byte id3 = data[8];
            final byte id4 = data[9];
            final byte id5 = data[10];
            final byte id6 = data[11];

            if (getUserInfoCallback != null) {
                super.handler.post(new Runnable() {
                    @Override
                    public void run() {
                        getUserInfoCallback.onCallback(gender, age, height, weight
                                , stepWidth, id1, id2, id3, id4, id5, id6);
                    }
                });
            }


        } else if (data[0] == (byte) SET_USER_INFO) {

            if (setUserInfoCallback != null) {
                super.handler.post(new Runnable() {
                    @Override
                    public void run() {
                        setUserInfoCallback.onCallback();
                    }
                });
            }

        } else if (data[0] == (byte) ENABLE_RT_STEP) {

            final int step = ((data[1] << 16) & 0xFF0000) + ((data[2] << 8) & 0xFF00) + (data[3] & 0xFF);
            final int calories = ((data[7] << 16) & 0xFF0000) + ((data[8] << 8) & 0xFF00) + (data[9] & 0xFF);
            final int distance = ((data[10] << 16) & 0xFF0000) + ((data[11] << 8) & 0xFF00) + (data[12] & 0xFF);
            final int walkTime = ((data[13] << 8) & 0xFF00) + (data[14] & 0xFF);

            if (enableRTStepCallback != null) {
                super.handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (step == 00 && calories == 0 && distance == 0 && walkTime == 0) {
                            enableRTStepCallback.onCallback();

                        } else {
                            enableRTStepCallback.onDataCallback(step, calories, distance, walkTime);
                        }
                    }
                });


            }


        } else if (data[0] == (byte) DISABLE_RT_STEP) {

            if (disableRTStepCallback != null) {
                super.handler.post(new Runnable() {
                    @Override
                    public void run() {
                        disableRTStepCallback.onCallback();

                    }
                });
            }

        } else if (data[0] == (byte) SET_DEVICE_ID) {

            if (setDeviceIdCallback != null) {
                super.handler.post(new Runnable() {
                    @Override
                    public void run() {
                        setDeviceIdCallback.onCallback();

                    }
                });
            }


        } else if (data[0] == (byte) GET_BATTERY) {

            final int batteryLevel = data[1] & 0xFF;
            final int batteryVoltage = ((data[3] << 8) & 0xFF00) + (data[4] & 0xFF);

            if (getBatteryLevelCallback != null) {
                super.handler.post(new Runnable() {
                    @Override
                    public void run() {

                        getBatteryLevelCallback.onCallback(batteryLevel
                                , batteryVoltage);
                    }
                });

            }

        } else if (data[0] == (byte) GET_MAC) {

            final byte mac1 = data[1];
            final byte mac2 = data[2];
            final byte mac3 = data[3];
            final byte mac4 = data[4];
            final byte mac5 = data[5];
            final byte mac6 = data[6];

            if (getDeviceMacCallback != null) {
                super.handler.post(new Runnable() {
                    @Override
                    public void run() {
                        getDeviceMacCallback.onCallback(mac1, mac2, mac3,
                                mac4, mac5, mac6);

                    }
                });
            }


        } else if (data[0] == (byte) ENABLE_LOW_BATTERY_REPORT) {

            final int battery = data[1] & 0xFF;
            final int chargeStatus = data[2] & 0xFF;

            if (lowBatteryReportCallback != null)
                super.handler.post(new Runnable() {
                    @Override
                    public void run() {
                        lowBatteryReportCallback.onCallback(battery, chargeStatus);

                    }
                });


        } else if (data[0] == (byte) ENABLE_HR_REPORT_WHILE_ECG_TEST) {

            final int hr = data[1] & 0xFF;

            if (enableHrReportWhileEcgTestCallback != null) {

                super.handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (hr == 0) {

                            enableHrReportWhileEcgTestCallback.onCallback();

                        } else {
                            enableHrReportWhileEcgTestCallback.onDataCallback(hr);
                        }
                    }
                });

            }


        } else if (data[0] == (byte) ENABLE_RT_ECG_AND_PPG_STATUS) {

            final int status = data[1] & 0xFF;

            if (enableEcgAndPPGCallback != null) {

                super.handler.post(new Runnable() {
                    @Override
                    public void run() {

                        enableEcgAndPPGCallback.onCallback(status);

                    }
                });
            }


        } else if (data[0] == (byte) ENABLE_RT_ECG_AND_PPG) {


            if (enableEcgAndPPGCallback != null) {

                super.handler.post(new Runnable() {
                    @Override
                    public void run() {

                        enableEcgAndPPGCallback.onDataCallback(data);
                    }
                });
            }

        } else if (data[0] == (byte) SET_AUTOMATIC_ECG_AND_PPG_TMPER_BATTERY_MEASUREMENT) {

            if (setAutomaticMessurementCallback != null) {
                super.handler.post(new Runnable() {
                    @Override
                    public void run() {
                        setAutomaticMessurementCallback.onCallback();
                    }
                });
            }

        } else if (data[0] == (byte) GET_AUTOMATIC_ECG_AND_PPG_TMPER_BATTERY_MEASUREMENT) {

            final int minutesInterval = ((data[1] << 8) & 0xFF00) + (data[2] & 0xFF);
            final boolean ecgToggle = data[3] == (byte) 0x1 ? true : false;
            final boolean ppgToggle = data[4] == (byte) 0x1 ? true : false;

            if (getAutomaticMessurementCallback != null) {

                super.handler.post(new Runnable() {
                    @Override
                    public void run() {
                        getAutomaticMessurementCallback.onCallback(minutesInterval, ecgToggle, ppgToggle);

                    }
                });

            }

        } else if (data[0] == (byte) GET_AUTOMATIC_ECG_AND_PPG_HISTORY_DATA_SECTION_HEADER) {

            final int sectionId = data[1] & 0xFF;
            int year = Util.ConvertBCD2Decimal(data[2]) + 2010;
            int month = Util.ConvertBCD2Decimal(data[3]) - 1;
            int day = Util.ConvertBCD2Decimal(data[4]);
            int hour = Util.ConvertBCD2Decimal(data[5]);
            int minute = Util.ConvertBCD2Decimal(data[6]);
            int second = Util.ConvertBCD2Decimal(data[7]);

            final Calendar c = Calendar.getInstance();
            c.set(year, month, day, hour, minute, second);

            final int voltagePercent = data[8] & 0xFF;
            final int measurementType = data[9] & 0xFF;
            final float ntcTemperature = ((data[10] & 0xFF) + ((data[11] << 8) & 0xFF00)) /10f;
            final float pcbTemperature = ((data[12] & 0xFF) + ((data[13] << 8) & 0xFF00))/10f;
            final int bodyPosition = data[14] & 0xFF;
            final long dataLength = (data[15] & 0xFF) + ((data[16] << 8) & 0xFF00) +
                    ((data[17] << 16) & 0xFF0000) + ((data[18] << 24) & 0xFF000000);

            if (getAutomaticMeasurementHistoryDataSectionHeaderCallback != null) {

                super.handler.post(new Runnable() {
                    @Override
                    public void run() {
                        getAutomaticMeasurementHistoryDataSectionHeaderCallback
                                .onCallback(sectionId, c, voltagePercent, measurementType,
                                        ntcTemperature, pcbTemperature, bodyPosition, dataLength);

                    }
                });
            }

        } else if (data[0] == (byte) GET_AUTOMATIC_ECG_AND_PPG_HISTORY_DATA_SECTION_DATA) {

            final int sectionId = data[1] & 0xFF;
            final int recordCount = data[2] & 0xFF;
            final byte[] records = new byte[data.length - 3];
            System.arraycopy(data, 2, records, 0, records.length);

            if (getAutomaticMeasurementHistorySectionDataCallback != null) {

                super.handler.post(new Runnable() {
                    @Override
                    public void run() {

                        getAutomaticMeasurementHistorySectionDataCallback.onCallback(
                                sectionId, recordCount, records
                        );
                    }

                });
            }


        } else if (data[0] == (byte) CHECK_BODY_POSITION) {

            final boolean result = data[1] == 0x0 ? false : true;

            if (checkBodyPositionCallback != null) {

                super.handler.post(new Runnable() {
                    @Override
                    public void run() {

                        checkBodyPositionCallback.onCallback(result);
                    }
                });
            }


        } else if (data[0] == (byte) GET_BODY_POSITION) {

            final int bodyPosition = data[1] & 0xFF;

            if (getBodyPositionCallback != null) {

                super.handler.post(new Runnable() {
                    @Override
                    public void run() {
                        getBodyPositionCallback.onCallback(bodyPosition);
                    }
                });
            }


        } else if (data[0] == (byte) CHECK_IS_BODY_TOUCHED) {

            final boolean isTouched = data[1] == 0x0 ? false : true;

            if (checkBodyIsTouchedCallback != null) {

                super.handler.post(new Runnable() {
                    @Override
                    public void run() {
                        checkBodyIsTouchedCallback.onCallback(isTouched);
                    }
                });

            }


        } else if (data[0] == (byte) BODY_TOUCH_REPORT) {

            if (bodyTouchReportCallback != null) {

                final boolean isTouched = data[1] == 0x0 ? false : true;
                int year = Util.ConvertBCD2Decimal(data[2]) + 2010;
                int month = Util.ConvertBCD2Decimal(data[3]) - 1;
                int day = Util.ConvertBCD2Decimal(data[4]);
                int hour = Util.ConvertBCD2Decimal(data[5]);
                int minute = Util.ConvertBCD2Decimal(data[6]);
                int second = Util.ConvertBCD2Decimal(data[7]);

                final Calendar c = Calendar.getInstance();
                c.set(year, month, day, hour, minute, second);


                super.handler.post(new Runnable() {
                    @Override
                    public void run() {
                        bodyTouchReportCallback.onCallback(isTouched, c);
                    }
                });
            }

        } else if (data[0] == (byte) AUTOMATIC_ECG_AND_PPG_MEASUREMENT_FINISH_REPORT) {

            if (enableAutomaticEcgAndPpgMeasurementFinishReportCallback != null) {

                super.handler.post(new Runnable() {
                    @Override
                    public void run() {
                        enableAutomaticEcgAndPpgMeasurementFinishReportCallback.onCallback();

                    }
                });
            }

        } else if (data[0] == (byte) GET_TOTAL_SPORT_DATA_ON_SPECIFIC_DAY) {

            //循环解析变长数据包。 会返回单条数据的N倍长度
            for (int i = 0; i < data.length / 27; i++) {

                final int dayIndex = data[i * 27 + 1] & 0xFF;
                int year = Util.ConvertBCD2Decimal(data[i * 27 + 2]) + 2010;
                int month = Util.ConvertBCD2Decimal(data[i * 27 + 3]) - 1;
                int day = Util.ConvertBCD2Decimal(data[i * 27 + 4]);
                final Calendar c = Calendar.getInstance();
                c.set(year, month, day,0,0,0);

                final long step = (data[i * 27 + 5] & 0xFF) + ((data[i * 27 + 6] << 8) & 0xFF00) +
                        ((data[i * 27 + 7] << 16) & 0xFF0000) + ((data[i * 27 + 8] << 24) & 0xFF000000);

                final long lengthOfExercise = (data[i * 27 + 9] & 0xFF) + ((data[i * 27 + 10] << 8) & 0xFF00) +
                        ((data[i * 27 + 11] << 16) & 0xFF0000) + ((data[i * 27 + 12] << 24) & 0xFF000000);

                final long distance = (data[i * 27 + 13] & 0xFF) + ((data[i * 27 + 14] << 8) & 0xFF00) +
                        ((data[i * 27 + 15] << 16) & 0xFF0000) + ((data[i * 27 + 16] << 24) & 0xFF000000);

                final long calories = (data[i * 27 + 17] & 0xFF) + ((data[i * 27 + 18] << 8) & 0xFF00) +
                        ((data[i * 27 + 19] << 16) & 0xFF0000) + ((data[i * 27 + 20] << 24) & 0xFF000000);

                final int reachGoalPercent = (data[i * 27 + 21] & 0xFF) + ((data[i * 27 + 22] << 8) & 0xFF00);

                if (getTotalSportDataOnSpecificDayCallback != null) {

                    super.handler.post(new Runnable() {
                        @Override
                        public void run() {
                            getTotalSportDataOnSpecificDayCallback.onCallback(dayIndex, c, step, lengthOfExercise
                                    , distance, calories, reachGoalPercent);
                        }
                    });
                }
            }

        } else if (data[0] == (byte) GET_DETAIL_SPORT_DATA_ON_SPECIFIC_DAY) {

            for (int i = 0; i < data.length / 25; i++) {


                final int dataIndex = data[i * 25 + 1] & 0xFF + ((data[i * 25 + 2] << 8) & 0xFF00);
                int year = Util.ConvertBCD2Decimal(data[i * 25 + 3]) + 2010;
                int month = Util.ConvertBCD2Decimal(data[i * 25 + 4]) - 1;
                int day = Util.ConvertBCD2Decimal(data[i * 25 + 5]);
                int hour = Util.ConvertBCD2Decimal(data[i * 25 + 6]);
                int minute = Util.ConvertBCD2Decimal(data[i * 25 + 7]);
                int second = Util.ConvertBCD2Decimal(data[i * 25 + 8]);


                final Calendar c = Calendar.getInstance();
                c.set(year, month, day, hour, minute, second);

                final int step = (data[i * 25 + 9] & 0xFF) + ((data[i * 25 + 10] << 8) & 0xFF00);

                final int distance = (data[i * 25 + 11] & 0xFF) + ((data[i * 25 + 12] << 8) & 0xFF00);

                final int calories = (data[i * 25 + 13] & 0xFF) + ((data[i * 25 + 14] << 8) & 0xFF00);

                final byte[] tenMinutesStep = new byte[10];

                System.arraycopy(data, i * 25 + 15, tenMinutesStep, 0, 10);

                if (getDetailSportDataOnSpecificDayCallback != null) {
                    getDetailSportDataOnSpecificDayCallback.onCallback(dataIndex, c, step, distance,
                            calories, tenMinutesStep);


                }
            }

        } else if (data[0] == (byte) GET_VERSION) {

            final String versionStr = data[1] + "." + data[2] + "." + data[3] + "." + data[4];
            int year = Util.ConvertBCD2Decimal(data[5]) + 2010;
            int month = Util.ConvertBCD2Decimal(data[6]) - 1;
            int day = Util.ConvertBCD2Decimal(data[7]);
            final Calendar c = Calendar.getInstance();
            c.set(year, month, day);

            if (getVersionCallback != null) {

                getVersionCallback.onCallback(versionStr, c);
            }

        } else if (data[0] == (byte) ENABLE_RT_TEMPERATURE_MESSUREMENT) {

            super.handler.post(new Runnable() {
                @Override
                public void run() {
                    if (enableRtTemperatureMessurementCallback != null) {
                        enableRtTemperatureMessurementCallback.onCallback();
                    }
                }
            });

        } else if (data[0] == 0x1D)

        {

            final float ntcTemp = ((data[4] & 0xFF) + ((data[3] << 8) & 0xFF00) +
                    ((data[2] << 16) & 0xFF0000) + ((data[1] << 24) & 0xFF000000) )/ 100f;

            final float pcbTemp = ((data[8] & 0xFF) + ((data[7] << 8) & 0xFF00) +
                    ((data[6] << 16) & 0xFF0000) + ((data[5] << 24) & 0xFF000000) )/ 100f;


            super.handler.post(new Runnable() {
                @Override
                public void run() {
                    if (enableRtTemperatureMessurementCallback != null) {
                        enableRtTemperatureMessurementCallback.onDataCallback(ntcTemp, pcbTemp);
                    }
                }
            });


        } else if (mCommandReceiveredCallback != null) {
            mCommandReceiveredCallback.onCommandReceived(data);
        }

    }

    public void setCommandReceiveredCallback(CommandReceiveredCallback mCommandReceiveredCallback) {
        this.mCommandReceiveredCallback = mCommandReceiveredCallback;
    }

    private CommandReceiveredCallback mCommandReceiveredCallback;

    public interface CommandReceiveredCallback {

        void onCommandReceived(byte[] cmd);

    }

    J1767BleCallbacks.SetTimeCallback setTimeCallback = null;

    /**
     * set datetime to device.
     *
     * @param c
     * @param callback callback of setting time.
     */
    public void setTime(Calendar c, J1767BleCallbacks.SetTimeCallback callback) {
        setTimeCallback = callback;

        if (super.isConnected() == false) {
            return;
        }

        byte[] value = new byte[16];
        value[0] = (byte) SET_TIME;
        value[1] = Util.ConvertDecimal2BCD((byte) (c.get(Calendar.YEAR) - 2010));
        value[2] = Util.ConvertDecimal2BCD((byte) (c.get(Calendar.MONTH) + 1));
        value[3] = Util.ConvertDecimal2BCD((byte) (c.get(Calendar.DAY_OF_MONTH)));
        value[4] = Util.ConvertDecimal2BCD((byte) (c.get(Calendar.HOUR_OF_DAY)));
        value[5] = Util.ConvertDecimal2BCD((byte) (c.get(Calendar.MINUTE)));
        value[6] = Util.ConvertDecimal2BCD((byte) (c.get(Calendar.SECOND)));
        value[15] = crc(value);
        super.writeTx(value);
    }

    J1767BleCallbacks.GetTimeCallback getTimeCallback = null;

    /**
     * get datetime from device.
     *
     * @param callback callback of getting time.
     */
    public void getTime(J1767BleCallbacks.GetTimeCallback callback) {

        getTimeCallback = callback;

        if (super.isConnected() == false) {
            return;
        }
        byte[] value = new byte[16];
        value[0] = (byte) GET_TIME;
        value[15] = crc(value);
        super.writeTx(value);
    }

    /***
     * Set user Infomation to device
     * @param gender True:male,false:female
     * @param age age
     * @param height height
     * @param weight weight
     * @param stepLength stepLength
     * @param callback callback
     */
    J1767BleCallbacks.SetUserInfoCallback setUserInfoCallback;


    public void setUserInfo(boolean gender, byte age, byte height, byte weight, byte stepLength
            , J1767BleCallbacks.SetUserInfoCallback callback
    ) {

        setUserInfoCallback = callback;

        byte[] command = new byte[16];
        command[0] = 0x2;
        command[1] = gender ? (byte) 0x1 : (byte) 0x0;
        command[2] = age;
        command[3] = height;
        command[4] = weight;
        command[5] = stepLength;
        command[15] = crc(command);
        super.writeTx(command);

    }

    J1767BleCallbacks.GetUserInfoCallback getUserInfoCallback;

    /**
     * Get User info
     *
     * @param callback callback
     */
    public void getUserInfo(J1767BleCallbacks.GetUserInfoCallback callback) {

        getUserInfoCallback = callback;

        byte[] command = new byte[16];
        command[0] = 0x42;
        command[15] = crc(command);
        super.writeTx(command);
    }

    J1767BleCallbacks.EnableRTStepCallback enableRTStepCallback;

    /**
     * Enable real-time step counting
     *
     * @param callback callback
     */
    public void enableRTStepCounting(J1767BleCallbacks.EnableRTStepCallback callback) {

        enableRTStepCallback = callback;

        byte[] command = new byte[16];
        command[0] = 0x09;
        command[15] = crc(command);
        super.writeTx(command);

    }

    J1767BleCallbacks.DisableRTStepCallback disableRTStepCallback;

    /**
     * Disable real-time step counting
     *
     * @param callback callback
     */
    public void disableRTStepCounting(J1767BleCallbacks.DisableRTStepCallback callback) {

        this.disableRTStepCallback = callback;


        byte[] command = new byte[16];
        command[0] = 0x0A;
        command[15] = crc(command);
        super.writeTx(command);
    }

    J1767BleCallbacks.SetDeviceIdCallback setDeviceIdCallback;

    /**
     * Set Device Id.
     *
     * @param id1
     * @param id2
     * @param id3
     * @param id4
     * @param id5
     * @param id6
     * @param callback callback
     */
    public void setDeviceId(
            byte id1, byte id2, byte id3, byte id4, byte id5, byte id6,
            J1767BleCallbacks.SetDeviceIdCallback callback) {

        setDeviceIdCallback = callback;

        byte[] command = new byte[16];
        command[0] = SET_DEVICE_ID;
        command[1] = id1;
        command[2] = id2;
        command[3] = id3;
        command[4] = id4;
        command[5] = id5;
        command[6] = id6;
        command[15] = crc(command);
        super.writeTx(command);
    }

    J1767BleCallbacks.GetBatteryLevelCallback getBatteryLevelCallback;


    /**
     * Get Device battery Remained
     *
     * @param callback callback
     */
    public void getBatteryLevel(J1767BleCallbacks.GetBatteryLevelCallback
                                        callback) {

        getBatteryLevelCallback = callback;

        byte[] command = new byte[16];
        command[0] = (byte) GET_BATTERY;
        command[15] = crc(command);
        super.writeTx(command);
    }

    J1767BleCallbacks.GetDeviceMacCallback getDeviceMacCallback;

    /**
     * Get this device's MAC address
     *
     * @param callback callback
     */
    public void getDeviceMac(J1767BleCallbacks.GetDeviceMacCallback callback) {
        getDeviceMacCallback = callback;

        byte[] command = new byte[16];
        command[0] = (byte) GET_MAC;
        command[15] = crc(command);
        super.writeTx(command);
    }

    /**
     * Reset this device to factory setting.
     */
    public void restartDevice() {

        if (super.isConnected() == false) {
            return;
        }

        byte[] value = new byte[16];
        value[0] = (byte) RESTART_DEVICE;
        value[15] = crc(value);
        super.writeTx(value);

    }

    J1767BleCallbacks.LowBatteryReportCallback lowBatteryReportCallback;

    /**
     * If you Set callback to this method.
     * when battery is lower than 10%. the callback will be called.
     *
     * @param callback
     */

    public void enableLowBatteryReport(J1767BleCallbacks.LowBatteryReportCallback callback) {
        lowBatteryReportCallback = callback;


    }

    J1767BleCallbacks.GetVersionCallback getVersionCallback;

    /**
     * Get Firmware Version
     *
     * @param callback callback
     */
    public void getVersion(J1767BleCallbacks.GetVersionCallback callback) {

        getVersionCallback = callback;

        if (super.isConnected() == false) {
            return;
        }

        byte[] value = new byte[16];
        value[0] = (byte) GET_VERSION;
        value[15] = crc(value);
        super.writeTx(value);
    }

    J1767BleCallbacks.EnableHrReportWhileEcgTestCallback enableHrReportWhileEcgTestCallback;

    /**
     * Enable or disable  HR data while ECG test.
     *
     * @param enable   true:enable , false:disable
     * @param callback callback
     */
    public void enableHrReportWhileEcgTest(boolean enable, J1767BleCallbacks.EnableHrReportWhileEcgTestCallback callback) {

        enableHrReportWhileEcgTestCallback = callback;
        if (super.isConnected() == false) {
            return;
        }

        byte[] value = new byte[16];
        value[0] = (byte) ENABLE_HR_REPORT_WHILE_ECG_TEST;
        if (enable) {
            value[1] = (byte) 0x1;

        } else {
            value[1] = (byte) 0x0;
        }
        value[15] = crc(value);
        super.writeTx(value);
    }

    J1767BleCallbacks.EnableEcgAndPPGCallback enableEcgAndPPGCallback;

    /**
     * real time ECG+PPG measuring
     * Notice:If the device electrode leaves the body during the measurement, the measurement is automatically suspended.
     * @param ecgToggle
     *
     * @param mainToggle
     *                  When mainToggle=0,This command is used to control ECG+PPG to be turned on or off at the same time. When ecgToggle=0, ECG+PPG measurement is turned off at the same time. ecgToggle is non-zero value means that ECG+PPG measurement is turned on at the same time, and the value of ecgToggle is the current intensity of PPG. Value range 1~15
     *                  When mainToggle=1,Represents a switch that controls the ECG's individual measurements. ecgToggle=0 is to turn off the ECG measurement alone, and ecgToggle is a non-zero value to turn on the ECG measurement alone.
     *                  When mainToggle=2,Indicates that the PPG measurement is turned on separately. ecgToggle=0 is to turn off ECGPPG measurement alone, ecgToggle is non-zero value to turn on ECGPPG measurement separately, and ecgToggle value is PPG current intensity (value range 1~15)
     * @param callback callback
     */
    public void enableEcgAndPPG(byte ecgToggle, byte mainToggle, J1767BleCallbacks.EnableEcgAndPPGCallback
            callback) {
        enableEcgAndPPGCallback = callback;

        byte[] value = new byte[16];
        value[0] = (byte) ENABLE_RT_ECG_AND_PPG;


        value[1] = ecgToggle;
        value[2] = mainToggle;

        value[15] = crc(value);
        Log.d("_SCANNING", "enableEcgAndPPG: WRITING ECG PPG ENABLE " + Arrays.toString(value));
        super.writeTx(value);
    }

    J1767BleCallbacks.EnableRtTemperatureMessurementCallback enableRtTemperatureMessurementCallback;

    /**
     * Enable or Disable Temperature Meassurement
     *
     * @param enable True:Enable,False:Disable
     * @param callback callback
     */
    public void enableTemperatureMeassurement(boolean enable, J1767BleCallbacks.EnableRtTemperatureMessurementCallback callback) {
        enableRtTemperatureMessurementCallback = callback;

        byte[] value = new byte[16];
        value[0] = (byte) ENABLE_RT_TEMPERATURE_MESSUREMENT;

        if (enable) {
            value[1] = 0x1;
        } else {
            value[1] = 0x0;
        }

        value[15] = crc(value);
        super.writeTx(value);
    }

    J1767BleCallbacks.SetAutomaticMeasurementCallback setAutomaticMessurementCallback;


    /**
     * Note that the temperature and battery level parameters are detected by default in the case of periodic measurements (cann’t be set).
     *
     * @param minutesInterval measure interval(The minimum setting is 30 mins,the max setting is 24 hours,Default one hour)
     * @param ecgToggle ECG period measurement is on, it is the default. ecgToggle == false means the ECG period measurement is off.
     * @param ppgToggle PPG period measurement is on ,it is the default,ppgToggle == false means PPG period measurement is off
     * @param callback callback
     */
    public void setAutomaticMeasurement(int minutesInterval, boolean ecgToggle, boolean ppgToggle
            , J1767BleCallbacks.SetAutomaticMeasurementCallback callback
    ) {
        setAutomaticMessurementCallback = callback;

        byte[] value = new byte[16];
        value[0] = (byte) SET_AUTOMATIC_ECG_AND_PPG_TMPER_BATTERY_MEASUREMENT;

        value[1] = (byte) ((minutesInterval >> 8) & 0xFF);
        value[2] = (byte) (minutesInterval & 0xFF);

        if (ecgToggle) {
            value[3] = (byte) 0x1;
        } else {
            value[3] = (byte) 0x0;
        }

        if (ppgToggle) {
            value[4] = (byte) 0x1;
        } else {
            value[4] = (byte) 0x0;
        }

        value[15] = crc(value);
        super.writeTx(value);


    }


    J1767BleCallbacks.GetAutomaticMeasurementCallback getAutomaticMessurementCallback;

    /**
     * get parameters for automatic measurement of ecg+ppg, temperature, and battery level
     * @param callback callback
     */
    public void getAutomaticMeasurement(J1767BleCallbacks.GetAutomaticMeasurementCallback callback) {

        getAutomaticMessurementCallback = callback;

        byte[] value = new byte[16];
        value[0] = (byte) GET_AUTOMATIC_ECG_AND_PPG_TMPER_BATTERY_MEASUREMENT;

        value[15] = crc(value);
        super.writeTx(value);
    }

    J1767BleCallbacks.GetAutomaticMeasurementHistoryDataSectionHeaderCallback getAutomaticMeasurementHistoryDataSectionHeaderCallback;

    /**
     *  Get ECG+PPG historical data block information
     * @param sectionId sectionId =1 indicates the header of the ECG + PPG automatic measurement storage corresponding to the latest storage location of the time stamp.
     *        sectionId = 1 indicates that the latest storage location of the timestamp pushes the storage header of the
     *        sectionId = 2 indicates that the latest storage location of the timestamp pushes the storage header of the corresponding ECG + PPG automatically after pushing the two storage locations.
     *        sectionId max No is 2 ,Minimum is 0
     * @param callback callback
     */
    public void getAutomaticMeasurementHistoryDataSectionHeader(int sectionId
            , J1767BleCallbacks.GetAutomaticMeasurementHistoryDataSectionHeaderCallback callback) {

        getAutomaticMeasurementHistoryDataSectionHeaderCallback = callback;

        byte[] cmd = new byte[16];
        cmd[0] = (byte) GET_AUTOMATIC_ECG_AND_PPG_HISTORY_DATA_SECTION_HEADER;
        cmd[1] = (byte) sectionId;
        cmd[15] = crc(cmd);
        super.writeTx(cmd);
    }

    J1767BleCallbacks.GetAutomaticMeasurementHistorySectionDataCallback getAutomaticMeasurementHistorySectionDataCallback;

    /**
     * automatic measurement of ecg+ppg historical data details
     * @param sectionId  sectionId = 0 Indicates the ECG + PPG automatic measurement history data details with the latest time-stamped storage location corresponding to the number 0.
     *           sectionId = 1 The ECG + PPG automatic measurement history data detailed information corresponding to the number 1 is obtained after the first storage location of the timestamp is updated.
     *           sectionId = 2 indicates the latest time-stamped storage location. The ECG + PPG automatic measurement history data details corresponding to the number 2 are pushed forward by 2 storage locations.
     *           sectionId maximum No.is 2 ,minimum No.is 0
     * @param callback callback
     */
    public void getAutomaticMeasurementHistoryDataSectionData(int sectionId
            , J1767BleCallbacks.GetAutomaticMeasurementHistorySectionDataCallback callback) {

        getAutomaticMeasurementHistorySectionDataCallback = callback;

        byte[] cmd = new byte[16];
        cmd[0] = (byte) GET_AUTOMATIC_ECG_AND_PPG_HISTORY_DATA_SECTION_DATA;
        cmd[1] = (byte) sectionId;
        cmd[15] = crc(cmd);
        super.writeTx(cmd);

    }

    J1767BleCallbacks.CheckBodyPositionCallback checkBodyPositionCallback;

    /**
     * Check if it is worn correctly
     * @param callback
     */
    public void CheckWornCorrectly(J1767BleCallbacks.CheckBodyPositionCallback callback) {
        checkBodyPositionCallback = callback;

        byte[] cmd = new byte[16];
        cmd[0] = (byte) CHECK_BODY_POSITION;
        cmd[15] = crc(cmd);
        super.writeTx(cmd);
    }

    J1767BleCallbacks.GetBodyPositionCallback getBodyPositionCallback;

    /**
     *  man body simple gesture recognition
     * @param callback callback
     */
    public void getBodyPosition(J1767BleCallbacks.GetBodyPositionCallback callback) {

        getBodyPositionCallback = callback;

        byte[] cmd = new byte[16];
        cmd[0] = (byte) CHECK_BODY_POSITION;
        cmd[15] = crc(cmd);
        super.writeTx(cmd);
    }

    J1767BleCallbacks.CheckBodyIsTouchedCallback checkBodyIsTouchedCallback;

    /**
     * Whether the ECG patch connected the skin well (add)
     * @param callback callback
     */
    public void CheckBodyisTouched(J1767BleCallbacks.CheckBodyIsTouchedCallback callback) {

        checkBodyIsTouchedCallback = callback;

        byte[] cmd = new byte[16];
        cmd[0] = (byte) CHECK_IS_BODY_TOUCHED;
        cmd[15] = crc(cmd);
        super.writeTx(cmd);
    }

    J1767BleCallbacks.BodyTouchReportCallback bodyTouchReportCallback;

    /**
     * (ECG patch connect skin report
     * @param callback callback
     */
    public void enableBodyTouchReport(J1767BleCallbacks.BodyTouchReportCallback callback) {

        bodyTouchReportCallback = callback;
    }

    J1767BleCallbacks.EnableAutomaticEcgAndPpgMeasurementFinishReportCallback enableAutomaticEcgAndPpgMeasurementFinishReportCallback;


    /**
     * Automatic ECG/PPG Measurement Completion Report
     * @param callback callback
     */
    public void enableAutomaticEcgAndPpgMeasurementFinishReport(J1767BleCallbacks.EnableAutomaticEcgAndPpgMeasurementFinishReportCallback callback) {
        enableAutomaticEcgAndPpgMeasurementFinishReportCallback = callback;

    }

    J1767BleCallbacks.GetTotalSportDataOnSpecificDayCallback getTotalSportDataOnSpecificDayCallback;

    /**
     *
     * @param callback
     */
    public void getTotalSportData(J1767BleCallbacks.GetTotalSportDataOnSpecificDayCallback callback) {

        getTotalSportDataOnSpecificDayCallback = callback;

        byte[] cmd = new byte[16];
        cmd[0] = (byte) GET_TOTAL_SPORT_DATA_ON_SPECIFIC_DAY;
        cmd[1] = 0;
        cmd[15] = crc(cmd);
        super.writeTx(cmd);
    }

    J1767BleCallbacks.GetDetailSportDataOnSpecificDayCallback getDetailSportDataOnSpecificDayCallback;

    /**
     *
     * @param controlBit
     * @param dataIndex
     * @param callback
     */
    public void getDetailSportDataOnSpecificDay(int controlBit, int dataIndex,
                                                J1767BleCallbacks.GetDetailSportDataOnSpecificDayCallback callback) {

        getDetailSportDataOnSpecificDayCallback = callback;
        byte[] cmd = new byte[16];
        cmd[0] = (byte) GET_DETAIL_SPORT_DATA_ON_SPECIFIC_DAY;
        cmd[1] = (byte) (dataIndex & 0xFF);
        cmd[2] = (byte) ((dataIndex >> 8) & 0xFF);

        cmd[15] = crc(cmd);
        super.writeTx(cmd);
    }

    private byte crc(byte[] value) {

        byte b = 0;
        for (int i = 0; i < 15; i++) {
            b += value[i];
        }

        return b;
    }


    @Deprecated
    public void getStepData() {

        byte[] command = new byte[16];
        command[0] = 0x48;
        command[15] = crc(command);
        super.writeTx(command);

    }



    @Deprecated
    public void getSpecificDayTotalData(byte days) {


        byte[] command = new byte[16];
        command[0] = 0xA;
        command[1] = days;
        command[15] = crc(command);
        super.writeTx(command);
    }
    @Deprecated
    public void getEveryDaysDetailsData(byte id) {

        byte[] command = new byte[16];
        command[0] = 0x43;
        command[1] = id;
        command[15] = crc(command);
        super.writeTx(command);
    }

    @Deprecated
    public void hibernate() {

        byte[] command = new byte[16];
        command[0] = 0x12;
        command[15] = crc(command);
        super.writeTx(command);

    }

    @Deprecated
    public void getModelNumber() {

        byte[] command = new byte[16];
        command[0] = (byte) 0xbe;
        command[15] = crc(command);
        super.writeTx(command);
    }

    @Deprecated
    public void getBaseHr() {

        byte[] command = new byte[16];
        command[0] = (byte) 0x14;
        command[15] = crc(command);
        super.writeTx(command);
    }

    @Deprecated
    public void getLogInfo(byte logType) {
        byte[] command = new byte[16];
        command[0] = (byte) 0x14;
        command[1] = logType;
        command[15] = crc(command);
        super.writeTx(command);
    }

    @Deprecated
    public void getSleepInfo(byte getType, byte isDebugInfoReturn) {

        byte[] command = new byte[16];
        command[0] = (byte) 0x18;
        command[1] = getType;
        command[2] = isDebugInfoReturn;
        command[15] = crc(command);
        super.writeTx(command);
    }

    @Deprecated
    public void enableRtEcg(byte enable) {

        byte[] command = new byte[16];
        command[0] = (byte) 0x16;
        command[1] = enable;
        command[15] = crc(command);
        super.writeTx(command);
    }

    @Deprecated
    public void enableIrPPGTest(byte enable) {

        byte[] command = new byte[16];
        command[0] = (byte) 0x33;
        command[1] = enable;
        command[15] = crc(command);
        super.writeTx(command);
    }

    @Deprecated
    public void downloadIrPPGData(byte enable) {

        byte[] command = new byte[16];
        command[0] = (byte) 0x35;
        command[1] = enable;
        command[15] = crc(command);
        super.writeTx(command);
    }

    @Deprecated
    public void getHistoryEcgData(byte block, byte address) {

        byte[] command = new byte[16];
        command[0] = (byte) 0x31;
        command[1] = block;
        command[2] = address;
        command[15] = crc(command);
        super.writeTx(command);
    }

    @Deprecated
    public void setEnableRtTemperature(byte enable) {

        byte[] command = new byte[16];
        command[0] = (byte) 0x1C;
        command[1] = enable;
        command[15] = crc(command);
        super.writeTx(command);

    }

    @Deprecated
    public void clearDevicesData() {

        byte[] command = new byte[16];
        command[0] = (byte) 0x49;
        command[15] = 0x49;
        super.writeTx(command);

    }

    @Deprecated
    public void getAutoMeasureSettings() {

        byte[] command = new byte[16];
        command[0] = (byte) 0x39;
        command[15] = (byte) 0x39;
        super.writeTx(command);

    }

    @Deprecated
    public void setAutoMeasureSettings(byte ecg_enable, byte ecg_hourInterval,
                                       byte tempere_enable, byte temp_hourInterval) {
        byte[] command = new byte[16];
        command[0] = (byte) 0x37;
        command[1] = ecg_enable;
        command[2] = ecg_hourInterval;
        command[3] = tempere_enable;
        command[4] = temp_hourInterval;
        command[15] = crc(command);

        super.writeTx(command);
    }

}