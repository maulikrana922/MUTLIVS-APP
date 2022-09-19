package com.youhong.cuptime.blesdk;

import java.util.Calendar;

/**
 * Created by Administrator on 2019/10/14.
 */

public class J1767BleCallbacks {

    public interface SetTimeCallback {
        void onCallback();
    }

    public interface GetTimeCallback {
        void onCallback(Calendar c);
    }

    public interface SetUserInfoCallback {
        void onCallback();
    }

    public interface GetUserInfoCallback {
        /**
         *
         * @param gender gender
         * @param age age
         * @param height height  unit is 1CM
         * @param weight weight unit is 1KG
         * @param strideLength Stride length
         * @param id1 id1
         * @param id2 id2
         * @param id3 id3
         * @param id4 id4
         * @param id5 id5
         * @param id6 id6
         */
        void onCallback(int gender, int age, int height, int weight, int strideLength,
                        byte id1, byte id2, byte id3, byte id4, byte id5, byte id6);
    }

    public interface EnableRTStepCallback {
        void onCallback();

        /**
         * @param step step
         * @param calories  Calories,(Unit is 0.01KCAL)
         * @param distance Distance,(Unit is 0.01KM)
         * @param activityTime activity time
         */
        void onDataCallback(int step, int calories, int distance, int activityTime);
    }

    public interface DisableRTStepCallback {
        void onCallback();
    }

    public interface SetDeviceIdCallback {
        void onCallback();
    }

    public interface GetBatteryLevelCallback {

        /**
         * get device battery
         * @param batteryLevel the level of battery
         * @param batteryVoltage voltage
         */
        void onCallback(int batteryLevel, int batteryVoltage);

    }

    public interface GetDeviceMacCallback {
        /**
         *  Get device's MAC address
         * @param mac1 mac1
         * @param mac2 mac2
         * @param mac3 mac3
         * @param mac4 mac4
         * @param mac5 mac5
         * @param mac6 mac6
         */
        void onCallback(byte mac1, byte mac2, byte mac3, byte mac4, byte mac5, byte mac6);
    }

    public interface LowBatteryReportCallback {

        /**
         * @param batteryLevel
         * @param chargeStatus ChargeStaus, 1:Charger is disconnected，2:In charge，3:battery is full.
         */
        void onCallback(int batteryLevel, int chargeStatus);

    }

    public interface EnableHrReportWhileEcgTestCallback {

        void onCallback();

        /**
         *
         * @param hr heart rate value
         */
        void onDataCallback(int hr);
    }

    public interface EnableEcgAndPPGCallback {

        /**
         * @param status 1 means that it is already in a real-time measurement,
                            2 indicates that the battery is too low to start this test.
                            3 indicates that it is not currently worn correctly.
                            4 indicates that it is currently charging.
                            Otherwise the AA in the reply is meaningless.
         */

        void onCallback(int status);

        /**
         * @param data when command mainToggle=0,and EcgToggle is not 0,the above reply ECG and PPG original data has meaning
            When command mainToggle=1,and EcgToggle is not 0,ECG has meaning ,PPG has no meaning
            When command mainToggle=2,and EcgToggle is not 0,PPG has meaning,ECG has no meaning ,pls ignore
             */
        void onDataCallback(byte[] data);
    }

    public interface SetAutomaticMeasurementCallback {
        void onCallback();

    }

    public interface GetAutomaticMeasurementCallback {

        /**
         *
         * @param minutesInterval is measure interval (The minimum setting is 30 mins,the max setting is 24 hours,Default one hour)
         * @param ecgToggle  the ECG period measurement is on, it is the default. ecgToggle == false means the ECG period measurement is off.
         * @param ppgToggle PPG period measurement is on ,it is the default,ppgToggle == false means PPG period measurement is off
         */

        void onCallback(int minutesInterval, boolean ecgToggle, boolean ppgToggle);
    }

