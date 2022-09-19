package com.youhong.cuptime.blesdk;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import android.util.Log;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.File;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import no.nordicsemi.android.dfu.DfuBaseService;

/**
 * Created by Administrator on 2017/1/20.
 */
public class BleOperation {

    private static final int COMMAND_ID_SETTING_TIME = 0x1;
    private static final int COMMAND_ID_GETTING_TIME = 0x41;
    private static final int COMMAND_ID_SETTING_BROADCAST_NAME = 0x2;
    private static final int COMMAND_ID_GETTING_BROADCAST_NAME = 0x42;
    private static final int COMMAND_ID_SETTING_STORAGE = 0x3;
    private static final int COMMAND_ID_GETTING_STORAGE = 0x43;
    private static final int COMMAND_ID_SETTING_ID = 0x5;
    private static final int COMMAND_ID_GETTING_ID = 0x45;
    private static final int COMMAND_ID_GETTING_DATA = 0x7;
    private static final int COMMAND_ID_ENABLE_RT_DATA = 0x9;
    private static final int COMMAND_ID_DISABLE_RT_DATA = 0x49;

    private static final int COMMAND_ID_OTA = 0x47;
    private static final int COMMAND_ID_GETTING_VERSION = 0x27;
    private static final int COMMAND_ID_RESET = 0x12;

    private static final int SHORTENED_LOCAL_NAME = 0x08;
    private static final int COMPLETE_LOCAL_NAME = 0x09;
    private static final int DURATION_OF_SCANNING = 15000;

    private Context mContext;
    private RFLampDevice mDevice;

    private boolean isConnected = false;
    private boolean isStateBCRegistered = false;
    private BluetoothAdapter adapter;
    LocalBroadcastManager local_bcm = null;
    private Timer mTimer;


    Handler handler;


