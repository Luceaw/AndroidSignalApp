package com.example.scanner;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.security.ProtectionDomain;
import java.text.DecimalFormat;
import java.util.ArrayList;

public class BluetoothActivity extends AppCompatActivity {

    private BluetoothManager bluetoothManager;
    private ArrayList<String> arrayList = new ArrayList<>();
    private ArrayList<Integer> valList = new ArrayList<>();
    private ArrayAdapter adapter;

    private ProgressBar mProgressBar;
    private CountDownTimer mCountDownTimer;
    int i=0;

    long startime;
    private static DecimalFormat df = new DecimalFormat("0.00");
    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    private boolean blueStartOn;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try
        {
            this.getSupportActionBar().hide();
        }
        catch (NullPointerException e){
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);
        arrayList.add("~~~ Bluetooth List ~~~");
        ListView listView = findViewById(R.id.blueList);
        adapter = new ArrayAdapter<>(this, R.layout.simple_list_item_1, arrayList);
        listView.setAdapter(adapter);
        mProgressBar=(ProgressBar)findViewById(R.id.progressBar);
        mProgressBar.setProgress(i);

    }


    public void scanBluetooth(View view) throws InterruptedException {
        i = 0;
        arrayList.clear();
        valList.clear();
        startime = System.currentTimeMillis();
        if (bluetoothAdapter != null) {
            Toast.makeText(this, "Scanning Bluetooth", Toast.LENGTH_LONG).show();
            if(!bluetoothAdapter.isEnabled()){
                blueStartOn = false;
                bluetoothAdapter.enable();
                long start = System.currentTimeMillis();
                while((System.currentTimeMillis() - start) < 3000 && (!bluetoothAdapter.isEnabled())){
                    Thread.sleep(50);
                }
                Thread.sleep(500);
            } else {
                blueStartOn = true;
            }

            // Register for broadcasts when a device is discovered.
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(receiver, filter);

            bluetoothAdapter.startDiscovery();

            mCountDownTimer  = new CountDownTimer(5000 , 100) {
                @Override
                public void onTick(long millisUntilFinished) {
                i++;
                int percent = i*100*100/5000;
                mProgressBar.setProgress(percent);
                }

                @Override
                public void onFinish() {
                    i++;
                    mProgressBar.setProgress(100);
                    if (!blueStartOn) {
                        bluetoothAdapter.disable();
                    }
                }
            }.start();


        }
        else {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_LONG).show();
        }
    }

    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver receiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            Toast.makeText(context ,"Found Device!", Toast.LENGTH_LONG).show();
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI,Short.MIN_VALUE);
                String name = intent.getStringExtra(BluetoothDevice.EXTRA_NAME);
                arrayList.add(name + ": " + rssi + " dBm");
                valList.add(rssi);
                adapter.notifyDataSetChanged();

                TextView textView = findViewById(R.id.timeText);

                // I miss python
                double Wsum = 0;
                double dBm;
                double mW;
                for(int i = 0; i < valList.size(); i++) {
                    dBm = valList.get(i);
                    mW = Math.pow(10, ((dBm - 30) / 10));
                    Wsum += mW;
                }
                String nsum = df.format(Wsum*1000000000);
                String dBmSum = df.format (10*(Math.log10(1000*Wsum)));
                String exposure = (dBmSum + " total RSSI / " + "\n" + nsum + " assumed nW");
                TextView textView2 = findViewById(R.id.exposureBox);
                textView2.setText(exposure);
            }
        }
    };

    @Override
    public void onPause() {
        super.onPause();
        try {
            unregisterReceiver(receiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
            if (bluetoothAdapter != null) {
                if(blueStartOn) {
                    Toast.makeText(this, "Leaving Bluetooth on!!", Toast.LENGTH_SHORT).show();
                    if (!bluetoothAdapter.isEnabled()) {
                        bluetoothAdapter.enable();
                    }
                    }
                else {
                    if (bluetoothAdapter.isEnabled()) {
                        bluetoothAdapter.disable();
                    }
                }
        }

    }



    public void goHome(View view) {
        Intent intent = new Intent (this, MainActivity.class);
        startActivity(intent);
    }
    public void goAll(View view) {
        Intent intent = new Intent (this, allActivityGraphs.class);
        startActivity(intent);
    }
    public void goBluetooth(View view) {
        Intent intent = new Intent (this, BluetoothActivity.class);
        startActivity(intent);
    }
    public void goNetwork(View view) {
        Intent intent = new Intent (this, NetworkActivity.class);
        startActivity(intent);
    }
    public void goWifi(View view) {
        Intent intent = new Intent (this, WifiActivity.class);
        startActivity(intent);
    }

}
