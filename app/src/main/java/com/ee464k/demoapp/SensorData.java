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
    JSONObject dataJSON;
    String dataString;
    boolean goodMsg = true;

    public SensorData(String message){
        goodMsg = true;
        // Convert string to JSON Object
        try {
            String cleanMsg = trimMessage(message);
            JSONObject dataJson = new JSONObject(cleanMsg);
            dataString = dataJson.toString();
            // Get seperate values from key/labels and put them into integers
            this.timestamp = dataJson.getInt("timestamp");
            this.spo2 = dataJson.getDouble("spO2");
            this.ppg_hr = dataJson.getDouble("ppg_hr");
            this.bodytemp = dataJson.getDouble("bodyTemp");
            this.ecg = dataJson.getInt("ecg");
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
            this.goodMsg = false;
            return null;
        }
    }


    @Override
    public String toString(){
        if(this == null){
            return "NULL";
        }
        return "{timestamp: " + timestamp + ", spO2: " + spo2 + ", ppg_hr: " + ppg_hr + ", bodyTemp: " + bodytemp + ", ecg: " + ecg + "}";
    }
}
