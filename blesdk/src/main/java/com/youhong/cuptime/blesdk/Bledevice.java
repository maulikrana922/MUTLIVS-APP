package com.youhong.cuptime.blesdk;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

public abstract class Bledevice {
    Intent serviceIntent;
    protected static final byte[] CRCPASSWORD = {'C', 'h', 'e', 'c', 'k', 'A', 'e', 's'};
    protected Context context = null;
    public String deviceName = null, deviceMac = null;
    protected LightBLEService bleService = null;
    public BluetoothDevice device = null;
    // public RFStarBLEBroadcastReceiver delegate = null;
    public int bleDeviceType; //

    public Bledevice(Context context, BluetoothDevice device) {
        this.device = device;
        this.deviceName = this.device.getName();
        this.deviceMac = this.device.getAddress();
        this.context = context;
        this.registerReceiver();
        if (serviceIntent == null) {
            serviceIntent = new Intent(this.context, LightBLEService.class);
            this.context.bindService(serviceIntent, serviceConnection, Service.BIND_AUTO_CREATE);
        }
    }

    /**
     * �Ƿ�����
     *
     * @return
     */
    public boolean isConnected() {
        // return gatt.connect();
        return true;
    }

    /**
     * ���ӷ���
     */
    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // TODO Auto-generated method stub
            bleService = null;
            MyLog.e(" gatt is not init");
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            bleService = ((LightBLEService.LocalBinder) service).getService();
            bleService.initBluetoothDevice(device, context);
            // bleService.initBluetoothDevice(device);
            MyLog.i(" gatt is  init");
        }
    };

    public void reConnected() {
        bleService.initBluetoothDevice(device, context);
    }

    /**
     * ��ȡ����ֵ
     *
     * @param characteristic
     */
    public void readValue(BluetoothGattCharacteristic characteristic) {
        if (characteristic == null) {
            MyLog.e("readValue characteristic is null");
        } else {
            bleService.readValue(this.device, characteristic);
        }

    }

    /**
     * ��������ֵд������
     *
     * @param characteristic
     */
    public void writeValue(BluetoothGattCharacteristic characteristic) {
        if (characteristic == null) {
            Log.d("_SCANNING", "writeValue in Bledevice: characteristics is null");
            MyLog.e("writeValue characteristic is null");
        } else {
            if (bleService == null) {
                Log.d("_SCANNING", "writeValue in Bledevice: bleService is null");
                MyLog.e("writeValue bleService characteristic is null");
                return;
            }

            MyLog.e("writeValue 11111");
            bleService.writeValue(this.device, characteristic);
        }
    }

    /**
     * ��Ϣʹ��
     *
     * @param characteristic
     * @param enable
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enable) {
        if (characteristic == null) {
            MyLog.e("Notification characteristic is null");
        } else {
            MyLog.i("set Notification ");
            bleService.setCharacteristicNotification(this.device, characteristic, enable);
        }

    }

    /**
     * �Ͽ�����
     */
    public void disconnectedDevice() {
        this.ungisterReceiver();
        if (serviceConnection != null)
            Log.d("_SCANNING", "disconnectedDevice: DISCONNECTING, Bledevice");
            try{
                this.context.unbindService(serviceConnection);
            }catch (Exception e){
                Log.d("_SCANNING", "disconnectedDevice: unbindService: " + e.getMessage());
        }

    }

    public void disconnectedDevice(String address) {
        // try {
        // if(gattUpdateRecevice.)
        // activity.unregisterReceiver(gattUpdateRecevice);

        this.bleService.disconnect(address);
        // this.context.unbindService(serviceConnection);
        this.device = null;
        // bleService = null;
        // } catch (Exception e) {
        // System.out.println(e.getStackTrace());
        // }

    }

    public void terminateGatt(){
        try{
            this.bleService.hardGattDisconnect();
        }catch (Exception e){
            Log.d("_SCANNING", "terminateGatt: ERROR: " + e.getMessage());
        }
    }

    public void disconnectedDevices() {
        try {
            this.bleService.disconnect();
        } catch (Exception e) {
            Log.d("_SCANNING", "disconnectedDevices: uncaught exception: " + e.getMessage());
            // TODO: handle exception
        }

    }

    public void closeDevice() {
        this.ungisterReceiver();
        Log.d("_SCANNING", "closeDevice: UNBINDING FROM SERVICE, Bledevice");
        this.context.unbindService(serviceConnection);
    }
    /**
     * ��ȡ����
     *
     * @return
     */
    // public List<BluetoothGattService> getBLEGattServices() {
    // return this.bleService.getSupportedGattServices(this.device);
    // }

    /**
     * ���ӹ㲥������
     *
     * @return
     */
    protected IntentFilter bleIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(LightBLEService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(LightBLEService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(LightBLEService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(LightBLEService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(LightBLEService.ACTION_GAT_RSSI);
        intentFilter.addAction(LightBLEService.ACTION_GATT_CONNECTING);
        // �Զ�������
        intentFilter.addAction("com.imagic.connected");
        return intentFilter;
    }

    /**
     * ע����������豸���������ݵģ��㲥
     *

     */
    public void registerReceiver() {
        ContextWrapper activity = (ContextWrapper) this.context;
        if (!isRegisteed) {
            activity.registerReceiver(gattUpdateReceiver, this.bleIntentFilter());
            isRegisteed = true;
        }
    }

    /**
     * ע�������������صĹ㲥
     */

    static boolean isRegisteed = false;

    public void ungisterReceiver() {


        ContextWrapper activity = (ContextWrapper) this.context;
        if (isRegisteed) {
            try {
                isRegisteed = false;
                activity.unregisterReceiver(gattUpdateReceiver);
                bleService.disconnect();
                bleService = null;

            } catch (IllegalArgumentException exp) {


            }

        }

    }

    /**
     * ��ʼ�������е�����
     */
    protected abstract void discoverCharacteristicsFromService();

    /**
     * ���������㲥
     */
    private BroadcastReceiver gattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, Intent intent) {
            if (LightBLEService.ACTION_GATT_SERVICES_DISCOVERED.equals(intent.getAction())) {
                try {
                    discoverCharacteristicsFromService();
                } catch (Exception e) {
                    // TODO: handle exception
                }
            }
        }
    };

}
