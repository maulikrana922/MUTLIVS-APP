package com.youhong.cuptime.blesdk;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Arrays;
import java.util.List;

public class RFLampDevice extends Bledevice {

    //This action would be broadcasted after RX and TX characteristic object is got.
    public static final String ACTION_SERVICES_DISCOVERED = "com.youhong.tempgun.tempgun.service.ACTION_SERVICES_DISCOVERED";
    private static final String TAG = "_SCANNING";

    private BluetoothGattCharacteristic RXCharateristic;
    private BluetoothGattCharacteristic TXCharateristic;

    public RFLampDevice(Context context, BluetoothDevice device) {
        // TODO Auto-generated constructor stub
        super(context, device);
        this.device = device;
    }

    @Override
    protected void discoverCharacteristicsFromService() {
        // ���޸�
        if (bleService == null || device == null) {

            return;
        }
        List<BluetoothGattService> services = bleService
                .getSupportedGattServices(this.device);
        if (services == null) {
            return;
        }

        boolean a = false, b = false;
        for (BluetoothGattService service : services) {
            for (BluetoothGattCharacteristic characteristic : service
                    .getCharacteristics()) {

                if (service.getUuid().
                        toString().toUpperCase().contains("FFF0")) {
                    Log.d(TAG, "discoverCharacteristicsFromService: FFF0)");
                    if (characteristic.getUuid().toString().toUpperCase().contains("FFF6")) {
                        Log.d(TAG, "discoverCharacteristicsFromService: FFF6)");
                        TXCharateristic = characteristic;
                        a = true;


                    } else if (characteristic.getUuid().toString().toUpperCase().contains("FFF7")) {
                        Log.d(TAG, "discoverCharacteristicsFromService: FFF7)");
                        RXCharateristic = characteristic;
                        this.setCharacteristicNotification(characteristic, true);
                        //Here should sendBroadcast
                        b = true;
                    }

                    if (a & b) {

                        context.sendBroadcast(new Intent(RFLampDevice.ACTION_SERVICES_DISCOVERED));
                        return;

                    }
                }

            }

        }
    }

    
    public void writeTx(byte[] values) {

        if (TXCharateristic == null) {
            System.out.println("TXCharateristic is null");
            return;
        }
        Log.d(TAG, "writeTx: writing in RFLampDevice: " + Arrays.toString(values));
        TXCharateristic.setValue(values);
        this.writeValue(TXCharateristic);
    }
}
