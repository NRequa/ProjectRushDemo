package com.ee464k.demoapp;

import android.util.Log;

import androidx.annotation.NonNull;

import com.android.volley.toolbox.JsonObjectRequest;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SensorAverages {
    List<SensorStats> sensorstats; // Stats for each data point, ordered {spO2, ppg_hr, bodyTemp, ecg}
    List<NormalDistribution> dataDistributions; // For calculating fitness score
    int dataPoints;
    double fitnessScore;

    private String[] dataPtNames = {"spO2", "ppg_hr", "bodyTemp", "ecg"};
    private JSONObject globals;

    public SensorAverages(JSONObject globals){
        this.dataPoints = 4;
        this.sensorstats = new ArrayList<>();
        this.dataDistributions = new ArrayList<>();
        this.globals = globals;

        try{
            for(int i = 0; i < this.dataPoints; i++){
                sensorstats.add(new SensorStats());
                dataDistributions.add(new NormalDistribution(globals.getDouble(dataPtNames[i] + "_avg"), globals.getDouble(dataPtNames[i] + "_sd")));
            }
        } catch (JSONException e){
            Log.e("AVERAGES", "Failed to get global averages", e);
        }

    }
    // Keep running average/stdDev of each piece of info
    public void updateStats(SensorData data) throws JSONException{

        // Update all the averages + std. dev for new data point
        for(int i = 0; i < dataPoints; i ++){
            sensorstats.set(i, findNewStats(data.dataPtList.get(i), sensorstats.get(i)));
        }
        updateFitnessScore();

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

    private void updateFitnessScore() throws JSONException{
        double fitnessScore = 0;

        for(int i = 0; i < dataDistributions.size(); i++){
            double cdfPtAvg = Math.abs(sensorstats.get(i).average - globals.getDouble(dataPtNames[i] + "_avg")) * -1;
            double cdfPtSD = sensorstats.get(i).stdDev - globals.getDouble(dataPtNames[i] + "_sd");
            double cdfPt = cdfPtAvg / cdfPtSD;
            double ptProbability = dataDistributions.get(i).cumulativeProbability(cdfPt);
            fitnessScore = fitnessScore + (1 - ptProbability);
        }

        this.fitnessScore = fitnessScore;
        Log.d("FITNESS", "Fitness score: " + this.fitnessScore);

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
