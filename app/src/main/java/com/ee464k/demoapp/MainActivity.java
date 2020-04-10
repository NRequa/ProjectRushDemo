package com.ee464k.demoapp;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.hardware.Sensor;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;

public class MainActivity extends AppCompatActivity {

    private LineGraphSeries<DataPoint> spo2_series, ppg_series, bodytemp_series, ecg_series;
    private int lastX = 0;
    private double generatedIndex = 0;
    private final String TAG = "MainActivity";
    private final String address = "AC:EE:9E:63:FB:8B"; // MAC address for my other android device, replace with HC-05 address in final version


    public UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private final int DATA_IN = 1;
    private final int UPLOAD_CHANGE = 2;
    public static Handler bluetoothRead;
    private Runnable updateEntries;
    int postNum;
    public LinkedBlockingQueue<SensorData> sensorDataBuffer;
    public LinkedBlockingQueue<SensorData> sensorDataDisplay;
    private SensorData submitHolder;


    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sensorDataBuffer = new LinkedBlockingQueue<SensorData>();
        sensorDataDisplay = new LinkedBlockingQueue<SensorData>();

        // Create handler to update UI and store data
        bluetoothRead = new Handler() {
            public void handleMessage(Message msg){
                if(msg.what == DATA_IN){
                        SensorData sensorData = (SensorData) msg.obj;
                        addEntries(sensorData);
                }
                else if(msg.what == UPLOAD_CHANGE){
                    SensorData newUpload = (SensorData) msg.obj;
                    if(msg.obj != null) {
                        updateToUpload(newUpload);
                    }
                }
            }
        };

