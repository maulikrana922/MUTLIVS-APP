package com.youhong.cuptime.blesdk;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;

import androidx.annotation.NonNull;

import java.util.Calendar;

/**
 * Created by Administrator on 2017/5/19.
 */

@Deprecated
public class CupTimeBleOperation extends BaseBleOperation {

    private static final int COMMAND_ID_SETTING_TIME = 0x56;
    private static final int COMMAND_ID_GETTING_TIME = 0x46;
    private static final int COMMAND_ID_SETTING_DAILY_DRINKING_GOAL = 0x5c;
    private static final int COMMAND_ID_GETTING_DAILY_DRINKING_GOAL = 0x4c;
    private static final int COMMAND_ID_SETTING_PERSONAL_INFO = 0x5b;
    private static final int COMMAND_ID_GETTING_PERSONAL_INFO = 0x4b;
    private static final int COMMAND_ID_GETTING_DRINKING_RECORD = 0x4a;
    private static final int COMMAND_ID_RESET_DRINKING_RECORD_INDEX = 0xb1;

    private static final int COMMAND_ID_CLEAR_DRINKING_RECORD = 0x62;
    private static final int COMMAND_ID_RESTART_DEVICE = 0x61;
    private static final int COMMAND_ID_CLOSE_DEVICE = 0xbc;
    private static final int COMMAND_ID_SETTING_DEVICE_NAME = 0x71;
    private static final int COMMAND_ID_GETTING_CURRENT_DRINKING_INFO = 0xb0;

    public static final String SIGNAL_GENE = "GE";
    public static final String SIGNAL_MOLKIT_SEED = "SD";
    public static final String SIGNAL_JSTYLE_SEED = "JC";

    private int INDEX_SIGNAL = 25;
    private int INDEX_NOTIFICATION_BYTE = 44;


    public CupTimeBleOperation(Context context) {
        super(context);
    }

