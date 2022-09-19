package com.es.multivs.data.network.netmodels;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class UserDevices {

    private String ownMacAddress;

    @SerializedName("mac_id")
    private String ownMacID;

    @SerializedName("status")
    private String status;

    @SerializedName("message")
    private String message;

    @SerializedName("list")
    private List<DeviceInfo> deviceInfoList;

    public UserDevices(String status, String message, List<DeviceInfo> deviceInfoList) {

        if (status != null) {
            this.status = status;
        }

        if (message != null) {
            this.message = message;
        }

        if (deviceInfoList != null || deviceInfoList.size() != 0) {
            this.deviceInfoList = deviceInfoList;
        }

//        this.ownMacAddress = GatewayData.getInstance().getMacAddress();
    }

    public String getOwnMacID() {
        return ownMacID;
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public List<DeviceInfo> getDeviceInfoList() {
        return deviceInfoList;
    }

    @NonNull
    @Override
    public String toString() {

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("User Devices: ");

        for (DeviceInfo deviceInfo : deviceInfoList) {
            stringBuilder.append("\n" + "device ID: ").append(deviceInfo.deviceID).append(" | device mac address: ").append(deviceInfo.macAddress);
        }

        return stringBuilder.toString();
    }

    public static class DeviceInfo {

        @SerializedName("device_id")
        private String deviceID;

        @SerializedName("device_name")
        private String deviceName;

        @SerializedName("device_type")
        private String deviceType;

        @SerializedName("mac_address")
        private String macAddress;

        @SerializedName("sensors")
        private String sensors;

        @SerializedName("firmware_version")
        private String firmwareVersion;

        @SerializedName("is_active")
        private int isActive;

        @SerializedName("in_use")
        private int inUse;

        public String getDeviceID(){
            return this.deviceID;
        }

        public String getDeviceMAC(){
            return this.macAddress;
        }
    }
}
