package com.ee464k.demoapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import java.util.Set;
import java.util.UUID;

public class BluetoothHandler {
    public String address;
    public UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    String TAG = "BLHandler";
    public BluetoothDevice hardwareBT = null;
    public Set<BluetoothDevice> pairedDevices;

    public BluetoothHandler(String address, Set<BluetoothDevice> pairedDevices){
        this.address = address;
        this.pairedDevices = pairedDevices;
    }

    public BluetoothDevice getDevice(){
        BluetoothDevice desiredDevice = null;

        for(BluetoothDevice device : pairedDevices){
            Log.d(TAG, device.getName() + " : " + device.getAddress());
            if(device.getAddress().equals(address)){
                desiredDevice = device;
            }
        }

        return desiredDevice;
    }


}
