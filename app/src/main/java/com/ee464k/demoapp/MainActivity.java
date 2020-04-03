package com.ee464k.demoapp;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
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
import com.ee464k.demoapp.R;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.UnsupportedEncodingException;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class MainActivity extends AppCompatActivity {
    private static final Random RANDOM = new Random();
    private LineGraphSeries<DataPoint> series;
    private int lastX = 0;
    private double generatedIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // GraphView practice block
        GraphView graph = (GraphView) findViewById(R.id.graph);
        graph.setTitle("Stress Score");
        series = new LineGraphSeries<>();
        graph.addSeries(series);

        // ViewPort customization
        Viewport viewport = graph.getViewport();
        viewport.setXAxisBoundsManual(true);
        viewport.setMinX(0);
        viewport.setMaxX(10);
        viewport.setScrollable(true);

        // Set index
        final TextView indexSubmit = (TextView) findViewById(R.id.indexDisplay);
        generatedIndex = ThreadLocalRandom.current().nextDouble(0,100);
        generatedIndex = Math.round(generatedIndex);
        indexSubmit.setText(Double.toString(generatedIndex));
    }

    @Override
    protected  void onResume(){
        super.onResume();

        // Thread to append data

        new Thread(new Runnable() {
            @Override
            public void run(){
                while(true){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run(){
                            addEntry();
                        }
                    });

                    try{
                        Thread.sleep(700);
                    } catch(InterruptedException e){
                        e.printStackTrace();
                    }

                }
            }
        }).start();


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
                        displayAverage(response);
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
            String url = "http://projrush-env.takuddxgcj.us-east-2.elasticbeanstalk.com/User";
            String indexValue = indexSubmit.getText().toString();

            // Only need to define index
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("index", indexSubmit.getText().toString());
            final String requestBody = jsonBody.toString();

            StringRequest stringRequest = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    Log.i("VOLLEY", response);
                    submissionNotify.show();

                    // Set new index for submission
                    double generatedIndex = ThreadLocalRandom.current().nextDouble(0,100);
                    generatedIndex = Math.round(generatedIndex);
                    indexSubmit.setText(Double.toString(generatedIndex));
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
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private void addEntry(){
        series.appendData(new DataPoint(lastX++, RANDOM.nextDouble() * 10d), true, 10);
    }
}
