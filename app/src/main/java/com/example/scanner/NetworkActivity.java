package com.example.scanner;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

public class NetworkActivity extends AppCompatActivity {

    private ArrayList<List> arrayList = new ArrayList<>();
    private ArrayList valList = new ArrayList<>();
    private ArrayAdapter adapter;
    long startime;
    private static DecimalFormat df = new DecimalFormat("0.00");
    private Timer timer = new Timer();
    private TimerTask timerTask;
    private boolean running = false;
    private TextView timeTaken;
    private TextView exposurebox;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            Objects.requireNonNull(this.getSupportActionBar()).hide();
        } catch (NullPointerException ignored) {
        }
        running = false;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_network);

        ListView listView = findViewById(R.id.networkList);
        adapter = new ArrayAdapter<>(this, R.layout.simple_list_item_1, arrayList);
        listView.setAdapter(adapter);

        timeTaken = findViewById(R.id.timeText);
        exposurebox = findViewById(R.id.exposureBox);
    }

    public void scanNetworks(View view) {
        if (!running) {
            timerTask = new TimerTask() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // Check if Data is Enabled
                            // Check if null
                            try {
                                if (getResults() == 1) {
                                    running = true;
                                }
                            } finally {
                            }
                        }
                    });
                }
            };
            timer.schedule(timerTask, 0, 500);
        }
    }

    @SuppressLint("MissingPermission")
    public int getResults() {
        long time = 0;

        arrayList.clear();
        valList.clear();
        startime = System.currentTimeMillis();
        TelephonyManager telephonyManager = (TelephonyManager) getApplicationContext()
                .getSystemService(Context.TELEPHONY_SERVICE);

        if (!telephonyManager.getAllCellInfo().isEmpty()) {

            ArrayList[] dBms = new scannerAppTools().telephonyDBm(telephonyManager.getAllCellInfo());
            valList = dBms[0];
            ArrayList times = dBms[1];

            if (times.size() > 0) {
                time = (long) times.get(1);

                if (valList.size() > 0) {
                    double[] sums = new scannerAppTools().getMw(valList);
                    int dBmSum = (int) Math.round(sums[0]);
                    int nsum = (int) Math.round(sums[1]);

                    timeTaken.setText((int) time);
                    arrayList = new ArrayList[]{valList, valList};

                    adapter.notifyDataSetChanged();

                    String exposure = (dBmSum + " dBm / " + "\n" + nsum + " ~nW");
                    exposurebox.setText(exposure);

                    return 1;
                } else {
                    return 0;
                }
            } else {
                return 0;
            }
        } else {
            return 1;
        }



    }


    public void goHome(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        timer.cancel();
    }

    public void goAll(View view) {
        Intent intent = new Intent(this, allActivityGraphs.class);
        startActivity(intent);
        timer.cancel();
    }

    public void goBluetooth(View view) {
        Intent intent = new Intent(this, BluetoothActivity.class);
        startActivity(intent);
        timer.cancel();
    }

    public void goNetwork(View view) {
        Intent intent = new Intent(this, NetworkActivity.class);
        startActivity(intent);
        timer.cancel();
    }

    public void goWifi(View view) {
        Intent intent = new Intent(this, WifiActivity.class);
        startActivity(intent);
        timer.cancel();
    }

    private long getValue(String fullS, String startS, String stopS) {
        int index = fullS.indexOf(startS) + (startS).length();
        int endIndex = fullS.indexOf(stopS, index);

        String segment = fullS.substring(index, endIndex).trim();

        return new Scanner(segment).useDelimiter("\\D+").nextLong();
    }

}
