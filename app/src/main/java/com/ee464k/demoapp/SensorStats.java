package com.ee464k.demoapp;

public class SensorStats {
    public double average;
    public double stdDev;
    public double variance;
    public int totalEntries;

    public SensorStats(){
        this.average = 0;
        this.stdDev = 0;
        this.variance = 0;
        this.totalEntries = 0;
    }

}