        // Set up bluetooth stuff
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null){
            Log.e(TAG, "Bluetooth not supported");
        }

        if(!mBluetoothAdapter.isEnabled()){
            Intent enableBTintent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBTintent, 1);
        }

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        // Get device we want
        BluetoothHandler BTsetup = new BluetoothHandler(address, pairedDevices);
        BluetoothDevice device = BTsetup.getDevice();
        Log.d("BluetoothConnect", "Device connected is : " + device.getName());


        // Get all the graph views
        GraphView spo2_graph = (GraphView) findViewById(R.id.spo2);
        GraphView ppg_graph = (GraphView) findViewById(R.id.ppg_hr);
        GraphView bodytemp_graph = (GraphView) findViewById(R.id.bodytemp);
        GraphView ecg_graph = (GraphView) findViewById(R.id.ecg);

        // Set up each graph w/ it's series
        spo2_graph.setTitle("SPO2");
        spo2_series = new LineGraphSeries<>();
        spo2_graph.addSeries(spo2_series);

        ppg_graph.setTitle("PPG");
        ppg_series = new LineGraphSeries<>();
        ppg_graph.addSeries(ppg_series);

        bodytemp_graph.setTitle("Body Temp");
        bodytemp_series = new LineGraphSeries<>();
        bodytemp_graph.addSeries(bodytemp_series);

        ecg_graph.setTitle("ECG");
        ecg_series = new LineGraphSeries<>();
        ecg_graph.addSeries(ecg_series);

        // ViewPort customization
        Viewport spo2viewport = spo2_graph.getViewport();
        spo2viewport.setXAxisBoundsManual(true);
        spo2viewport.setMinX(0);
        spo2viewport.setMaxX(10);
        spo2viewport.setScrollable(true);

        Viewport ppgviewport = ppg_graph.getViewport();
        ppgviewport.setXAxisBoundsManual(true);
        ppgviewport.setMinX(0);
        ppgviewport.setMaxX(10);
        ppgviewport.setScrollable(true);

        Viewport bodytempviewport = bodytemp_graph.getViewport();
        bodytempviewport.setXAxisBoundsManual(true);
        bodytempviewport.setMinX(0);
        bodytempviewport.setMaxX(10);
        bodytempviewport.setScrollable(true);

        Viewport ecgviewport = ecg_graph.getViewport();
        ecgviewport.setXAxisBoundsManual(true);
        ecgviewport.setMinX(0);
        ecgviewport.setMaxX(10);
        ecgviewport.setScrollable(true);

        // Set index
        final TextView indexSubmit = (TextView) findViewById(R.id.indexDisplay);
        indexSubmit.setText(Double.toString(generatedIndex));

        // Start BT connection
        ConnectThread connect = new ConnectThread(device);
        connect.start();
    }

    @Override
    protected  void onResume(){
        super.onResume();

    }

    public void updateToUpload(SensorData data){
        TextView uploadDisplay = (TextView) findViewById(R.id.averageDisplay);
        uploadDisplay.setText(data.toString());

    }
    public void getAverages(View view) {
        final TextView avgDisplay = (TextView) findViewById(R.id.averageDisplay);

        // From https://developer.android.com/training/volley/simple
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "http://projrush-env.takuddxgcj.us-east-2.elasticbeanstalk.com/Average";

        // Request builder
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        avgDisplay.setText("Response: " + response);
                        //displayAverage(response);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                avgDisplay.setText(error.toString());
            }

        });

        queue.add(stringRequest);
    }

    public void displayAverage(String result){
        // JSON with all our data to average
        final TextView avgDisplay = (TextView) findViewById(R.id.averageView);
        try {
            JSONArray dataList = new JSONArray(result);
             double sum = 0;
            for(int i = 0; i < dataList.length(); i++){
                JSONObject dataPt = dataList.getJSONObject(i);
                double indexVal = dataPt.getDouble("index");
                sum += indexVal;
            }

            double avg = Math.round(sum / dataList.length());
            Log.d("DISPLAY", "displayAverage: " + avg);
            avgDisplay.setText(Double.toString(avg));

        } catch (JSONException e) {
            e.printStackTrace();
        }


    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void submitSession(View view){
        final Toast submissionNotify = Toast.makeText(this, "Profile submitted", Toast.LENGTH_SHORT);
        final Toast errorNotify = Toast.makeText(this, "Submission error", Toast.LENGTH_SHORT);
        final TextView indexSubmit = (TextView) findViewById(R.id.indexDisplay);


        try {
            // From https://developer.android.com/training/volley/simple
            // https://stackoverflow.com/questions/33573803/how-to-send-a-post-request-using-volley-with-string-body
            RequestQueue queue = Volley.newRequestQueue(this);
            String url = "http://projrush-env.takuddxgcj.us-east-2.elasticbeanstalk.com/uploadData";
            String indexValue = indexSubmit.getText().toString();

            // Only need to define index
            final String requestBody = submitHolder.dataString;
            Log.d("VOLLEY", requestBody);

            StringRequest stringRequest = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    Log.i("VOLLEY", response);
                    submissionNotify.show();
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e("VOLLEY", error.toString());
                    errorNotify.show();
                }
            }) {
                @Override
                public String getBodyContentType() {
                    return "application/json; charset=utf-8";
                }

                @Override
                public byte[] getBody() throws AuthFailureError {
                    try {
                        return requestBody == null ? null : requestBody.getBytes("utf-8");
                    } catch (UnsupportedEncodingException uee) {
                        VolleyLog.wtf("Unsupported Encoding while trying to get the bytes of %s using %s", requestBody, "utf-8");
                        return null;
                    }
                }

                @Override
                protected Response<String> parseNetworkResponse(NetworkResponse response) {
                    String responseString = "";
                    if (response != null) {
                        responseString = String.valueOf(response.statusCode);
                        // can get more details such as response.headers
                    }
                    return Response.success(responseString, HttpHeaderParser.parseCacheHeaders(response));
                }
            };

            queue.add(stringRequest);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void addEntries(SensorData data){
        if(data != null) {
            spo2_series.appendData(new DataPoint(lastX, data.spo2), true, 10);
            ppg_series.appendData(new DataPoint(lastX, data.ppg_hr), true, 10);
            bodytemp_series.appendData(new DataPoint(lastX, data.bodytemp), true, 10);
            ecg_series.appendData(new DataPoint(lastX, data.ecg), true, 10);
            lastX++;
        }
    }

    private class DataGrabber extends Thread{
        @Override
        public void run(){
            String dummyMsg = "{TimeStamp:1234, Sp02:12.32, PPG_HR:24.36, BodyTemperature:24.35, ECG:509}";
            try {
                sensorDataDisplay.put(new SensorData(dummyMsg));
            } catch(InterruptedException e){};

            while(true){
                SensorData uploadData = sensorDataDisplay.poll();
                submitHolder = uploadData;
                Message displayUpdate = bluetoothRead.obtainMessage(UPLOAD_CHANGE, uploadData);
                bluetoothRead.sendMessage(displayUpdate);
                try {
                    Thread.sleep(2000);
                } catch(InterruptedException e){
                    Log.e(TAG, "Update display info got interrupted.", e);
                }
            }

        }
    }

    // Handles retrieving sensor data and updating the
    private class BufferReader extends Thread{
        @Override
        public void run() {
            while(true){
                try{
                    SensorData graphSensorInfo = sensorDataBuffer.poll();
                    if(graphSensorInfo != null) {
                        Log.d("Buffer", "Sensor Data from Q: " + graphSensorInfo.toString());
                        sensorDataDisplay.put(graphSensorInfo);
                    }
                    Message readMsg = bluetoothRead.obtainMessage(DATA_IN, graphSensorInfo);
                    bluetoothRead.sendMessage(readMsg);
                    Thread.sleep(180);
                } catch(InterruptedException e){
                    Log.e(TAG, "Buffer reader thread interrupted.", e);
                }


            }
        }
    }

    // Handles connecting to bluetooth device
    private class ConnectThread extends Thread{
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device){
            BluetoothSocket tmp = null;
            mmDevice = device;

            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket creation failed", e);
            }

            mmSocket = tmp;
        }

        public void run() {

            try {
                mmSocket.connect();
                Log.d(TAG, "Connected to socket");
            } catch (IOException connectException){
                // Can't connect, try to close socket
                try {
                    mmSocket.close();
                } catch (IOException closeException){
                    Log.e(TAG, "Coudn't close socket.", closeException);
                }
                // Should do something here, say restart app or loop a search?
                Log.d(TAG, "Couldn't connect to socket");
                return;
            }

            manageMyConnectedSocket(mmSocket);
        }

        public void cancel(){
            try {
                mmSocket.close();
            } catch(IOException e){
                Log.e(TAG, "Couldn't close socket", e);
            }
        }

        private void manageMyConnectedSocket(BluetoothSocket socket){
            ConnectedThread receiveThread = new ConnectedThread(socket);
            Log.d("RunnablePost", "Program start at: " + System.currentTimeMillis());

            receiveThread.start();
        }
    }

    // Handles the connection to a bluetooth device once made
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private byte[] mmBuffer;
        String sensorJSON;

        public ConnectedThread(BluetoothSocket socket){
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e){
                Log.e(TAG, "Error getting input stream.", e);
            }

            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error getting output stream.", e);
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run(){
            mmBuffer = new byte[1024];
            int numBytes;
            BufferReader uiRunner = new BufferReader();
            DataGrabber grab = new DataGrabber();
            uiRunner.start();
            grab.start();

            while(true) {
                try {
                    numBytes = mmInStream.read(mmBuffer);
                    String stringMessage = new String(mmBuffer, 0, numBytes);
                    SensorData newData = new SensorData(stringMessage);
                    sensorDataBuffer.put(newData);
                    Log.d("Buffer", "Buffer Status: " + sensorDataBuffer.toString());

                } catch(IOException e){
                    Log.d(TAG, "Input stream disconnected", e);
                    break;
                }
                catch(InterruptedException e){
                    Log.e(TAG, "Error in placing message to queue");

                }
                catch(NullPointerException e){
                    Log.e(TAG, "Tried to put null object in queue", e);
                }
            }
        }



    }
}



