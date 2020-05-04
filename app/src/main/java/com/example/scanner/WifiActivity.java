package com.example.scanner;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class WifiActivity extends AppCompatActivity {

    private WifiManager wifiManager;
    private ArrayList<String> arrayList = new ArrayList<>();
    private ArrayList<Integer> valList = new ArrayList<>();
    private ArrayAdapter adapter;
    long startime;
    private static DecimalFormat df = new DecimalFormat("0.00");
    private boolean wifiScanning;
    private boolean wifistart;

    BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            wifiScanning = false;
            List<ScanResult> results = wifiManager.getScanResults();
            unregisterReceiver(wifiReceiver);

            String time_taken = (((System.currentTimeMillis() - startime) / 1000) + "s");
            Toast.makeText(context, "Received", Toast.LENGTH_SHORT).show();
            TextView textView = findViewById(R.id.timeText);
            textView.setText(time_taken);

            for (ScanResult scanResult : results) {
                String name = scanResult.SSID;
                if (name.length() > 8) {
                    name = name.substring(0, 8);
                    name = (name + "...");
                    arrayList.add(name + ": " + scanResult.level + "dBm / " + scanResult.frequency + "MHz / " + scanResult.channelWidth + " MHz");
                } else {
                    arrayList.add(name + ": " + scanResult.level + " / " + scanResult.frequency + " / " + scanResult.channelWidth);
                }
                valList.add(scanResult.level);
                adapter.notifyDataSetChanged();
            }

            // I miss python
            double Wsum = 0;
            double dBm;
            double mW;
            for (int i = 0; i < valList.size(); i++) {
                dBm = valList.get(i);
                mW = Math.pow(10, ((dBm - 30) / 10));
                Wsum += mW;
            }
            String nsum = df.format(Wsum * 1000000000);
            String dBmSum = df.format(10 * (Math.log10(1000 * Wsum)));
            String exposure = (dBmSum + " dBm / " + "\n" + nsum + " nW");
            TextView textView2 = findViewById(R.id.exposureBox);
            textView2.setText(exposure);

        }
    };

    private void scanWifi() throws InterruptedException {

        if (System.currentTimeMillis() - startime > 10000) {
            wifiScanning = false;
        }

        if (!wifiScanning) {

            if (wifiManager != null) {

                if (!wifiManager.isWifiEnabled()) {
                    wifiManager.setWifiEnabled(true);
                }

                arrayList.clear();
                valList.clear();
                registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
                if (wifiManager.startScan()) {
                    wifiScanning = true;
                    Toast.makeText(this, "Scanning wifi....", Toast.LENGTH_LONG).show();
                    startime = System.currentTimeMillis();
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            Objects.requireNonNull(this.getSupportActionBar()).hide();
        } catch (NullPointerException ignored) {
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi);

        arrayList.add("~~~ Router list ~~~");

        Button buttonScan = findViewById(R.id.scanBtn);
        buttonScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    scanWifi();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        ListView listView = findViewById(R.id.wifiList);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        adapter = new ArrayAdapter<>(this, R.layout.simple_list_item_1, arrayList);
        listView.setAdapter(adapter);
        if (wifiManager != null) {
            wifistart = wifiManager.isWifiEnabled();
        }
    }

    public void onPause() {
        super.onPause();
        if (wifiManager != null) {
            if (wifistart) {
                if (!wifiManager.isWifiEnabled()) {
                    wifiManager.setWifiEnabled(true);
                }
            } else {
                if (wifiManager.isWifiEnabled()) {
                    wifiManager.setWifiEnabled(false);
                }
            }
        }
    }

    public void goHome(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    public void goAll(View view) {
        Intent intent = new Intent(this, allActivityGraphs.class);
        startActivity(intent);
    }

    public void goBluetooth(View view) {
        Intent intent = new Intent(this, BluetoothActivity.class);
        startActivity(intent);
    }

    public void goNetwork(View view) {
        Intent intent = new Intent(this, NetworkActivity.class);
        startActivity(intent);
    }

    public void goWifi(View view) {
        Intent intent = new Intent(this, WifiActivity.class);
        startActivity(intent);
    }


}