    public interface GetAutomaticMeasurementHistoryDataSectionHeaderCallback {
        /**
         * @param sectionId data number(Each storage location corresponds to a unique data number)
         * @param c the date in device when starting measuring
         * @param voltagePercent  voltage level
         * @param measurementType
         * @param ntcTemperature Temperature value measured by NTC
         * @param pcbTemperature pcb temperature degree
         * @param bodyPosition
         * @param dataLength
         */
        void onCallback(int sectionId, Calendar c, int voltagePercent, int measurementType,
                        float ntcTemperature, float pcbTemperature, int bodyPosition
                , long dataLength
        );

    }

    public interface GetAutomaticMeasurementHistorySectionDataCallback {

        /**
         *
         * @param id @id  message number,It is used to determine whether there is any loss of packets when subsequent packets are continuously sent.(The value ranges from 0 to 255. Each time the message is sent, the value of P0 is automatically incremented by 1
         * @param recordCount How many ECG or PPG records are included in this message?
         * @param records ECG PPG records max Value = (recordCount - 1)
         */
        void onCallback(int id, int recordCount, byte[] records);
    }

    public interface DeleteAutomaticMeasurementHistoryDataCallback {

        void onCallback();
    }


    public interface CheckBodyPositionCallback {

        /**
         *
         * @param isCorrect
         * @isCorrect =stand for wear correctly,stand for not wear properly
         *
         */
        void onCallback(boolean isCorrect);
    }

    public interface GetBodyPositionCallback {
        /**
         * @param bodyPosition
                                bodyPosition=1  the current status is lay down
                                bodyPosition=2 the current status is upright
                                bodyPosition=3 the current status is moving(moving more than 10 steps is considered to be moving)
                                bodyPosition=4 the current status is left side lying
                                bodyPosition=5, the current status is right side lying
                                bodyPosition=6 the current status is lie prostrate
                                bodyPosition=7 the current status is unknown
         */
        void onCallback(int bodyPosition);

    }

    public interface CheckBodyIsTouchedCallback {
        /**
         *
         * @param isCorrectTouch True stand for wear correctly,False stand for not wear properly

         */
        void onCallback(boolean isCorrectTouch);
    }

    public interface BodyTouchReportCallback {

        /**
         *
         * @param isCorrectTouch True,means connect not well
                                 False means connect well
         * @param c present the datetime of touching
         */
        void onCallback(boolean isCorrectTouch, Calendar c);
    }

    public interface EnableAutomaticEcgAndPpgMeasurementFinishReportCallback {
        /**
         * This instruction does not require a gateway request, and is sent by the ECG patch to the gateway. After the ECG/PPG detection of each timing measurement is completed, the command will be sent to the gateway actively. After receiving the command, the gateway should immediately request the historical data of the ECG/PPG raw data generated by the synchronized automatic measurement.
         */
        void onCallback();
    }


    public interface GetTotalSportDataOnSpecificDayCallback {
        /**
         *
         * Get total daily activity information
         * @param dayIndex serial number,The total data of the last X days. 0 means the same day, it is real-time data. The range is 0 to 29. A total of 30 days of data
         * @param c data Date
         * @param step steps
         * @param distance distance unit is 0.01KCAL
         * @param calories calories unit is 0.01KM
         * @param ReachGoalPercent goal ,unit is 1%
         */
        void onCallback(int dayIndex, Calendar c, long step, long LengthOfExercise, long distance, long calories, int ReachGoalPercent);
    }

    public interface GetDetailSportDataOnSpecificDayCallback {

        /**
         * Get Detail Steps
         * @param dataIndex dataIndex
         * @param c data Date
         * @param step data step
         * @param distance distance unit is 0.01KCAL
         * @param calories calories unit is 0.01KM
         * @param tenMinutesStep  represent the steps per minute within 10 minutes
         */
        void onCallback(int dataIndex, Calendar c, int step, int distance, int calories, byte[] tenMinutesStep);
    }

    public interface GetVersionCallback {

        /**
         * Get Version
         * @param version Version number
         * @param calendar Version Date
         */
        void onCallback(String version, Calendar calendar);

    }

    public interface EnableRtTemperatureMessurementCallback {

        void onCallback();

        /**
         *
         * @param ntcTemperature the temperature collected by the NTC sensor
         * @param pcbTemperature pcb temperature degree
         */
        void onDataCallback(float ntcTemperature, float pcbTemperature);
    }

}
