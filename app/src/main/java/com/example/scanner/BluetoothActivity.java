package com.example.scanner;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Objects;

public class BluetoothActivity extends AppCompatActivity {

    private ArrayList<String> arrayList = new ArrayList<>();
    private ArrayList<Long> blueList = new ArrayList<>();
    private ArrayAdapter adapter;

    private ProgressBar mProgressBar;
    private BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    private boolean blueStartOn;
    private boolean scanning;
    int i = 0;
    private BroadcastReceiver bluereceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            Toast.makeText(context, "Found Device!", Toast.LENGTH_SHORT).show();
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                long rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
                String name = intent.getStringExtra(BluetoothDevice.EXTRA_NAME);
                arrayList.add(name + ": " + rssi + " dBm");
                adapter.notifyDataSetChanged();
                blueList.add(rssi);
                TextView blueExp = findViewById(R.id.exposureBox);

                double[] result = new scannerAppTools().getMw(blueList);
                String exposure = (result[0] + " Sum RSSI");
                blueExp.setText(exposure);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        scanning = false;
        try {
            Objects.requireNonNull(this.getSupportActionBar()).hide();
        } catch (NullPointerException ignored) {
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);

        adapter = new ArrayAdapter<>(this, R.layout.simple_list_item_1, arrayList);
        ListView listView = findViewById(R.id.blueList);
        listView.setAdapter(adapter);

        arrayList.add("~~~ Bluetooth List ~~~");
        mProgressBar = findViewById(R.id.progressBar);
        mProgressBar.setProgress(i);

    }

    public void scanBluetooth(View view) throws InterruptedException {
        if (!scanning) {
            scanning = true;
            i = 0;

            arrayList.clear();
            blueList.clear();
            if (bluetoothAdapter != null) {
                Toast.makeText(this, "Scanning Bluetooth", Toast.LENGTH_SHORT).show();
                if (!bluetoothAdapter.isEnabled()) {
                    blueStartOn = false;
                    bluetoothAdapter.enable();
                    long start = System.currentTimeMillis();
                    while ((System.currentTimeMillis() - start) < 3000 && (!bluetoothAdapter.isEnabled())) {
                        Thread.sleep(50);
                    }
                    Thread.sleep(500);
                } else {
                    blueStartOn = true;
                }

                IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                registerReceiver(bluereceiver, filter);

                bluetoothAdapter.startDiscovery();

                new CountDownTimer(5000, 100) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                        i++;
                        int percent = i * 100 * 100 / 5000;
                        mProgressBar.setProgress(percent);
                    }

                    @Override
                    public void onFinish() {
                        i++;
                        mProgressBar.setProgress(100);
                        if (!blueStartOn) {
                            scanning = false;
                            bluetoothAdapter.disable();
                        }
                    }
                }.start();
            } else {
                Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            unregisterReceiver(bluereceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (bluetoothAdapter != null) {
            if (blueStartOn) {
                Toast.makeText(this, "Leaving Bluetooth on!!", Toast.LENGTH_SHORT).show();
                if (!bluetoothAdapter.isEnabled()) {
                    bluetoothAdapter.enable();
                }
            } else {
                if (bluetoothAdapter.isEnabled()) {
                    bluetoothAdapter.disable();
                }
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        scanning = false;
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
