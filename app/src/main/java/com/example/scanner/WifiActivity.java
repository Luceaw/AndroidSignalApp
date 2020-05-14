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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class WifiActivity extends AppCompatActivity {

    private WifiManager wifiManager;
    private ArrayList<String> arrayList = new ArrayList<>();
    private ArrayList<Long> valList = new ArrayList<>();
    private ArrayAdapter adapter;
    long startime;
    private boolean wifiScanning;
    private boolean wifistart;
    private TextView wifiText;
    private TextView wifiExp;
    private boolean hasPermission = true;

    // Broadcast receiver for Wifi scan results
    BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            wifiScanning = false;
            unregisterReceiver(wifiReceiver);

            List<ScanResult> results = wifiManager.getScanResults();

            String time_taken = (((System.currentTimeMillis() - startime) / 1000) + "s");
            Toast.makeText(context, "Received", Toast.LENGTH_SHORT).show();

            wifiText.setText(time_taken);

            // Calculate and set total exposure
            double[] result = new scannerAppTools().getMw(valList);
            String exposure = ("~" + result[0] + " Sum dBm / ~" + result[1] + " nW");
            wifiExp.setText(exposure);

            // Update wifi list
            for (ScanResult scanResult : results) {
                String name = scanResult.SSID;
                if (name.length() > 8) {
                    name = name.substring(0, 8);
                    name = (name + "...");
                    arrayList.add(name + ": " + scanResult.level + "dBm / " + scanResult.frequency + " MHz");
                } else {
                    arrayList.add(name + ": " + scanResult.level + "dBm / " + scanResult.frequency + " MHz");
                }
                valList.add((long) scanResult.level);
                adapter.notifyDataSetChanged();
            }

        }
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            wifistart = wifiManager.isWifiEnabled();
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi);

        try {
            Objects.requireNonNull(this.getSupportActionBar()).hide();
        } catch (NullPointerException ignored) {
        }

        wifiText = findViewById(R.id.timeText);
        wifiExp = findViewById(R.id.exposureBox);
        ListView wifiList = findViewById(R.id.wifiList);

        arrayList.add("Press Scan to get results");

        Button buttonScan = findViewById(R.id.scanBtn);
        buttonScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scanWifi();
            }
        });

        adapter = new ArrayAdapter<>(this, R.layout.simple_list_item_1, arrayList);
        wifiList.setAdapter(adapter);

        // Check permissions
        List<String> permissionsNeeded = new MainActivity().permissionsNeeded(this);
        if(new MainActivity().missingPermissions(permissionsNeeded, this)) {
            hasPermission = false;
        }
    }

    private void scanWifi() {

        // If app has permissions and start button already hasn't been pressed
        if(hasPermission) {

            // Clear lists
            arrayList.clear();
            valList.clear();
            adapter.notifyDataSetChanged();

            // Fail-safe
            if (System.currentTimeMillis() - startime > 10000) {
                wifiScanning = false;
            }

            if (!wifiScanning) {
                if (wifiManager != null) {
                    if (!wifiManager.isWifiEnabled()) {
                        wifiManager.setWifiEnabled(true);
                    }

                    registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
                    if (wifiManager.startScan()) {
                        wifiScanning = true;
                        Toast.makeText(this, "Scanning wifi....", Toast.LENGTH_SHORT).show();
                        startime = System.currentTimeMillis();
                    }
                }
            }
        } else {
            Toast.makeText(this, "Missing permissions!! Return to home to allow", Toast.LENGTH_SHORT).show();
        }
    }


    // Return Wifi to original state on change
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
        if (wifiReceiver != null) {
            try {
                unregisterReceiver(wifiReceiver);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // Buttons

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
