package com.youhong.cuptime.blesdk;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import android.util.Log;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Administrator on 2017/5/18.
 */

public abstract class BaseBleOperation {

    private static final int SHORTENED_LOCAL_NAME = 0x08;
    private static final int COMPLETE_LOCAL_NAME = 0x09;
    private static final int DURATION_OF_SCANNING = 15000;

    public static final int PROGRESS_FINISHED = 101;
    private static final String TAG = "_SCANNING";
    protected Context mContext;
    protected RFLampDevice mDevice;
    protected Handler handler;

    private boolean isConnected = false;
    private boolean isStateBCRegistered = false;
    private boolean isFirstTimeConnected = true;
    private boolean isAutoReconnect = false;
    private boolean is_OTA_Disconnected = false;
    private boolean is_OTA_Disconnected2 = false;
    private boolean isStopScaned = true;
    private boolean isAlreadyInTimer = false;

    private long reconnecting_timeOut_value = 60 * 10;  //10mins
    private long reconnecting_timeOut_remains = reconnecting_timeOut_value;


    protected BluetoothAdapter mBluetoothAdapter;

    protected LocalBroadcastManager local_bcm = null;
    private Timer mTimer;

    protected static Map<String, BluetoothDevice> map = new HashMap<>();
    private TimerTask mTimerTask = null;

    private BleOperation.DeviceOtaProgressCallback ota_callback = null;

    public BaseBleOperation(Context context) {

        this.mContext = context;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
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

    private BroadcastReceiver state_receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction() == LightBLEService.ACTION_GATT_CONNECTED) {
                Log.d("_SCANNING", "onReceive: LightBLEService RECEIVED CONNECTED");

                isConnected = true;

                if (isFirstTimeConnected) {

                    isFirstTimeConnected = false;

                }

            } else if (intent.getAction() == LightBLEService.ACTION_GATT_DISCONNECTED) {
                Log.d("_SCANNING", "onReceive: LightBLEService state_receiver RECEIVED DICONNECTED");

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
                    Log.d("_SCANNING", "onReceive: OTA TRYING TO UNBIND");

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

                            if (mDevice!=null){
                                scanForDevice(mDevice.device, inner_callback);
                            }else{
                                Log.d(TAG, "onReceive: cannot scanForDevice, mDevice is null!!");
                            }
                        }
                    }
                }


            } else if (intent.getAction() == RFLampDevice.ACTION_SERVICES_DISCOVERED) {
                Log.d("_SCANNING", "onReceive: LightBLEService RECEIVED SERVICE DISCOVERED");

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
                                reconnect_callback.onResultNotify(false);
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

            } else if (intent.getAction() == LightBLEService.ACTION_DATA_AVAILABLE) {

                Action_RX_data(context, intent);

            }
        }
    };

    BleOperation.ResultNotifyCallback inner_callback = new BleOperation.ResultNotifyCallback() {
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

    protected abstract void Action_RX_data(Context context, Intent intent);

    protected void writeTx(byte[] values) {

        if (mDevice == null) {
            MyLog.e("mDevice == null in BaseBleOperation.writeTx()");
            Log.d(TAG, "writeTx: bluetooth device is null");
            return;
        }
        Log.d(TAG, "writeTx: writing from BaseBleOperation: " + Arrays.toString(values));
        mDevice.writeTx(values);
    }

    protected void writeCheck(byte[] values) {

        if (mDevice == null) {
            MyLog.e("mDevice == null in BaseBleOperation.writeTx()");
            return;
        }
        //   mDevice.writeCheck(values);

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

    private void scanForDevice(final BluetoothDevice dev, final BleOperation.ResultNotifyCallback callback) {
        Log.d(TAG, "scanForDevice: BASEBLE IS SCANNING FOR " + dev);
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
                                        mBluetoothAdapter.stopLeScan(this);
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
                                    mBluetoothAdapter.stopLeScan(ble_callback);
                                    isStopScaned = true;
                                    //callback.onResultNotify(false);
                                }
                            }
                        }, 5000);
                        BaseBleOperation.this.mBluetoothAdapter.startLeScan(ble_callback);
                    }
                }, 0, 25000);

        isAlreadyInTimer = true;
        // final boolean isScanningStop = false;
    }


    private boolean isManualDisconnected = false;
    private ResultNotifyCallback disconnect_callback;
    private ResultNotifyCallback manuallyDisconnect_callback;

    public void hardDisconnect() {
        mTimer.purge();
        if (mDevice != null) {
//            isManualDisconnected = true;
            mDevice.terminateGatt();
            mDevice.disconnectedDevice();
        }

        mDevice = null;
    }

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

