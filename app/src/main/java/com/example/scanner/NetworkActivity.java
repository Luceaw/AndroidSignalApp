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

import java.util.ArrayList;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class NetworkActivity extends AppCompatActivity {

    private ArrayList<String> arrayList = new ArrayList<>();
    private ArrayList valList = new ArrayList<>();
    long startime;
    private Timer timer = new Timer();
    private boolean running = false;
    private TextView timeTaken;
    private TextView exposurebox;
    private ArrayAdapter adapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        running = false;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_network);

        try {
            Objects.requireNonNull(this.getSupportActionBar()).hide();
        } catch (NullPointerException ignored) {
        }

        ListView listView = findViewById(R.id.networkList);
        adapter = new ArrayAdapter<>(this, R.layout.simple_list_item_1, arrayList);
        listView.setAdapter(adapter);

        timeTaken = findViewById(R.id.timeText);
        exposurebox = findViewById(R.id.exposureBox);

    }

    public void scanNetworks(View view) {
        if (!running) {
            TimerTask timerTask = new TimerTask() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (getResults() == 1) {
                                running = true;
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
        long time;
        arrayList.clear();
        valList.clear();
        startime = System.currentTimeMillis();
        TelephonyManager telephonyManager = (TelephonyManager) getApplicationContext()
                .getSystemService(Context.TELEPHONY_SERVICE);

        if (!telephonyManager.getAllCellInfo().isEmpty()) {

            ArrayList[] dBms = new scannerAppTools().telephonyDBm(telephonyManager.getAllCellInfo());
            valList = dBms[0];
            ArrayList times = dBms[1];
            ArrayList names = dBms[2];
            ArrayList status = dBms[3];

            if (names.size() == valList.size() && valList.size() == status.size()
                    && times.size() > 0 && valList.size() > 0) {

                time = (long) times.get(1);
                String string = (time + " s");
                timeTaken.setText(string);

                double[] sums = new scannerAppTools().getMw(valList);
                int dBmSum = (int) Math.round(sums[0]);
                int nsum = (int) Math.round(sums[1]);

                String exposure = (dBmSum + " dBm / " + "\n" + nsum + " ~nW");
                exposurebox.setText(exposure);

                for (int i = 0; i < names.size(); i++) {
                    Object name = names.get(i);
                    Object dBm = valList.get(i);
                    Object inUse = status.get(i);
                    String concat = (name + ": " + dBm + " dBm " + inUse);
                    arrayList.add(concat);
                }

                adapter.notifyDataSetChanged();
                return 1;
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

}
