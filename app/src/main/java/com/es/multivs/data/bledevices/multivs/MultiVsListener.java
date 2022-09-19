package com.es.multivs.data.bledevices.multivs;


public interface MultiVsListener {

    void onPatchDataReceived(PatchData patchData);

    void onWearingStatus(boolean isWornCorrectly);

    void onBodyPositionChanged(int bodyPosition);

    void onTemperatureReceived(float value);

    void onHeartRateReceived(int heartRate);

    void onStepsReceived(int steps);


    void onBadData();

    void onConnected(boolean isConnected);

    void onBatteryLevelReceived(int batteryLevel);
}