    public BleOperation(Context context) {
        this.mContext = context;
        adapter = BluetoothAdapter.getDefaultAdapter();
        local_bcm = LocalBroadcastManager.getInstance(mContext);

        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {

                if (msg.obj != null) {

                    ((MyRunnable) msg.obj).Run(msg.getData());

                }
            }
        };
        mTimer = new Timer();
    }

    /**
     * 获取当前SDK层是否已连接上设备
     *
     * @return 返回当前SDK层是否已经连接设备, True表示已连接, Flase表示未连接
     */
    public boolean isConnected() {
        return isConnected;
    }


    private void registerBC() {

        if (!isStateBCRegistered) {

            IntentFilter filter = new IntentFilter();
            filter.addAction(LightBLEService.ACTION_DATA_AVAILABLE);
            filter.addAction(RFLampDevice.ACTION_SERVICES_DISCOVERED);
            filter.addAction(LightBLEService.ACTION_GATT_DISCONNECTED);
            filter.addAction(LightBLEService.ACTION_GATT_CONNECTED);
            mContext.registerReceiver(state_receiver, filter);
            isStateBCRegistered = true;
        }
    }

    private void unregisterBC() {
        if (isStateBCRegistered) {

            mContext.unregisterReceiver(state_receiver);
            isStateBCRegistered = false;

        }
    }


    BroadcastReceiver ota_receive = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction() == DfuBaseService.BROADCAST_ERROR) {

                is_OTA_Disconnected = false;
                if (ota_callback != null) {

                    mContext.stopService(intent);
                    unregister_OTA_BC();

                    MyRunnable runnable20 = new MyRunnable() {
                        @Override
                        public void Run(Bundle data) {

                            ota_callback.OtaProgressCallback(PROGRESS_FAILED);

                        }
                    };
                    Message msg20 = new Message();
                    msg20.obj = runnable20;
                    handler.handleMessage(msg20);
                }

            } else if (intent.getAction() == DfuBaseService.BROADCAST_PROGRESS) {

             /* 02-08 17:08:27.090 7489-7489/com.youhong.bletest_j1663 E/Progress:: -2
                02-08 17:08:29.750 7489-7489/com.youhong.bletest_j1663 E/Progress:: 0
                02-08 17:08:29.910 7489-7489/com.youhong.bletest_j1663 E/Progress:: 1
                02-08 17:08:30.230 7489-7489/com.youhong.bletest_j1663 E/Progress:: 2
                02-08 17:08:30.450 7489-7489/com.youhong.bletest_j1663 E/Progress:: 3
                02-08 17:08:30.700 7489-7489/com.youhong.bletest_j1663 E/Progress:: 4
                02-08 17:08:30.990 7489-7489/com.youhong.bletest_j1663 E/Progress:: 5
                02-08 17:08:31.180 7489-7489/com.youhong.bletest_j1663 E/Progress:: 6
                02-08 17:08:31.480 7489-7489/com.youhong.bletest_j1663 E/Progress:: 7
                02-08 17:08:31.690 7489-7489/com.youhong.bletest_j1663 E/Progress:: 8
                02-08 17:08:32.040 7489-7489/com.youhong.bletest_j1663 E/Progress:: 9
                02-08 17:08:32.220 7489-7489/com.youhong.bletest_j1663 E/Progress:: 10
                02-08 17:08:32.490 7489-7489/com.youhong.bletest_j1663 E/Progress:: 11
                02-08 17:08:32.700 7489-7489/com.youhong.bletest_j1663 E/Progress:: 12
                02-08 17:08:33.020 7489-7489/com.youh ong.bletest_j1663 E/Progress:: 13
                02-08 17:08:33.210 7489-7489/com.youhong.bletest_j1663 E/Progress:: 14
                02-08 17:08:33.440 7489-7489/com.youhong.bletest_j1663 E/Progress:: 15
                02-08 17:08:33.810 7489-7489/com.youhong.bletest_j1663 E/Progress:: 16
                02-08 17:08:34.060 7489-7489/com.youhong.bletest_j1663 E/Progress:: 17
                02-08 17:08:34.320 7489-7489/com.youhong.bletest_j1663 E/Progress:: 18
                02-08 17:08:34.460 7489-7489/com.youhong.bletest_j1663 E/Progress:: 19
                02-08 17:08:34.730 7489-7489/com.youhong.bletest_j1663 E/Progress:: 20
                02-08 17:08:34.940 7489-7489/com.youhong.bletest_j1663 E/Progress:: 21
                02-08 17:08:35.210 7489-7489/com.youhong.bletest_j1663 E/Progress:: 22
                02-08 17:08:35.440 7489-7489/com.youhong.bletest_j1663 E/Progress:: 23
                02-08 17:08:35.790 7489-7489/com.youhong.bletest_j1663 E/Progress:: 24
                02-08 17:08:36.040 7489-7489/com.youhong.bletest_j1663 E/Progress:: 25
                02-08 17:08:36.360 7489-7489/com.youhong.bletest_j1663 E/Progress:: 26
                02-08 17:08:36.570 7489-7489/com.youhong.bletest_j1663 E/Progress:: 27
                02-08 17:08:36.800 7489-7489/com.youhong.bletest_j1663 E/Progress:: 28
                02-08 17:08:37.130 7489-7489/com.youhong.bletest_j1663 E/Progress:: 29
                02-08 17:08:37.320 7489-7489/com.youhong.bletest_j1663 E/Progress:: 30
                02-08 17:08:37.640 7489-7489/com.youhong.bletest_j1663 E/Progress:: 31
                02-08 17:08:37.880 7489-7489/com.youhong.bletest_j1663 E/Progress:: 32
                02-08 17:08:38.180 7489-7489/com.youhong.bletest_j1663 E/Progress:: 33
                02-08 17:08:38.470 7489-7489/com.youhong.bletest_j1663 E/Progress:: 34
                02-08 17:08:38.750 7489-7489/com.youhong.bletest_j1663 E/Progress:: 35
                02-08 17:08:39.010 7489-7489/com.youhong.bletest_j1663 E/Progress:: 36
                02-08 17:08:39.320 7489-7489/com.youhong.bletest_j1663 E/Progress:: 37
                02-08 17:08:39.540 7489-7489/com.youhong.bletest_j1663 E/Progress:: 38
                02-08 17:08:39.700 7489-7489/com.youhong.bletest_j1663 E/Progress:: 39
                02-08 17:08:40.070 7489-7489/com.youhong.bletest_j1663 E/Progress:: 40
                02-08 17:08:40.250 7489-7489/com.youhong.bletest_j1663 E/Progress:: 41
                02-08 17:08:40.570 7489-7489/com.youhong.bletest_j1663 E/Progress:: 42
                02-08 17:08:40.740 7489-7489/com.youhong.bletest_j1663 E/Progress:: 43
                02-08 17:08:41.120 7489-7489/com.youhong.bletest_j1663 E/Progress:: 44
                02-08 17:08:41.370 7489-7489/com.youhong.bletest_j1663 E/Progress:: 45
                02-08 17:08:41.710 7489-7489/com.youhong.bletest_j1663 E/Progress:: 46
                02-08 17:08:41.920 7489-7489/com.youhong.bletest_j1663 E/Progress:: 47
                02-08 17:08:42.310 7489-7489/com.youhong.bletest_j1663 E/Progress:: 48
                02-08 17:08:42.600 7489-7489/com.youhong.bletest_j1663 E/Progress:: 49
                02-08 17:08:43.000 7489-7489/com.youhong.bletest_j1663 E/Progress:: 50
                02-08 17:08:43.380 7489-7489/com.youhong.bletest_j1663 E/Progress:: 51
                02-08 17:08:43.730 7489-7489/com.youhong.bletest_j1663 E/Progress:: 52
                02-08 17:08:44.060 7489-7489/com.youhong.bletest_j1663 E/Progress:: 53
                02-08 17:08:44.330 7489-7489/com.youhong.bletest_j1663 E/Progress:: 54
                02-08 17:08:44.690 7489-7489/com.youhong.bletest_j1663 E/Progress:: 55
                02-08 17:08:44.990 7489-7489/com.youhong.bletest_j1663 E/Progress:: 56
                02-08 17:08:45.370 7489-7489/com.youhong.bletest_j1663 E/Progress:: 57
                02-08 17:08:45.590 7489-7489/com.youhong.bletest_j1663 E/Progress:: 58
                02-08 17:08:45.860 7489-7489/com.youhong.bletest_j1663 E/Progress:: 59
                02-08 17:08:46.060 7489-7489/com.youhong.bletest_j1663 E/Progress:: 60
                02-08 17:08:46.310 7489-7489/com.youhong.bletest_j1663 E/Progress:: 61
                02-08 17:08:46.520 7489-7489/com.youhong.bletest_j1663 E/Progress:: 62
                02-08 17:08:46.850 7489-7489/com.youhong.bletest_j1663 E/Progress:: 63
                02-08 17:08:47.010 7489-7489/com.youhong.bletest_j1663 E/Progress:: 64
                02-08 17:08:47.210 7489-7489/com.youhong.bletest_j1663 E/Progress:: 65
                02-08 17:08:47.470 7489-7489/com.youhong.bletest_j1663 E/Progress:: 66
                02-08 17:08:47.780 7489-7489/com.youhong.bletest_j1663 E/Progress:: 67
                02-08 17:08:48.030 7489-7489/com.youhong.bletest_j1663 E/Progress:: 68
                02-08 17:08:48.230 7489-7489/com.youhong.bletest_j1663 E/Progress:: 69
                02-08 17:08:48.620 7489-7489/com.youhong.bletest_j1663 E/Progress:: 70
                02-08 17:08:48.770 7489-7489/com.youhong.bletest_j1663 E/Progress:: 71
                02-08 17:08:49.150 7489-7489/com.youhong.bletest_j1663 E/Progress:: 72
                02-08 17:08:49.410 7489-7489/com.youhong.bletest_j1663 E/Progress:: 73
                02-08 17:08:49.670 7489-7489/com.youhong.bletest_j1663 E/Progress:: 74
                02-08 17:08:49.850 7489-7489/com.youhong.bletest_j1663 E/Progress:: 75
                02-08 17:08:50.070 7489-7489/com.youhong.bletest_j1663 E/Progress:: 76
                02-08 17:08:50.410 7489-7489/com.youhong.bletest_j1663 E/Progress:: 77
                02-08 17:08:50.590 7489-7489/com.youhong.bletest_j1663 E/Progress:: 78
                02-08 17:08:50.930 7489-7489/com.youhong.bletest_j1663 E/Progress:: 79
                02-08 17:08:51.140 7489-7489/com.youhong.bletest_j1663 E/Progress:: 80
                02-08 17:08:51.470 7489-7489/com.youhong.bletest_j1663 E/Progress:: 81
                02-08 17:08:51.640 7489-7489/com.youhong.bletest_j1663 E/Progress:: 82
                02-08 17:08:51.970 7489-7489/com.youhong.bletest_j1663 E/Progress:: 83
                02-08 17:08:52.170 7489-7489/com.youhong.bletest_j1663 E/Progress:: 84
                02-08 17:08:52.480 7489-7489/com.youhong.bletest_j1663 E/Progress:: 85
                02-08 17:08:52.660 7489-7489/com.youhong.bletest_j1663 E/Progress:: 86
                02-08 17:08:52.950 7489-7489/com.youhong.bletest_j1663 E/Progress:: 87
                02-08 17:08:53.200 7489-7489/com.youhong.bletest_j1663 E/Progress:: 88
                02-08 17:08:53.410 7489-7489/com.youhong.bletest_j1663 E/Progress:: 89
                02-08 17:08:53.760 7489-7489/com.youhong.bletest_j1663 E/Progress:: 90
                02-08 17:08:53.960 7489-7489/com.youhong.bletest_j1663 E/Progress:: 91
                02-08 17:08:54.280 7489-7489/com.youhong.bletest_j1663 E/Progress:: 92
                02-08 17:08:54.490 7489-7489/com.youhong.bletest_j1663 E/Progress:: 93
                02-08 17:08:54.790 7489-7489/com.youhong.bletest_j1663 E/Progress:: 94
                02-08 17:08:55.080 7489-7489/com.youhong.bletest_j1663 E/Progress:: 95
                02-08 17:08:55.590 7489-7489/com.youhong.bletest_j1663 E/Progress:: 96
                02-08 17:08:55.850 7489-7489/com.youhong.bletest_j1663 E/Progress:: 97
                02-08 17:08:56.220 7489-7489/com.youhong.bletest_j1663 E/Progress:: 98
                02-08 17:08:56.410 7489-7489/com.youhong.bletest_j1663 E/Progress:: 99
                02-08 17:08:56.870 7489-7489/com.youhong.bletest_j1663 E/Progress:: -5*/
                int progress = intent.getIntExtra(DfuBaseService.EXTRA_DATA, 0);

                //  MyLog.e("Progress:" + progress);
                if (progress == 0) {

                    if (ota_callback != null) {

                        MyRunnable runnable21 = new MyRunnable() {
                            @Override
                            public void Run(Bundle data) {
                                ota_callback.OtaProgressCallback(PROGRESS_READY);

                            }
                        };

                        Message msg21 = new Message();
                        msg21.obj = runnable21;
                        handler.handleMessage(msg21);

                    }

                } else if (progress > 0 && progress < 100) {
                    if (ota_callback != null) {


                        MyRunnable runnable21 = new MyRunnable() {
                            @Override
                            public void Run(Bundle data) {

                                int progress = data.getInt("progress");
                                ota_callback.OtaProgressCallback(progress);

                            }
                        };

                        Message msg21 = new Message();
                        msg21.obj = runnable21;
                        Bundle bundle = new Bundle();
                        bundle.putInt("progress", progress);
                        msg21.setData(bundle);
                        handler.handleMessage(msg21);
                    }
                } else if (progress == -5) {

                    unregister_OTA_BC();
                    mContext.stopService(BleOperation.this.intent);
                    if (mDevice != null) {

                        OTA_Reconnect();

                    } else {

                        MyRunnable runnable22 = new MyRunnable() {
                            @Override
                            public void Run(Bundle data) {

                                ota_callback.OtaProgressCallback(PROGRESS_FINISHED);

                            }
                        };
                        Message msg22 = new Message();
                        msg22.obj = runnable22;
                        handler.handleMessage(msg22);

                    }
                }
            }

        }
    };


    boolean isFirstTimeConnected = true;

    BroadcastReceiver state_receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction() == LightBLEService.ACTION_DATA_AVAILABLE) {

                byte[] values = intent.getByteArrayExtra(LightBLEService.EXTRA_DATA);


                for (int i = 0; i < values.length; i++) {

                    values[i] = (byte) (values[i] ^ 0x55);

                }
                switch (values[0]) {

                    case COMMAND_ID_SETTING_TIME:

                        MyRunnable runnable = new MyRunnable() {
                            @Override
                            public void Run(Bundle data) {

                                if (setTime_callback != null) {
                                    setTime_callback.onResultNotify(true);
                                }
                            }
                        };

                        Message msg = new Message();
                        msg.obj = runnable;
                        handler.handleMessage(msg);
                        break;

                    case COMMAND_ID_GETTING_TIME:
                        if (getDateTime_callback != null) {

                            Calendar c = Calendar.getInstance();
                            int year = Util.ConvertBCD2Decimal(values[1]) + 2000;
                            int month = Util.ConvertBCD2Decimal(values[2]) - 1;
                            int day = Util.ConvertBCD2Decimal(values[3]);
                            int hour = Util.ConvertBCD2Decimal(values[4]);
                            int minute = Util.ConvertBCD2Decimal(values[5]);
                            int second = Util.ConvertBCD2Decimal(values[6]);

                            c.set(Calendar.YEAR, year);
                            c.set(Calendar.MONTH, month);
                            c.set(Calendar.DAY_OF_MONTH, day);
                            c.set(Calendar.HOUR_OF_DAY, hour);
                            c.set(Calendar.MINUTE, minute);
                            c.set(Calendar.SECOND, second);


                            MyRunnable runnable1 = new MyRunnable() {
                                @Override
                                public void Run(Bundle data) {

                                    if (getDateTime_callback != null) {
                                        Calendar c = (Calendar) data.getSerializable("c");
                                        getDateTime_callback.ongetDateTime(c);
                                    }
                                }
                            };
                            Message msg1 = new Message();
                            msg1.obj = runnable1;
                            Bundle bundle = new Bundle();
                            bundle.putSerializable("c", c);
                            msg1.setData(bundle);
                            handler.handleMessage(msg1);
                        }
                        break;
                    case COMMAND_ID_SETTING_BROADCAST_NAME:
                        if (setBroadcastName_callback != null)
                            setBroadcastName_callback.onResultNotify(true);
                        break;
                    case COMMAND_ID_GETTING_BROADCAST_NAME:
                        if (getBraodcast_callback != null) {
                            String name = "";
                            try {
                                name = new String(values, 1, 14, "ascii");
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }

                            MyRunnable runnable2 = new MyRunnable() {
                                @Override
                                public void Run(Bundle data) {

                                    String name = data.getString("name");

                                    if (getBraodcast_callback != null) {
                                        getBraodcast_callback.onGetBroadcastingName(name);
                                    }
                                }
                            };
                            Message msg2 = new Message();
                            msg2.obj = runnable2;
                            Bundle bundle2 = new Bundle();
                            bundle2.putString("name", name);
                            msg2.setData(bundle2);
                            handler.handleMessage(msg2);
                        }
                        break;
                    case COMMAND_ID_SETTING_STORAGE:

                        MyRunnable runnable3 = new MyRunnable() {
                            @Override
                            public void Run(Bundle data) {
                                if (setStorageStatus_callback != null) {
                                    setStorageStatus_callback.onResultNotify(true);
                                }

                            }
                        };
                        Message msg3 = new Message();
                        msg3.obj = runnable3;
                        handler.handleMessage(msg3);

                        break;
                    case COMMAND_ID_GETTING_STORAGE:

                        MyRunnable runnable4 = new MyRunnable() {
                            @Override
                            public void Run(Bundle data) {
                                byte[] values = data.getByteArray("values");
                                if (getStorageStatus_callback != null) {
                                    if (values[1] == 0x1)

                                        getStorageStatus_callback.onResultNotify(true);

                                    else
                                        getStorageStatus_callback.onResultNotify(false);
                                }

                            }
                        };
                        Bundle bundle4 = new Bundle();
                        bundle4.putByteArray("values", values);
                        Message msg4 = new Message();
                        msg4.obj = runnable4;
                        msg4.setData(bundle4);
                        handler.handleMessage(msg4);
                        break;
                    case COMMAND_ID_SETTING_ID:

                        MyRunnable runnable5 = new MyRunnable() {
                            @Override
                            public void Run(Bundle data) {
                                if (setDeviceId_callback != null) {
                                    setDeviceId_callback.onResultNotify(true);
                                }
                            }
                        };
                        Message msg5 = new Message();
                        msg5.obj = runnable5;
                        handler.handleMessage(msg5);
                        break;
                    case COMMAND_ID_GETTING_ID:
                        if (getDeviceId_callback != null) {

                            byte[] id = new byte[14];
                            for (int i = 0; i < id.length; i++) {
                                id[i] = values[1 + i];

                            }

                            MyRunnable runnable6 = new MyRunnable() {
                                @Override
                                public void Run(Bundle data) {
                                    if (getDeviceId_callback != null) {
                                        byte[] id = data.getByteArray("id");
                                        getDeviceId_callback.onGetDeviceId(id);

                                    }
                                }
                            };
                            Message msg6 = new Message();
                            msg6.obj = runnable6;
                            Bundle bundle6 = new Bundle();
                            bundle6.putByteArray("id", id);
                            msg6.setData(bundle6);
                            handler.handleMessage(msg6);
                        }
                        break;
                    case COMMAND_ID_GETTING_DATA:

                        if (readData_callback != null) {

                            if (values[1] != (byte) 0xFF) {

                                if (values[1] == 0x7
                                        &&
                                        values[2] == 0x7
                                        &&
                                        values[3] == 0x7
                                        &&
                                        values[4] == 0x7
                                        &&
                                        values[5] == 0x7
                                        &&
                                        values[6] == 0x7
                                        &&
                                        values[7] == 0x7
                                        &&
                                        values[8] == 0x7
                                        &&
                                        values[9] == 0x7
                                        &&
                                        values[0xA] == 0x7
                                        &&
                                        values[0xB] == 0x7
                                        ) {

                                    return;
                                }

                                Calendar c1 = Calendar.getInstance();

                                int year = Util.ConvertBCD2Decimal(values[1]) + 2000;
                                int month = Util.ConvertBCD2Decimal(values[2]) - 1;
                                int day = Util.ConvertBCD2Decimal(values[3]);
                                int hour = Util.ConvertBCD2Decimal(values[4]);
                                int minute = Util.ConvertBCD2Decimal(values[5]);
                                int temperature = ((values[6] << 8) & 0x0000FF00) + (values[7] & 0x000000FF);
                                float f_temp = (temperature * 1.0f) / 10;


                                c1.set(Calendar.YEAR, year);
                                c1.set(Calendar.MONTH, month);
                                c1.set(Calendar.DAY_OF_MONTH, day);
                                c1.set(Calendar.HOUR_OF_DAY, hour);
                                c1.set(Calendar.MINUTE, minute);
                                c1.set(Calendar.SECOND, 0);


                                MyRunnable runnable7 = new MyRunnable() {
                                    @Override
                                    public void Run(Bundle data) {

                                        if (readData_callback != null) {

                                            Calendar c1 = (Calendar) data.getSerializable("c1");
                                            float f_temp = data.getFloat("f_temp");
                                            readData_callback.onGetStoredTemperature(c1, f_temp);
                                        }

                                    }
                                };
                                Bundle bundle7 = new Bundle();
                                bundle7.putFloat("f_temp", f_temp);
                                bundle7.putSerializable("c1", c1);
                                Message msg7 = new Message();
                                msg7.obj = runnable7;
                                msg7.setData(bundle7);
                                handler.handleMessage(msg7);

                            }

                            if (values[0xA] != (byte) 0xFF) {

                                Calendar c2 = Calendar.getInstance();
                                int year2 = Util.ConvertBCD2Decimal(values[9 + 1]) + 2000;
                                int month2 = Util.ConvertBCD2Decimal(values[10 + 1]) - 1;
                                int day2 = Util.ConvertBCD2Decimal(values[11 + 1]);
                                int hour2 = Util.ConvertBCD2Decimal(values[12 + 1]);
                                int minute2 = Util.ConvertBCD2Decimal(values[13 + 1]);
                                int temperature2 = ((values[14 + 1] << 8) & 0x0000FF00) + (values[15 + 1] & 0x000000FF);
                                float f_temp2 = (temperature2 * 1.0f) / 10;

                                c2.set(Calendar.YEAR, year2);
                                c2.set(Calendar.MONTH, month2);
                                c2.set(Calendar.DAY_OF_MONTH, day2);
                                c2.set(Calendar.HOUR_OF_DAY, hour2);
                                c2.set(Calendar.MINUTE, minute2);
                                c2.set(Calendar.SECOND, 0);


                                MyRunnable runnable7 = new MyRunnable() {
                                    @Override
                                    public void Run(Bundle data) {

                                        if (readData_callback != null) {

                                            Calendar c1 = (Calendar) data.getSerializable("c1");
                                            float f_temp = data.getFloat("f_temp");
                                            readData_callback.onGetStoredTemperature(c1, f_temp);

                                        }
                                    }
                                };
                                Bundle bundle7 = new Bundle();
                                bundle7.putFloat("f_temp", f_temp2);
                                bundle7.putSerializable("c1", c2);
                                Message msg8 = new Message();
                                msg8.obj = runnable7;
                                msg8.setData(bundle7);
                                handler.handleMessage(msg8);
                            }
                        }

                        break;
                    case COMMAND_ID_ENABLE_RT_DATA:
                        if (enableRealTimeTemperature_callback1 != null) {
                            if (values[1] == 0 &&
                                    values[2] == 0
                                    &&
                                    values[3] == 0
                                    &&
                                    values[4] == 0
                                    &&
                                    values[5] == 0
                                    &&
                                    values[6] == 0
                                    &&
                                    values[7] == 0
                                    &&
                                    values[8] == 0
                                    &&
                                    values[9] == 0
                                    &&
                                    values[0xA] == 0
                                    &&
                                    values[0xB] == 0
                                    ) {

                                MyRunnable runnable10 = new MyRunnable() {
                                    @Override
                                    public void Run(Bundle data) {

                                        enableRealTimeTemperature_callback1.onResultNotify(true);

                                    }
                                };
                                Message msg10 = new Message();
                                msg10.obj = runnable10;
                                handler.handleMessage(msg10);

                            } else {

                                int temperature = ((values[1] << 8) & 0x0000FF00) + (values[2] & 0x000000FF);
                                float f_temp = (temperature * 1.0f) / 10;
                                int battery = values[0x5];

                                int year = Util.ConvertBCD2Decimal(values[8]) + 2000;
                                int month = Util.ConvertBCD2Decimal(values[9]) - 1;
                                int day = Util.ConvertBCD2Decimal(values[0xA]);
                                int hour = Util.ConvertBCD2Decimal(values[0xB]);
                                int minute = Util.ConvertBCD2Decimal(values[0xC]);
                                int second = Util.ConvertBCD2Decimal(values[0xD]);

                                int voltage = ((values[0x6] << 8) & 0x0000FF00) + (values[0x7] & 0x000000FF);


                                Calendar c = Calendar.getInstance();
                                c.set(Calendar.YEAR, year);
                                c.set(Calendar.MONTH, month);
                                c.set(Calendar.DAY_OF_MONTH, day);
                                c.set(Calendar.HOUR_OF_DAY, hour);
                                c.set(Calendar.MINUTE, minute);
                                c.set(Calendar.SECOND, second);

                                MyRunnable runnable11 = new MyRunnable() {
                                    @Override
                                    public void Run(Bundle data) {
                                        Calendar c = (Calendar) data.getSerializable("c");
                                        float f_temp = data.getFloat("f_temp");
                                        int battery = data.getInt("battery");


                                        if (enableRealTimeTemperature_callback2 != null) {

                                            enableRealTimeTemperature_callback2.onRealTimeTemperatureReceived(c, f_temp, battery);
                                        }
                                    }
                                };


                                Message msg11 = new Message();
                                msg11.obj = runnable11;
                                Bundle bundle = new Bundle();
                                bundle.putSerializable("c", c);
                                bundle.putFloat("f_temp", f_temp);
                                bundle.putInt("battery", battery);
                                msg11.setData(bundle);
                                handler.handleMessage(msg11);


                                if (enableRealTimeVoltage_callback != null) {


                                    MyRunnable runnable33 = new MyRunnable() {
                                        @Override
                                        public void Run(Bundle data) {
                                            Calendar c = (Calendar) data.getSerializable("c");
                                            float f_temp = data.getFloat("f_temp");
                                            int battery = data.getInt("battery");
                                            int voltage = data.getInt("voltage");

                                            enableRealTimeVoltage_callback.onRealTimeVoltageReceived(c, f_temp, battery, voltage);
                                        }
                                    };

                                    Message msg33 = new Message();
                                    msg33.obj = runnable33;
                                    Bundle bundle33 = new Bundle();
                                    bundle33.putSerializable("c", c);
                                    bundle33.putFloat("f_temp", f_temp);
                                    bundle33.putInt("battery", battery);
                                    bundle33.putInt("voltage", voltage);
                                    msg33.setData(bundle33);
                                    handler.handleMessage(msg33);
                                }
                            }
                        }
                        break;
                    case COMMAND_ID_DISABLE_RT_DATA:

                        MyRunnable runnable12 = new MyRunnable() {
                            @Override
                            public void Run(Bundle data) {
                                if (disableRealTimeTemperature_callback != null) {

                                    disableRealTimeTemperature_callback.onResultNotify(true);

                                }
                            }
                        };
                        Message msg12 = new Message();
                        msg12.obj = runnable12;
                        handler.handleMessage(msg12);

                        break;
                    case COMMAND_ID_GETTING_VERSION:
                        if (getFirmwareVersion_callback != null) {

                            Calendar c = Calendar.getInstance();

                            int year = Util.ConvertBCD2Decimal(values[8]) + 2000;
                            int month = Util.ConvertBCD2Decimal(values[9]) - 1;
                            int day = Util.ConvertBCD2Decimal(values[10]);

                            c.set(Calendar.YEAR, year);
                            c.set(Calendar.MONTH, month);
                            c.set(Calendar.DAY_OF_MONTH, day);
                            byte[] ver = new byte[3];
                            ver[0] = values[5];
                            ver[1] = values[6];
                            ver[2] = values[7];
                            String version = Util.ShowBytesSeparatedWithChar(ver, '.');

                            MyRunnable runnable13 = new MyRunnable() {
                                @Override
                                public void Run(Bundle data) {

                                    Calendar c = (Calendar) data.getSerializable("c");
                                    String version = data.getString("version");
                                    getFirmwareVersion_callback.onGetFirmwareVersion(version, c);

                                }
                            };
                            Message msg13 = new Message();
                            msg13.obj = runnable13;
                            Bundle bundle = new Bundle();
                            bundle.putSerializable("c", c);
                            bundle.putString("version", version);
                            msg13.setData(bundle);
                            handler.handleMessage(msg13);

                        }
                        break;
                    case COMMAND_ID_RESET:

                        MyRunnable runnable14 = new MyRunnable() {
                            @Override
                            public void Run(Bundle data) {

                                if (reset_callback != null) {

                                    reset_callback.onResultNotify(true);

                                }
                            }
                        };
                        Message msg14 = new Message();
                        msg14.obj = runnable14;
                        handler.handleMessage(msg14);
                        break;
                }
            } else if (intent.getAction() == LightBLEService.ACTION_GATT_CONNECTED) {
                isConnected = true;

                if (isFirstTimeConnected) {

                    isFirstTimeConnected = false;

                }
            } else if (intent.getAction() == RFLampDevice.ACTION_SERVICES_DISCOVERED) {

                if (is_OTA_Disconnected2) {

                    is_OTA_Disconnected2 = false;
                    if (ota_callback != null) {

                        MyRunnable runnable16 = new MyRunnable() {
                            @Override
                            public void Run(Bundle data) {
                                ota_callback.OtaProgressCallback(PROGRESS_FINISHED);
                            }
                        };

                        Message msg16 = new Message();
                        msg16.obj = runnable16;
                        handler.handleMessage(msg16);
                    }

                } else if (isAutoReconnect) {
                    isAutoReconnect = false;
                    if (reconnect_callback != null) {
                        //reconnection successful
                        //Clear up auto-reconnect TimerTask
                        // Reset timeOut value.
                        mTimerTask.cancel();
                        isAlreadyInTimer = false;
                        reconnecting_timeOut_remains = reconnecting_timeOut_value;
                        MyRunnable runnable15 = new MyRunnable() {
                            @Override
                            public void Run(Bundle data) {
                                reconnect_callback.onResultNotify(true);
                            }
                        };

                        Message msg15 = new Message();
                        msg15.obj = runnable15;
                        handler.handleMessage(msg15);

                    }
                } else {

                    if (connect_callback != null) {

                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                MyRunnable runnable17 = new MyRunnable() {
                                    @Override
                                    public void Run(Bundle data) {
                                        connect_callback.onResultNotify(true);
                                    }
                                };

                                Message msg17 = new Message();
                                msg17.obj = runnable17;
                                handler.handleMessage(msg17);
                            }
                        }, 500);

                    }
                }
            } else if (intent.getAction() == LightBLEService.ACTION_GATT_DISCONNECTED) {
                isConnected = false;

                //Disconnect situation : When OTA command is send.
                if (is_OTA_Disconnected && is_OTA_Disconnected2) {
                    isStopScaned = false;
                    is_OTA_Disconnected = false;
                    //mBluetoothAdapter.startLeScan(ota_leScancallback);

                    //Disconnect situation : OTA is finished , failed to reconnect.
                } else if (is_OTA_Disconnected2) {
                    // mDevice.reConnected();
                    mDevice.disconnectedDevice();
                    BluetoothDevice device = mDevice.device;

                    Log.e("abc", "" + (device == null));
                    mDevice = new RFLampDevice(mContext, device);
                } else {
                    if (isManualDisconnected) {
                        isManualDisconnected = false;
                        unregisterBC();
                        if (manuallyDisconnect_callback != null) {

                            MyRunnable runnable18 = new MyRunnable() {
                                @Override
                                public void Run(Bundle data) {

                                    manuallyDisconnect_callback.onResultNotify(true);
                                }
                            };

                            Message msg18 = new Message();
                            msg18.obj = runnable18;
                            handler.handleMessage(msg18);

                        }

                    } else {
                        // Report UI Layer
                        if (isFirstTimeConnected) {
                            MyRunnable runnable19 = new MyRunnable() {
                                @Override
                                public void Run(Bundle data) {
                                    connect_callback.onResultNotify(false);
                                }
                            };
                            Message msg19 = new Message();
                            msg19.obj = runnable19;
                            handler.handleMessage(msg19);

                        } else {


                            if (!isAlreadyInTimer) {
                                if (disconnect_callback != null) {
                                    MyRunnable runnable25 = new MyRunnable() {
                                        @Override
                                        public void Run(Bundle data) {


                                            disconnect_callback.onResultNotify(true);


                                        }
                                    };
                                    Message msg25 = new Message();
                                    msg25.obj = runnable25;
                                    handler.handleMessage(msg25);
                                }


                            } else {

                                MyRunnable runnable25 = new MyRunnable() {
                                    @Override
                                    public void Run(Bundle data) {
                                        reconnect_callback.onResultNotify(false);
                                    }
                                };
                                Message msg25 = new Message();
                                msg25.obj = runnable25;
                                handler.handleMessage(msg25);
                            }

                            scanForDevice(mDevice.device, inner_callback);
                        }
                    }
                }
            }
        }
    };

    ResultNotifyCallback inner_callback = new ResultNotifyCallback() {
        @Override
        public void onResultNotify(boolean result) {
            if (result) {
                isAutoReconnect = true;

            } else {
                if (reconnect_callback != null) {

                    MyRunnable runnable20 = new MyRunnable() {
                        @Override
                        public void Run(Bundle data) {
                            //reconnection failed.
                            if (reconnect_callback != null) {
                                reconnect_callback.onResultNotify(false);
                            }
                        }
                    };

                    Message msg20 = new Message();
                    msg20.obj = runnable20;
                    handler.handleMessage(msg20);
                }
            }
        }
    };

    boolean isAlreadyInTimer = false;

    TimerTask mTimerTask = null;

    /**
     * 设置重连超时的时间值,单位为秒 默认为10 minutes,请在调用BleOperation.Connect 之前设置该值
     *
     * @param reconnecting_timeout_settings 设置重连超时的时间值。最好为25seconds 的倍数。
     */
    public void setReconnecting_timeOut_value(long reconnecting_timeOut_value) {
        this.reconnecting_timeOut_value = reconnecting_timeOut_value;
        this.reconnecting_timeOut_remains = reconnecting_timeOut_value;
    }

    private long reconnecting_timeOut_value = 60 * 10;  //10mins
    private long reconnecting_timeOut_remains = reconnecting_timeOut_value;

    private void scanForDevice(final BluetoothDevice dev, final ResultNotifyCallback callback) {
        if (isAlreadyInTimer) {

            return;
        }
        if (dev == null) {
            MyLog.e("dev == null");
            return;
        }

        mTimer.scheduleAtFixedRate(

                mTimerTask = new TimerTask() {
                    @Override
                    public void run() {

                        reconnecting_timeOut_remains -= 25;
                        if (reconnecting_timeOut_remains < 25) {
                            this.cancel();
                            isAlreadyInTimer = false;
                            if (timeout_callback != null) {

                                MyRunnable runnable = new MyRunnable() {
                                    @Override
                                    public void Run(Bundle data) {

                                        isFirstTimeConnected = true;
                                        timeout_callback.ReconnectingTimeOutNotify();

                                    }
                                };
                                Message msg = new Message();
                                msg.obj = runnable;
                                handler.handleMessage(msg);
                            }
                            return;
                        }


                        isStopScaned = false;
                        final BluetoothAdapter.LeScanCallback ble_callback = new BluetoothAdapter.LeScanCallback() {
                            @Override
                            public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {

                                if (dev.getAddress().equals(device.getAddress())) {
                                    if (isStopScaned == false) {
                                        adapter.stopLeScan(this);
                                        mDevice.reConnected();
                                        callback.onResultNotify(true);
                                        isStopScaned = true;
                                    }
                                }
                            }
                        };
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {

                                if (!isStopScaned) {
                                    adapter.stopLeScan(ble_callback);
                                    isStopScaned = true;
                                    //callback.onResultNotify(false);
                                }
                            }
                        }, 5000);
                        BleOperation.this.adapter.startLeScan(ble_callback);
                    }
                }, 0, 25000);

        isAlreadyInTimer = true;
        // final boolean isScanningStop = false;
    }

    boolean isAutoReconnect = false;

    private void OTA_Reconnect() {

        mDevice.reConnected();

    }

    boolean isStopScaned = true;

    private BluetoothAdapter.LeScanCallback ota_leScancallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {

            if ((isStopScaned == false) && device.getName() != null && device.getName().toLowerCase().equals(DFU_BC_NAME)) {
                adapter.stopLeScan(ota_leScancallback);
                handler.removeCallbacks(ota_watchDog);
                isStopScaned = true;
                name = device.getName();
                mac = device.getAddress();
                ota_handler.sendEmptyMessage(0);

            }
        }
    };

    public interface RealTimeVoltageReceivedCallback {

        public void onRealTimeVoltageReceived(Calendar c, float temperature, int battery, int voltage);

    }


    //Normal setting callback
    public interface ResultNotifyCallback {

        public void onResultNotify(boolean result);

    }


    //For syncTime
    public interface GetDateTimeCallback {

        public void ongetDateTime(Calendar c);

    }

    //For GetBroacastingName
    public interface GetBroadcastingNameCallback {

        public void onGetBroadcastingName(String broadcastName);

    }
    //For getStoringStauts

    //For getDeviceId
    public interface GetDeviceIdCallback {

        public void onGetDeviceId(byte[] deviceId);
    }

    public interface GetStoredTemperatureCallback {

        public void onGetStoredTemperature(Calendar c, float temperature);

    }

    public interface RealTimeTemperatureReceivedCallback {

        /**
         * @param temperature     温度值
         * @param batteryReamains 0~3档位的电量
         */
        public void onRealTimeTemperatureReceived(Calendar calendar, float temperature, int batteryReamains);

    }

    public interface GetFirmwareVersionCallback {

        /**
         * @param versionId Firmware版本号
         * @param date      Firmware 编译日期 有效值(年,月,日)
         */
        public void onGetFirmwareVersion(String versionId, Calendar date);

    }

    public interface DeviceDisconveredCallback {
        public void onNewDeviceDisconvered(String deviceName, String mac, int rssi);
    }

    public interface DeviceOtaProgressCallback {

        /**
         * @param progress 从-1 ~ 100
         */
        public void OtaProgressCallback(int progress);

    }

    public interface ReconnectingTimeOutCallback {

        public void ReconnectingTimeOutNotify();

    }

    ResultNotifyCallback setTime_callback;

    private boolean printLog() {
        if (!isConnected) {
            Log.e("Notify", "Device not connected");
            return false;
        }
        return true;
    }

    private byte Crc(byte[] value) {
        byte c = 0;
        for (int i = 0; i < 19; i++) {
            c += value[i];
        }
        return c;
    }

    /**
     * 为设备设置时间
     *
     * @param dateTime 设置的时间数据.
     * @param callback 设置成功时的回调.
     */
    public void command_setTime(Calendar dateTime, ResultNotifyCallback callback) {

        if (!printLog()) {

            return;
        }
        byte[] values = new byte[20];
        values[0] = COMMAND_ID_SETTING_TIME;
        values[1] = Util.ConvertDecimal2BCD(((byte) (dateTime.get(Calendar.YEAR) - 2000)));
        values[2] = Util.ConvertDecimal2BCD(((byte) (dateTime.get(Calendar.MONTH) + 1)));
        values[3] = (Util.ConvertDecimal2BCD((byte) dateTime.get(Calendar.DAY_OF_MONTH)));
        values[4] = (Util.ConvertDecimal2BCD((byte) dateTime.get(Calendar.HOUR_OF_DAY)));
        values[5] = (Util.ConvertDecimal2BCD((byte) dateTime.get(Calendar.MINUTE)));
        values[6] = (Util.ConvertDecimal2BCD((byte) dateTime.get(Calendar.SECOND)));
        this.setTime_callback = callback;
        values[19] = Crc(values);
        mDevice.writeTx(values);
    }

    GetDateTimeCallback getDateTime_callback;

    /**
     * 获取设备的当前时间
     *
     * @param callback 获取成功时的回调
     */
    public void command_getTime(GetDateTimeCallback callback) {
        if (!printLog()) {

            return;
        }
        this.getDateTime_callback = callback;
        byte[] values = new byte[20];
        values[0] = COMMAND_ID_GETTING_TIME;
        values[19] = Crc(values);
        mDevice.writeTx(values);
    }

    ResultNotifyCallback setBroadcastName_callback;

    /**
     * 为设备设置广播时的名称
     *
     * @param broadcastName 设备名称,字符集为ascii, 且最大长度不超过14个字符
     * @param callback      设置成功时的回调
     */
    public void command_setBroadcastName(String broadcastName, ResultNotifyCallback callback) {

        if (!printLog()) {

            return;
        }
        broadcastName = broadcastFilter(broadcastName);
        byte[] values = new byte[20];
        values[0] = COMMAND_ID_SETTING_BROADCAST_NAME;
        for (int i = 0; i < broadcastName.length(); i++) {

            values[1 + i] = broadcastName.getBytes()[i];

        }
        values[19] = Crc(values);
        this.setBroadcastName_callback = callback;
        mDevice.writeTx(values);
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

    GetBroadcastingNameCallback getBraodcast_callback;

    /**
     * 获取设备当前的广播名称
     *
     * @param callback 获取成功时返回的回调
     */
    public void command_getBroadacastName(GetBroadcastingNameCallback callback) {
        if (!printLog()) {
            return;
        }

        byte[] values = new byte[20];
        values[0] = COMMAND_ID_GETTING_BROADCAST_NAME;
        values[19] = Crc(values);

        this.getBraodcast_callback = callback;
        mDevice.writeTx(values);
    }

    ResultNotifyCallback setStorageStatus_callback;

    /**
     * 为设备设置存储状态,status状态标识是否将每次测量的温度记录在体温计内
     *
     * @param status   true时开启,false时关闭
     * @param callback 设置成功时调用的回调
     */
    public void command_setStorageStatus(boolean status, ResultNotifyCallback callback) {
        if (!printLog()) {
            return;
        }
        byte[] values = new byte[20];
        values[0] = COMMAND_ID_SETTING_STORAGE;
        if (status)
            values[1] = 0x1;

        values[19] = Crc(values);
        setStorageStatus_callback = callback;
        mDevice.writeTx(values);
    }

    ResultNotifyCallback getStorageStatus_callback;

    /**
     * 获取设备当前的存储状态
     *
     * @param callback 获取成功时调用的回调
     */
    public void command_getStorageStatus(ResultNotifyCallback callback) {
        if (!printLog()) {
            return;
        }

        byte[] values = new byte[20];
        values[0] = COMMAND_ID_GETTING_STORAGE;
        values[19] = Crc(values);
        getStorageStatus_callback = callback;
        mDevice.writeTx(values);
    }

    ResultNotifyCallback setDeviceId_callback;

    /**
     * 为设备设置DeviceId
     *
     * @param deviceId 14个字节长度且值从0x0~0xF的设备ID
     * @param callback 设置成功时调用的回调
     */
    public void command_setDeviceId(byte[] deviceId, ResultNotifyCallback callback) {
        if (!printLog()) {
            return;
        }
        byte[] values = new byte[20];
        values[0] = COMMAND_ID_SETTING_ID;

        for (int i = 0; i < deviceId.length && i < 14; i++) {
            values[1 + i] = deviceId[i];
        }
        values[19] = Crc(values);
        setDeviceId_callback = callback;
        mDevice.writeTx(values);
    }

    GetDeviceIdCallback getDeviceId_callback;

    /**
     * 获取设备当前的DeviceId
     *
     * @param callback 获取成功时调用的回调
     */
    public void command_getDeviceId(GetDeviceIdCallback callback) {
        if (!printLog()) {

            return;
        }
        byte[] values = new byte[20];
        values[0] = COMMAND_ID_GETTING_ID;
        values[19] = Crc(values);

        this.getDeviceId_callback = callback;
        mDevice.writeTx(values);
    }

    ResultNotifyCallback enableRealTimeTemperature_callback1;
    RealTimeTemperatureReceivedCallback enableRealTimeTemperature_callback2;

    public void setEnableRealTimeVoltage_callback(RealTimeVoltageReceivedCallback enableRealTimeVoltage_callback) {
        this.enableRealTimeVoltage_callback = enableRealTimeVoltage_callback;
    }

    RealTimeVoltageReceivedCallback enableRealTimeVoltage_callback;

    /**
     * 开启实时温度反馈,间隔为10s
     *
     * @param callback1 开启成功时将会调用的回调
     * @param callback2 温度和电量反馈时调用的回调
     */
    public void command_enableRealTimeTemperature(ResultNotifyCallback callback1, RealTimeTemperatureReceivedCallback callback2) {
        if (!printLog()) {

            return;
        }

        byte[] values = new byte[20];
        values[0] = COMMAND_ID_ENABLE_RT_DATA;
        values[19] = Crc(values);
        enableRealTimeTemperature_callback1 = callback1;
        enableRealTimeTemperature_callback2 = callback2;
        mDevice.writeTx(values);
    }

    ResultNotifyCallback disableRealTimeTemperature_callback;

    /**
     * 关闭实时温度反馈,间隔为10s
     *
     * @param callback 关闭成功时调用的回调
     */
    public void command_disableRealTimeTemperature(ResultNotifyCallback callback) {
        if (!printLog()) {

            return;
        }
        byte[] values = new byte[20];
        values[0] = COMMAND_ID_DISABLE_RT_DATA;
        values[19] = Crc(values);

        disableRealTimeTemperature_callback = callback;
        mDevice.writeTx(values);
    }

    GetFirmwareVersionCallback getFirmwareVersion_callback;

    /**
     * 获取设备的当前版本号
     *
     * @param callback 获取成功时调用的回调
     */
    public void command_getFirmwareVersion(GetFirmwareVersionCallback callback) {
        if (!printLog()) {

            return;
        }
        byte[] values = new byte[20];
        values[0] = COMMAND_ID_GETTING_VERSION;
        values[19] = Crc(values);

        getFirmwareVersion_callback = callback;
        mDevice.writeTx(values);
    }

    ResultNotifyCallback reset_callback;

    /**
     * 重置设备
     *
     * @param callback 设置成功时调用的回调
     */
    public void command_reset(ResultNotifyCallback callback) {

        if (!printLog()) {

            return;
        }

        byte[] values = new byte[20];
        values[0] = COMMAND_ID_RESET;
        values[19] = Crc(values);

        reset_callback = callback;
        mDevice.writeTx(values);
    }

    GetStoredTemperatureCallback readData_callback;

    /**
     * 获取设备中存储的历史温度 ,只有最近的24小时内的数据
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param callback  数据返回回调
     */
    public void command_readData(Calendar startTime, Calendar endTime, GetStoredTemperatureCallback callback) {

        readData_callback = callback;
        byte start_year = (byte) (startTime.get(Calendar.YEAR) - 2000);
        byte start_month = (byte) (startTime.get(Calendar.MONTH) + 1);
        byte start_day = (byte) (startTime.get(Calendar.DAY_OF_MONTH));
        byte start_hour = (byte) (startTime.get(Calendar.HOUR_OF_DAY));
        byte start_minute = (byte) (startTime.get(Calendar.MINUTE));

        byte end_year = (byte) (endTime.get(Calendar.YEAR) - 2000);
        byte end_month = (byte) (endTime.get(Calendar.MONTH) + 1);
        byte end_day = (byte) (endTime.get(Calendar.DAY_OF_MONTH));
        byte end_hour = (byte) (endTime.get(Calendar.HOUR_OF_DAY));
        byte end_minute = (byte) (endTime.get(Calendar.MINUTE));
        byte[] value = new byte[20];

        value[0] = 0x7;
        value[1] = Util.ConvertDecimal2BCD(start_year);
        value[2] = Util.ConvertDecimal2BCD(start_month);
        value[3] = Util.ConvertDecimal2BCD(start_day);
        value[4] = Util.ConvertDecimal2BCD(start_hour);
        value[5] = Util.ConvertDecimal2BCD(start_minute);

        value[7] = Util.ConvertDecimal2BCD(end_year);
        value[8] = Util.ConvertDecimal2BCD(end_month);
        value[9] = Util.ConvertDecimal2BCD(end_day);
        value[0xA] = Util.ConvertDecimal2BCD(end_hour);
        value[0xB] = Util.ConvertDecimal2BCD(end_minute);
        value[19] = Crc(value);
        mDevice.writeTx(value);
    }


    DeviceDisconveredCallback scan_callback;

    private class DeviceInfo implements Serializable {


        int rssi;
        String name;
        String mac;
        BluetoothDevice device;

        DeviceInfo(int rssi, String name, String mac, BluetoothDevice device) {
            this.rssi = rssi;
            this.name = name;
            this.mac = mac;
            this.device = device;
        }

    }

    private static String decodeDeviceName(byte[] data) {
        String name = null;
        int fieldLength, fieldName;
        int packetLength = data.length;
        for (int index = 0; index < packetLength; index++) {
            fieldLength = data[index];
            if (fieldLength == 0)
                break;
            fieldName = data[++index];

            if (fieldName == COMPLETE_LOCAL_NAME || fieldName == SHORTENED_LOCAL_NAME) {
                name = decodeLocalName(data, index + 1, fieldLength - 1);
                break;
            }
            index += fieldLength - 1;
        }
        return name;
    }

    @Nullable
    private static String decodeLocalName(final byte[] data, final int start, final int length) {
        try {
            return new String(data, start, length, "UTF-8");
        } catch (final UnsupportedEncodingException e) {
            Log.e("scan", "Unable to convert the complete local name to UTF-8", e);
            return null;
        } catch (final IndexOutOfBoundsException e) {
            Log.e("scan", "Error when reading complete local name", e);
            return null;
        }
    }

    BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {

            String deviceName = device.getName();

            if (device.getName() != null && device.getName().contains(broadcastNameFilter)) {

                if (!map.containsKey(device.getAddress())) {

                    DeviceInfo deviceInfo = new DeviceInfo(rssi, decodeDeviceName(scanRecord), device.getAddress(), device);
                    map.put(deviceInfo.mac, deviceInfo.device);


                    Bundle bundle = new Bundle();
                    bundle.putSerializable("deviceInfo", deviceInfo);

                    MyRunnable runnable = new MyRunnable() {
                        @Override
                        public void Run(Bundle data) {

                            if (scan_callback != null) {

                                DeviceInfo info = (DeviceInfo) data.getSerializable("deviceInfo");
                                scan_callback.onNewDeviceDisconvered(info.name, info.mac, info.rssi);
                            }
                        }
                    };
                    Message msg = new Message();
                    msg.obj = runnable;
                    msg.setData(bundle);
                    handler.sendMessage(msg);
                }
            }
        }
    };
    static Map<String, BluetoothDevice> map = new HashMap<>();

    String name = null;
    String mac = null;

    Handler ota_handler = new Handler(Looper.getMainLooper()) {

        @Override
        public void handleMessage(Message msg) {

            sendZip(name, mac);

        }
    };
    boolean isScanning = false;

    private void stopLeScan(boolean immediately) {

        if (immediately) {

            if (isScanning) {

                this.adapter.stopLeScan(leScanCallback);
                isScanning = false;
            }
        } else {

            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {

                    if (isScanning) {
                        adapter.stopLeScan(leScanCallback);
                        isScanning = false;

                    }
                }
            }, DURATION_OF_SCANNING);
        }

    }

    String broadcastNameFilter = null;

    /**
     * 开启蓝牙设备搜索
     *
     * @param broadcastNameFilter 搜索设备时的名称过滤，只要包含其中字符就可以。
     * @param callback            搜索设备所返回的回调
     */
    public void scan(String broadcastNameFilter, DeviceDisconveredCallback callback) {
        this.broadcastNameFilter = broadcastNameFilter;
        scan_callback = callback;
        if (!isScanning) {

            map.clear();
            adapter.startLeScan(leScanCallback);
            stopLeScan(false);
            isScanning = true;

        } else {

            Log.e("Notify", "Still scaning!");
            //   stopLeScan(true);
            map.clear();
            Log.e("Notify", "" + map.size());
            //   isScanning = true;
            //   mBluetoothAdapter.startLeScan(leScanCallback);
            //   stopLeScan(false);

        }
    }

    /**
     * 停止蓝牙设备搜索
     */
    public void stopScan() {

        stopLeScan(true);

    }

    ResultNotifyCallback connect_callback;
    ResultNotifyCallback reconnect_callback;
    ReconnectingTimeOutCallback timeout_callback;

    /**
     * 连接设备
     *
     * @param mac       连接设备所需的MAC地址.
     * @param callback1 连接回调通知，当连接成功时将会接收到携带True为参数的回调，
     *                  当连接失败时就会接收到携带Flase为参数的回调.
     * @param callback2 设备重连回调通知.当重连成功将会接收到携带True为参数的回调,
     *                  当重连失败时将会接收到携带False为参数的回调.
     * @param callback3 当重连超时会被调用的回调
     * @param callback4 当意外断开连接的时候会收到的回调.
     */
    public void connect(String mac, ResultNotifyCallback callback1, ResultNotifyCallback callback2
            , ReconnectingTimeOutCallback callback3, ResultNotifyCallback callback4) {

        connect_callback = callback1;
        reconnect_callback = callback2;
        timeout_callback = callback3;
        disconnect_callback = callback4;


        if (isConnected) {
            if (connect_callback != null) {

                MyRunnable runnable = new MyRunnable() {
                    @Override
                    public void Run(Bundle data) {
                        connect_callback.onResultNotify(true);
                    }
                };
                Message msg = new Message();
                msg.obj = runnable;
                handler.handleMessage(msg);

            }

            return;
        }
        BluetoothDevice device = (BluetoothDevice) map.get(mac);

        if (device == null) {
            Log.e("Notify", "Device mac:" + mac + " not found");
            return;
        }
        registerBC();

        //!Is service closed?
        this.mDevice = new RFLampDevice(mContext, device);
    }

    boolean isManualDisconnected = false;
    ResultNotifyCallback disconnect_callback;
    ResultNotifyCallback manuallyDisconnect_callback;

    /**
     * 断开与设备的连接
     *
     * @param callback1 当手动连接断开后会被调用的回调
     */
    public void disconnect(ResultNotifyCallback callback1) {


        manuallyDisconnect_callback = callback1;
        mTimer.purge();
        if (!isConnected) {
            return;
        }

        if (mDevice != null) {
            isManualDisconnected = true;
            mDevice.disconnectedDevices();
            mDevice.disconnectedDevice();
        }


        //Delay the unregistration of broadcast to receive disconnected braodacast.
        mDevice = null;
    }

    DeviceOtaProgressCallback ota_callback = null;
    File ota_file = null;

    private boolean is_OTA_Disconnected = false;
    private boolean is_OTA_Disconnected2 = false;

    /**
     * OTA-Relatived Constants
     **/
    public static final int PROGRESS_DFU_PREPARE = -2;
    //public static final int PROGRESS_DFU_START = -1;
    public static final int PROGRESS_DFU_FAILD = -3;
    public static final int PROGRESS_READY = 0;
    public static final int PROGRESS_FINISHED = 101;
    public static final int PROGRESS_FAILED = 103;
    private static final String DFU_BC_NAME = "dfutarg";

    /**
     * OTA升级
     *
     * @param mPatchPath 升级包路径，升级包必须为一个Zip 文件，否则会失败
     * @param callback   设备固件更新进度反馈回调
     */
    public void OTA(String mPatchPath, DeviceOtaProgressCallback callback) {

     /*   if (!printLog()) {
            return;
        }*/
       /* if (!isConnected) {
            Log.e("Notify", "The device is not connected");
            return;
        }*/

        String mPatchFileType = mPatchPath.substring(mPatchPath.lastIndexOf("."));
        if (!mPatchFileType.toLowerCase().equals(".zip")) {
            Log.e("Notify", "The upgrade patch must be a zip file.");
        }


        File file = new File(mPatchPath);
        if (!file.exists()) {
            Log.e("Notify", "The upgrade patch :" + mPatchPath + " not found!");
        }

        ota_file = file;
        ota_callback = callback;

        if (isConnected) {
            is_OTA_Disconnected = true;
            is_OTA_Disconnected2 = true;
            byte[] values = new byte[20];
            values[0] = COMMAND_ID_OTA;
            values[19] = Crc(values);
            mDevice.writeTx(values);
            isStopScaned = false;
            adapter.startLeScan(ota_leScancallback);
            handler.postDelayed(ota_watchDog, 10000);

        } else {

            isStopScaned = false;
            adapter.startLeScan(ota_leScancallback);
            handler.postDelayed(ota_watchDog, 10000);

        }

        if (ota_callback != null) {
            ota_callback.OtaProgressCallback(PROGRESS_DFU_PREPARE);

        }

    }

    Runnable ota_watchDog = new

            Runnable() {
                @Override
                public void run() {
                    isAutoReconnect = false;
                    adapter.stopLeScan(ota_leScancallback);
                    ota_callback.OtaProgressCallback(PROGRESS_DFU_FAILD);

                }
            };

    private boolean isStateBCRegistered2 = false;

    private void register_OTA_BC() {

        IntentFilter filter = new IntentFilter();
        filter.addAction(DfuBaseService.BROADCAST_PROGRESS);
        filter.addAction(DfuBaseService.BROADCAST_ERROR);
        isStateBCRegistered2 = true;
        local_bcm.registerReceiver(ota_receive, filter);
    }

    private void unregister_OTA_BC() {

        if (isStateBCRegistered2) {
            MyLog.e("Unregister");
            isStateBCRegistered2 = false;
            local_bcm.unregisterReceiver(ota_receive);
        }
    }

    Intent intent = null;

    private void sendZip(String name, String address) {
        intent = new Intent(mContext, DddService.class);

        intent.putExtra(DfuBaseService.EXTRA_DEVICE_NAME, name);
        intent.putExtra(DfuBaseService.EXTRA_DEVICE_ADDRESS, address);
        intent.putExtra(DfuBaseService.EXTRA_FILE_MIME_TYPE, DfuBaseService.MIME_TYPE_ZIP);
        intent.putExtra(DfuBaseService.EXTRA_FILE_TYPE, DfuBaseService.TYPE_APPLICATION);
        intent.putExtra(DfuBaseService.EXTRA_FILE_PATH, ota_file.getAbsolutePath());
        intent.putExtra(DfuBaseService.EXTRA_FILE_URI, Uri.fromFile(ota_file));
        register_OTA_BC();
        mContext.startService(intent);
        if (ota_callback != null) {

            //ota_callback.OtaProgressCallback(PROGRESS_DFU_START);
        }


    }
}