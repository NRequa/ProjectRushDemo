package com.ee464k.demoapp;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

public class SensorData {
    public int timestamp;
    public double spo2;
    public double ppg_hr;
    public double bodytemp;
    public int ecg;

    public SensorData(String message){
        // Convert string to JSON Object
        try {
            String cleanMsg = trimMessage(message);
            JSONObject jObject = new JSONObject(cleanMsg);
            // Get seperate values from key/labels and put them into integers
            this.timestamp = jObject.getInt("TimeStamp");
            this.spo2 = jObject.getDouble("Sp02");
            this.ppg_hr = jObject.getDouble("PPG_HR");
            this.bodytemp = jObject.getDouble("BodyTemperature");
            this.ecg = jObject.getInt("ECG");
        } catch(JSONException e){
            Log.e("SENSORDATA", "Couldn't parse JSON object", e);
        }
    }


    public String trimMessage(String message){
        int startPt = 0;
        int endPt = 0;
        // Occasionally get multiple JSON strings, just takes first one.
        try {
            startPt = message.indexOf('{');
            String midString = message.substring(startPt);
            endPt = midString.indexOf('}');
            return midString.substring(0, endPt + 1);
        } catch (IndexOutOfBoundsException e){
            // Something went very wrong, just set to 0 for this pt
            return "{TimeStamp: 0, Sp02: 0, PPG_HR: 0, BodyTemperature: 0, ECG: 0}";
        }
    }


    @Override
    public String toString(){
        if(this == null){
            return "NULL";
        }
        return "{" + timestamp + ", " + spo2 + ", " + ppg_hr + ", " + bodytemp + ", " + ecg + "}";
    }
}
