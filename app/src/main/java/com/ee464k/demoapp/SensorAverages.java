package com.ee464k.demoapp;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class SensorAverages {
    List<SensorStats> sensorstats; // Stats for each data point, ordered {spO2, ppg_hr, bodyTemp, ecg}
    int dataPoints;

    public SensorAverages(){
        this.dataPoints = 4;
        this.sensorstats = new ArrayList<SensorStats>();
        for(int i = 0; i < this.dataPoints; i++){
            sensorstats.add(new SensorStats());
        }
    }
    // Keep running average/stdDev of each piece of info
    public void updateStats(SensorData data){
        for(int i = 0; i < dataPoints; i ++){
            sensorstats.set(i, findNewStats(data.dataPtList.get(i), sensorstats.get(i)));
        }

    }

    private SensorStats findNewStats(Double newEntry, SensorStats oldStats){
        double prev_variance = oldStats.variance;
        double prev_mean = oldStats.average;
        oldStats.totalEntries += 1;

        oldStats.average = prev_mean + (newEntry - prev_mean) / oldStats.totalEntries;
        oldStats.variance = prev_variance + (newEntry - oldStats.average) * (newEntry - prev_mean);
        oldStats.stdDev = Math.sqrt(oldStats.variance / oldStats.totalEntries);

        return oldStats;
    }


    @Override
    public String toString() {
        String[] sensorName = {"\"spO2", "\"ppg_hr", "\"bodyTemp", "\"ecg"};
        StringBuilder finalString = new StringBuilder();
        finalString.append("{");
        for(int i = 0; i < dataPoints; i++){
            String holder = sensorName[i] + "_avg\": " + sensorstats.get(i).average + ", ";
            finalString.append(holder);
            if(i != dataPoints - 1){
                finalString.append(sensorName[i] + "_sd\": " + sensorstats.get(i).stdDev + ", ");
            }
            else{
                finalString.append(sensorName[i] + "_sd\": " + sensorstats.get(i).stdDev);
            }
        }
        finalString.append("}");
        return finalString.toString();
    }
}