    @Override
    protected void Action_RX_data(Context context, Intent intent) {

        byte[] values = intent.getByteArrayExtra(LightBLEService.EXTRA_DATA);

        if (values[0] == (byte) COMMAND_ID_SETTING_TIME) {

            if (setTime_callback != null) {


                MyRunnable runnable = new MyRunnable() {
                    @Override
                    public void Run(Bundle data) {

                        setTime_callback.onResultNotify(true);
                    }
                };
                Message msg = new Message();
                msg.obj = runnable;
                handler.handleMessage(msg);

            }
        } else if (values[0] == (byte) COMMAND_ID_GETTING_TIME) {


            if (getTime_callback != null) {

                long second = (((values[7] << 24)) & 0x00000000FF000000L)
                        + (values[6] << 16) & 0x00FF0000
                        + (values[5] << 8) & 0x0000FF00 +
                        (values[4] & 0x000000FF);

                Calendar c = Calendar.getInstance();
                c.setTimeInMillis(second * 1000);

                int battery = values[16] & 0xFF;


                MyRunnable runnable = new MyRunnable() {
                    @Override
                    public void Run(Bundle data) {
                        int battery = data.getInt("battery");
                        Calendar c = (Calendar) data.getSerializable("c");
                        getTime_callback.onGetDateTime(c, battery);
                    }
                };

                Message msg = new Message();
                msg.obj = runnable;
                msg.setData(new Bundle());
                msg.getData().putSerializable("c", c);
                msg.getData().putInt("battery", battery);
                handler.handleMessage(msg);
            }

        } else if (values[0] == (byte) COMMAND_ID_SETTING_DAILY_DRINKING_GOAL) {

            if (setDailyDrinkingGoal_callback != null) {

                MyRunnable runnable = new MyRunnable() {
                    @Override
                    public void Run(Bundle data) {

                        setDailyDrinkingGoal_callback.onResultNotify(true);

                    }
                };
                Message msg = new Message();
                msg.obj = runnable;
                super.handler.handleMessage(msg);


            }

        } else if (values[0] == (byte) COMMAND_ID_GETTING_DAILY_DRINKING_GOAL) {

            int dailyDrinkGoal =
                    (values[8] >> 8) + (values[9]);

            if (getDailyDrinkingGoal_callback != null) {
                MyRunnable runnable = new MyRunnable() {
                    @Override
                    public void Run(Bundle data) {

                        int ml = data.getInt("dailyDrinkGoal");
                        getDailyDrinkingGoal_callback.onGetDailyDrinkingGoal(ml);

                    }
                };

                Message msg = new Message();
                msg.obj = runnable;
                Bundle bundle = new Bundle();
                bundle.putInt("dailyDrinkGoal", dailyDrinkGoal);
                msg.setData(bundle);
                super.handler.handleMessage(msg);

            }
        } else if (values[0] == (byte) COMMAND_ID_GETTING_PERSONAL_INFO) {

            if (getPersonalInfoCallback != null) {

                int weight = (values[4] & 0x0000000FF);
                int stature = (values[5] & 0x0000000FF);
                int birthYear = (values[6] & 0x000000FF) + 1920;
                int sex = values[7] & 0x0000000FF;
                MyRunnable runnable = new MyRunnable() {
                    @Override
                    public void Run(Bundle data) {

                        int weight = data.getInt("weight");
                        int stature = data.getInt("stature");
                        int birthYear = data.getInt("birthYear");
                        int sex = data.getInt("sex");
                        getPersonalInfoCallback.onGetPersonalInfo(weight,
                                stature, birthYear, sex);
                    }
                };

                Message msg = new Message();
                msg.obj = runnable;
                msg.setData(new Bundle());
                msg.getData().putInt("weight", weight);
                msg.getData().putInt("stature", stature);
                msg.getData().putInt("birthYear", birthYear);
                msg.getData().putInt("sex", sex);
                super.handler.handleMessage(msg);

            }

        } else if (values[0] == (byte) COMMAND_ID_SETTING_PERSONAL_INFO) {

            if (setPersonalInfoCallback != null) {

                MyRunnable runnable = new MyRunnable() {
                    @Override
                    public void Run(Bundle data) {

                        setPersonalInfoCallback.onResultNotify(true);

                    }
                };

                Message msg = new Message();
                msg.obj = runnable;
                super.handler.handleMessage(msg);
            }
        } else if (values[0] == (byte) COMMAND_ID_GETTING_DRINKING_RECORD) {
            if (getDrinkingRecordCallback != null) {


                long utcSeconds =
                        ((values[7] << 24) & 0xFF000000L)
                                + ((values[6] << 16) & 0x00FF0000)
                                + ((values[5] << 8) & 0x0000FF00)
                                + (values[4] & 0x000000FF);

                int drinkingML = ((values[9] << 8) & 0x0000FF00) +
                        (values[8] & 0xFF);

                int temperature = ((values[0xB] << 8) & 0x0000FF00) +
                        (values[0xA] & 0xFF);

                int percent = values[0xC] & 0xFF;

                MyRunnable runnable = new MyRunnable() {
                    @Override
                    public void Run(Bundle data) {

                        long utcSeconds = data.getLong("utcSeconds");
                        int drinkingML = data.getInt("drinkingML");
                        int temperature = data.getInt("temperature");
                        int percent = data.getInt("percent");
                        getDrinkingRecordCallback.onGetDrinkingRecordCallback(utcSeconds,
                                drinkingML, temperature, percent
                        );

                    }
                };

                Message msg = new Message();
                msg.obj = runnable;
                msg.setData(new Bundle());
                msg.getData().putLong("utcSeconds", utcSeconds);
                msg.getData().putInt("drinkingML", drinkingML);
                msg.getData().putInt("temperature", temperature);
                msg.getData().putInt("percent", percent);
                super.handler.handleMessage(msg);
            }

        } else if (values[0] == (byte) COMMAND_ID_CLEAR_DRINKING_RECORD) {

            if (clearDrinkingRecordCallback != null) {

                MyRunnable runnable = new MyRunnable() {
                    @Override
                    public void Run(Bundle data) {


                        clearDrinkingRecordCallback.onResultNotify(true);
                    }
                };
                Message msg = new Message();
                msg.obj = runnable;
                super.handler.handleMessage(msg);
            }
        } else if (values[0] == (byte) COMMAND_ID_RESTART_DEVICE) {

            if (restartDeviceCallback != null) {
                MyRunnable runnable = new MyRunnable() {
                    @Override
                    public void Run(Bundle data) {

                        restartDeviceCallback.onResultNotify(true);

                    }
                };
                Message msg = new Message();
                msg.obj = runnable;
                super.handler.handleMessage(msg);

            }


        } else if (values[0] == (byte) COMMAND_ID_CLOSE_DEVICE) {

            if (closeDeviceCallback != null) {
                MyRunnable runnable = new MyRunnable() {
                    @Override
                    public void Run(Bundle data) {

                        closeDeviceCallback.onResultNotify(true);

                    }
                };
                Message msg = new Message();
                msg.obj = runnable;
                super.handler.handleMessage(msg);

            }
        } else if (values[0] == (byte) COMMAND_ID_SETTING_DEVICE_NAME) {

            if (setDeviceNameCallback != null) {
                MyRunnable runnable = new MyRunnable() {
                    @Override
                    public void Run(Bundle data) {

                        setDeviceNameCallback.onResultNotify(true);

                    }
                };
                Message msg = new Message();
                msg.obj = runnable;
                super.handler.handleMessage(msg);
            }
        } else if (values[0] == (byte) COMMAND_ID_RESET_DRINKING_RECORD_INDEX) {

            if (getDrinkingRecordEndingCallback != null) {


                MyRunnable runnable = new MyRunnable() {
                    @Override
                    public void Run(Bundle data) {

                        getDrinkingRecordEndingCallback.onResultNotify(true);
                    }
                };

                Message msg = new Message();
                msg.obj = runnable;
                super.handler.handleMessage(msg);

            }


        }
    }