//        handler.postDelayed(() -> mDevice = null,2000);
        //Delay the unregistration of broadcast to receive disconnected braodacast.
        mDevice = null;
    }


    private ResultNotifyCallback connect_callback;
    private ResultNotifyCallback reconnect_callback;
    private ReconnectingTimeOutCallback timeout_callback;


//    /**
//     * 连接设备
//     *
//     * @param mac       连接设备所需的MAC地址.
//     * @param callback1 连接回调通知，当连接成功时将会接收到携带True为参数的回调，
//     *                  当连接失败时就会接收到携带Flase为参数的回调.
//     * @param callback2 设备重连回调通知.当重连成功将会接收到携带True为参数的回调,
//     *                  当重连失败时将会接收到携带False为参数的回调.
//     * @param callback3 当重连超时会被调用的回调
//     * @param callback4 当意外断开连接的时候会收到的回调.
//     */
//    public void connect(String mac, ResultNotifyCallback callback1, ResultNotifyCallback callback2
//            , ReconnectingTimeOutCallback callback3, ResultNotifyCallback callback4) {
//
//        connect_callback = callback1;
//        reconnect_callback = callback2;
//        timeout_callback = callback3;
//        disconnect_callback = callback4;
//
//        if (isConnected) {
//            if (connect_callback != null) {
//
//                MyRunnable runnable = new MyRunnable() {
//                    @Override
//                    public void Run(Bundle data) {
//                        connect_callback.onResultNotify(true);
//                    }
//                };
//                Message msg = new Message();
//                msg.obj = runnable;
//                handler.handleMessage(msg);
//
//            }
//            return;
//        }
//        BluetoothDevice device = (BluetoothDevice) map.get(mac);
//
//        if (device == null) {
//            Log.e("Notify", "Device mac:" + mac + " not found");
//            return;
//        }
//        registerBC();
//        //!Is service closed?
//        this.mDevice = new RFLampDevice(mContext, device);
//    }

    /**
     * 连接设备
     *
     * @param device    .
     * @param callback1 连接回调通知，当连接成功时将会接收到携带True为参数的回调，
     *                  当连接失败时就会接收到携带Flase为参数的回调.
     * @param callback2 设备重连回调通知.当重连成功将会接收到携带True为参数的回调,
     *                  当重连失败时将会接收到携带False为参数的回调.
     * @param callback3 当重连超时会被调用的回调
     * @param callback4 当意外断开连接的时候会收到的回调.
     */
    public void connect(BluetoothDevice device, ResultNotifyCallback callback1, ResultNotifyCallback callback2
            , ReconnectingTimeOutCallback callback3, ResultNotifyCallback callback4) {

        connect_callback = callback1;
        reconnect_callback = callback2;
        timeout_callback = callback3;
        disconnect_callback = callback4;

        if (isConnected) {
            if (connect_callback != null) {
                Log.d("_SCANNING", "connect: device seems to be already connected");
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
        Log.d("_SCANNING", "connect: device seems to be not connected");


        if (device == null) {
            Log.e("Notify", "Device mac:" + device.getAddress() + " not found");
            return;
        }
        registerBC();
        //!Is service closed?
        this.mDevice = new RFLampDevice(mContext, device);
    }


    protected void stopLeScan(boolean immediately, final BluetoothAdapter.LeScanCallback callback) {

        if (immediately) {

            if (isScanning) {

                this.mBluetoothAdapter.stopLeScan(callback);
                isScanning = false;
            }
        } else {

            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {

                    if (isScanning) {
                        mBluetoothAdapter.stopLeScan(callback);
                        isScanning = false;

                    }
                }
            }, DURATION_OF_SCANNING);
        }

    }

    private String broadcastNameFilter = null;

    private DeviceDisconveredCallback scan_callback;

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

    protected boolean isScanning = false;

    public void stopScan() {

        stopLeScan(true, leScanCallback);
        isScanning = false;
    }

    private BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {

            String deviceName = device.getName();
            String mac = device.getAddress();
//            if (device.getName() != null && (broadcastNameFilter.length() == 0 || device.getName().contains(broadcastNameFilter))) {
            if (mac.equals("DF:75:BD:D9:DC:5C")) {
                stopScan();
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
            mBluetoothAdapter.startLeScan(leScanCallback);
            stopLeScan(false, leScanCallback);
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

    //Normal setting callback
    public interface ResultNotifyCallback {

        public void onResultNotify(boolean result);

    }

    public interface ReconnectingTimeOutCallback {

        public void ReconnectingTimeOutNotify();

    }

    public interface DeviceDisconveredCallback {
        public void onNewDeviceDisconvered(String deviceName, String mac, int rssi);
    }
}
