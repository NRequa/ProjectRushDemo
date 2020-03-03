package com.ee464k.demoapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.ee464k.demoapp.R;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.Random;

public class MainActivity extends AppCompatActivity {
    private static final Random RANDOM = new Random();
    private LineGraphSeries<DataPoint> series;
    private int lastX = 0;
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
    }

    @Override
    protected  void onResume(){
        super.onResume();

        // Thread to append data

        new Thread(new Runnable() {
            @Override
            public void run(){
                for(int i = 0; i < 100; i++){
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
        String url = "https://projrush-env.takuddxgcj.us-east-2.elasticbeanstalk.com/Average";

        // Request builder
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        avgDisplay.setText("Response: " + response.substring(0, 500));
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                avgDisplay.setText(error.toString());
            }

        });

        queue.add(stringRequest);
    }


    public void submitSession(View view){
        Toast submissionNotify = Toast.makeText(this, "Profile submitted", Toast.LENGTH_SHORT);
        submissionNotify.show();

    }

    private void addEntry(){
        series.appendData(new DataPoint(lastX++, RANDOM.nextDouble() * 10d), true, 10);
    }
}