    BaseBleOperation.ResultNotifyCallback setTime_callback = null;

    public void command_setTime(Calendar calendar, BaseBleOperation.ResultNotifyCallback callback) {
        setTime_callback = callback;
        int minute = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);
        int second = (int) (calendar.getTimeInMillis() / 1000);

        byte[] values = new byte[20];
        values[0] = (byte) COMMAND_ID_SETTING_TIME;
        values[7] = (byte) ((second & 0xFF000000) >> 24);
        values[6] = (byte) ((second & 0x00FF0000) >> 16);
        values[5] = (byte) ((second & 0x0000FF00) >> 8);
        values[4] = (byte) (second & 0x000000FF);
        values[9] = (byte) ((minute & 0x0000FF00) >> 8);
        values[8] = (byte) ((minute & 0x000000FF));

        super.writeTx(values);
    }

    GetDateTimeCallback getTime_callback;

    public void command_getTime(GetDateTimeCallback callback) {

        getTime_callback = callback;
        byte[] values = new byte[20];
        values[0] = (byte) COMMAND_ID_GETTING_TIME;
        writeTx(values);
    }

    BaseBleOperation.ResultNotifyCallback setDailyDrinkingGoal_callback;

    public void command_setDailyDrinkingGoal(int dailyDrinkingGoal, BaseBleOperation.ResultNotifyCallback callback) {

        setDailyDrinkingGoal_callback = callback;

        byte[] values = new byte[20];

        values[0] = (byte) COMMAND_ID_SETTING_DAILY_DRINKING_GOAL;
        values[9] = (byte) ((dailyDrinkingGoal & 0x0000FF00) >> 8);
        values[8] = (byte) (dailyDrinkingGoal & 0x000000FF);

        writeTx(values);

    }

    GetDailyDrinkingGoalCallback getDailyDrinkingGoal_callback;

    public void command_getDailyDrinkingGoal(GetDailyDrinkingGoalCallback callback) {


        getDailyDrinkingGoal_callback = callback;

        byte[] values = new byte[20];
        values[0] = (byte) (COMMAND_ID_GETTING_DAILY_DRINKING_GOAL);
        writeTx(values);

    }

    GetPersonalInfoCallback getPersonalInfoCallback = null;

    public void command_getPersonalInfo(GetPersonalInfoCallback callback) {

        getPersonalInfoCallback = callback;

        byte[] values = new byte[20];
        values[0] = (byte) COMMAND_ID_GETTING_PERSONAL_INFO;
        super.writeTx(values);

    }

    BaseBleOperation.ResultNotifyCallback setPersonalInfoCallback = null;

    public void command_setPersonalInfo(int weight, int stature, int birthYear, int sex, BaseBleOperation.ResultNotifyCallback callback) {
        setPersonalInfoCallback = callback;

        byte[] values = new byte[20];
        values[0] = COMMAND_ID_SETTING_PERSONAL_INFO;
        values[4] = (byte) weight;
        values[5] = (byte) stature;
        values[6] = (byte) (birthYear - 1920);
        values[7] = (byte) sex;
        super.writeTx(values);
    }

    GetDrinkingRecordCallback getDrinkingRecordCallback = null;
    ResultNotifyCallback getDrinkingRecordEndingCallback = null;


    public void command_getDrinkingRecord(long utcSeconds, GetDrinkingRecordCallback callback, ResultNotifyCallback callback2) {

        getDrinkingRecordCallback = callback;
        getDrinkingRecordEndingCallback = callback2;
        byte[] values = new byte[20];
        values[0] = (byte) COMMAND_ID_GETTING_DRINKING_RECORD;

        values[7] = (byte) ((utcSeconds & 0xFF000000) >> 24);
        values[6] = (byte) ((utcSeconds & 0x00FF0000) >> 16);
        values[5] = (byte) ((utcSeconds & 0x0000FF00) >> 8);
        values[4] = (byte) (utcSeconds & 0x000000FF);
        super.writeTx(values);
    }


    BaseBleOperation.ResultNotifyCallback clearDrinkingRecordCallback = null;

    public void command_clearDrinkingRecord(BaseBleOperation.ResultNotifyCallback callback) {
        clearDrinkingRecordCallback = callback;
        byte[] values = new byte[20];
        values[0] = (byte) COMMAND_ID_CLEAR_DRINKING_RECORD;
        super.writeTx(values);

    }

    BaseBleOperation.ResultNotifyCallback restartDeviceCallback = null;

    public void command_restartDevice(BaseBleOperation.ResultNotifyCallback callback) {

        restartDeviceCallback = callback;
        byte[] values = new byte[20];
        values[0] = (byte) COMMAND_ID_RESTART_DEVICE;
        super.writeTx(values);

    }

    BaseBleOperation.ResultNotifyCallback closeDeviceCallback = null;

    public void command_closeDevice(BaseBleOperation.ResultNotifyCallback callback) {

        closeDeviceCallback = callback;
        byte[] values = new byte[20];
        values[0] = (byte) COMMAND_ID_CLOSE_DEVICE;
        super.writeTx(values);

    }

    private String broadcastFilter(String name) {
        name = name.substring(0, name.length() < 14 ? name.length() : 14);
        byte[] datas = name.getBytes();
        for (int i = 0; i < datas.length; i++) {
            if (datas[i] < 32 || datas[1] > 127) {
                datas[i] = ' ';
            }
        }
        return new String(datas);
    }

    BaseBleOperation.ResultNotifyCallback setDeviceNameCallback = null;

    public void command_setDeviceName(String name, BaseBleOperation.ResultNotifyCallback callback) {

        setDeviceNameCallback = callback;
        String broadcastName = broadcastFilter(name);
        byte[] values = new byte[20];
        values[0] = COMMAND_ID_SETTING_DEVICE_NAME;
        for (int i = 0; i < broadcastName.length(); i++) {

            values[1 + i] = broadcastName.getBytes()[i];

        }
        super.writeTx(values);
    }

    BaseBleOperation.ResultNotifyCallback resetDrikingRecordIndexCallback = null;

    public void command_resetDrinkingRecordIndex(BaseBleOperation.ResultNotifyCallback callback) {
        resetDrikingRecordIndexCallback = callback;

        byte[] values = new byte[20];
        values[0] = (byte) COMMAND_ID_RESET_DRINKING_RECORD_INDEX;
        super.writeTx(values);
    }

    //DeviceDisconveredCallback scan_callback;
    String Signal = null;

    GeneSeedDisconveredCallback scan_callback;


    public void scan(@NonNull String broadcastNameFilter, GeneSeedDisconveredCallback callback) {

        BaseBleOperation.map.clear();
        this.Signal = broadcastNameFilter;
        scan_callback = callback;
        super.mBluetoothAdapter.startLeScan(leCallback);
        super.stopLeScan(false, leCallback);
        super.isScanning = true;


    }

    public void bind() {

        if (!isConnected()) {

            return;
        }

        byte[] command = new byte[16];
        command[0] = 0x32;
        command[15] = 0x32;
        super.writeTx(command);
    }

    // HashMap<String, BluetoothDevice> map = new HashMap();

    private BluetoothAdapter.LeScanCallback leCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {


            if (Signal.equals(SIGNAL_GENE)) {

                if (device.getName() != null && scanRecord[25] == (byte) 71 && scanRecord[26] == (byte) 69) {

                    if (!BaseBleOperation.map.containsKey(device.getAddress())) {

                        BaseBleOperation.map.put(device.getAddress(), device);

                        if (scan_callback != null) {
                            MyRunnable runnable = new MyRunnable() {
                                @Override
                                public void Run(Bundle data) {

                                    String signal = data.getString("signal");
                                    String mac = data.getString("mac");
                                    String name = data.getString("name");
                                    int rssi = data.getInt("rssi");
                                    int remainMinutes2Notification = data.getInt("remainMinutes2Notification");
                                    int CountOfNofication = data.getInt("CountOfNofication");
                                    scan_callback.onDisconvered(signal, mac, name, rssi, remainMinutes2Notification, CountOfNofication);


                                }
                            };

                            Message msg = new Message();
                            msg.obj = runnable;
                            msg.setData(new Bundle());
                            msg.getData().putString("signal", SIGNAL_GENE);
                            msg.getData().putString("mac", device.getAddress());
                            msg.getData().putString("name", device.getName());
                            msg.getData().putInt("rssi", rssi);
                            msg.getData().putInt("remainMinutes2Notification", scanRecord[22]);
                            msg.getData().putInt("CountOfNofication", scanRecord[23]);
                            CupTimeBleOperation.super.handler.handleMessage(msg);
                        }
                    }
                }
            } else {

                if (device.getName() != null && (scanRecord[25] == (byte) 83 && scanRecord[26] == (byte) 68) || ((scanRecord[28] == (byte) 74 && scanRecord[29] == (byte) 67))) {

                    if (!BaseBleOperation.map.containsKey(device.getAddress())) {

                        BaseBleOperation.map.put(device.getAddress(), device);

                        if (scan_callback != null) {
                            MyRunnable runnable = new MyRunnable() {
                                @Override
                                public void Run(Bundle data) {

                                    String signal = data.getString("signal");
                                    String mac = data.getString("mac");
                                    String name = data.getString("name");
                                    int rssi = data.getInt("rssi");
                                    int remainMinutes2Notification = data.getInt("remainMinutes2Notification");
                                    int CountOfNofication = data.getInt("CountOfNofication");
                                    scan_callback.onDisconvered(signal, mac, name, rssi, remainMinutes2Notification, CountOfNofication);

                                }
                            };

                            Message msg = new Message();
                            msg.obj = runnable;
                            msg.setData(new Bundle());
                            msg.getData().putString("signal", SIGNAL_JSTYLE_SEED);
                            msg.getData().putString("mac", device.getAddress());
                            msg.getData().putString("name", device.getName());
                            msg.getData().putInt("rssi", rssi);
                            msg.getData().putInt("remainMinutes2Notification", scanRecord[22]);
                            msg.getData().putInt("CountOfNofication", scanRecord[23]);
                            CupTimeBleOperation.super.handler.handleMessage(msg);

                        }
                    }
                }
            }

        }
    };

    public interface GetDateTimeCallback {

        void onGetDateTime(Calendar c, int battery);

    }

    public interface GetDailyDrinkingGoalCallback {

        void onGetDailyDrinkingGoal(int ml);

    }

    public interface GetPersonalInfoCallback {

        void onGetPersonalInfo(int weight, int stature, int birthYear, int sex);

    }

    public interface GetDrinkingRecordCallback {

        void onGetDrinkingRecordCallback(long utcSeconds, int drinkingML, int temperature, int percent);
    }

    public interface GeneSeedDisconveredCallback {

        void onDisconvered(String signal, String mac, String name, int rssi, int remainMinutes2Notification, int CountOfNofication);

    }

}

