package com.luceaw.scanner;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.telephony.CellInfo;
import android.telephony.TelephonyManager;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class AllActivity extends AppCompatActivity {

    private ArrayList<Long> blueList = new ArrayList<>();
    private ArrayList netList = new ArrayList<>();
    private ArrayList<Long> wifiList = new ArrayList<>();

    long blueTime;
    long netTime;
    long wifiTime;

    private WifiManager wifiManager;
    private BluetoothAdapter bluetoothAdapter;
    private TelephonyManager telephonyManager;

    private TextView wifiExp;
    // Broadcast receiver for Wifi scan
    BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            unregisterReceiver(wifiReceiver);
            List<ScanResult> results = wifiManager.getScanResults();
            wifiTime = System.currentTimeMillis();
            for (ScanResult scanResult : results) {
                wifiList.add((long) scanResult.level);
            }

            if (wifiList.size() > 0) {
                double[] result = new scannerAppTools().getMw(wifiList);
                changeSize(result[0], "wifiBox");
                String exposure = (result[0] + " Sum dBm / ~" + result[1] + " nW");
                wifiExp.setText(exposure);
            }
            startWifi();
        }
    };
    private TextView netExp;
    private TextView blueExp;
    // Broadcast receiver for Bluetooth Scan
    BroadcastReceiver bluereceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            blueTime = System.currentTimeMillis();
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                long rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
                blueList.add(rssi);
            }
            if (blueList.size() > 0) {
                double[] result = new scannerAppTools().getMw(blueList);
                changeSize(result[0], "bluetoothBox");
                String exposure = (result[0] + " Sum RSSI");
                blueExp.setText(exposure);
            }
        }
    };
    private TextView blueText;
    private TextView netText;
    // Callback for cell info
    public TelephonyManager.CellInfoCallback cellInfoCallback = new TelephonyManager.CellInfoCallback() {
        @Override
        public void onCellInfo(@NonNull List<CellInfo> cellInfo) {
            long time;
            ArrayList[] dBms = new scannerAppTools().telephonyDBm(cellInfo);
            netList = dBms[0];
            ArrayList times = dBms[1];
            ArrayList names = dBms[2];
            ArrayList status = dBms[3];

            if (names.size() == netList.size() && netList.size() == status.size()
                    && times.size() > 0 && netList.size() > 0) {
                time = (long) times.get(0);
                double[] result = new scannerAppTools().getMw(netList);
                changeSize(result[0], "networkBox");
                String timeString = ((int) time/1000 + " s");
                String exposure = (result[0] + " Sum dBm / ~" + result[1] + " nW");
                netExp.setText(exposure);
                netText.setText(timeString);
            }
        }
    };
    private TextView wifiText;
    private Timer timer = new Timer();
    private boolean running = false;
    private boolean blueStartOn;
    private boolean wifistartOn;

    private boolean hasPermission = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        running = false;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all);
        Thread.setDefaultUncaughtExceptionHandler(new TopExceptionHandler(this));


        try {
            Objects.requireNonNull(this.getSupportActionBar()).hide();
        } catch (NullPointerException ignored) {
        }

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        telephonyManager = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        // Record start state
        if (bluetoothAdapter != null) {
            blueStartOn = bluetoothAdapter.isEnabled();
        }
        if (wifiManager != null) {
            wifistartOn = wifiManager.isWifiEnabled();
        }

        blueTime = System.currentTimeMillis();
        wifiTime = System.currentTimeMillis();

        wifiExp = findViewById(R.id.wifiBox);
        netExp = findViewById(R.id.networkBox);
        blueExp = findViewById(R.id.bluetoothBox);

        wifiText = findViewById(R.id.wifiTime);
        blueText = findViewById(R.id.blueTime);
        netText = findViewById(R.id.netTime);

        // Check permissions
        List<String> permissionsNeeded = new MainActivity().permissionsNeeded(this);
        if(new MainActivity().missingPermissions(permissionsNeeded, this)) {
            hasPermission = false;
        }

    }

    // Main scan loop
    public void scanAll(View view) {

        // If app has permissions and start button already hasn't been pressed
        if(hasPermission) {
            if (!running) {
                startBluetooth();
                startWifi();
                TimerTask timerTask = new TimerTask() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                getNetwork();
                                running = true;
                            }
                        });
                    }
                };
                timer.schedule(timerTask, 0, 100);
            }
        } else {
            Toast.makeText(this, "Missing permissions!! Return to home to allow", Toast.LENGTH_SHORT).show();
        }
    }

    // Cell Network results method, called on a loop
    public void getNetwork() {
        startBluetooth(); // Call on a loop to avoid sleep

        // Update result times
        String wifitimetaken = (((System.currentTimeMillis() - wifiTime) / 1000) + " s");
        wifiText.setText(wifitimetaken);

        String bluetimetaken = (((System.currentTimeMillis() - blueTime) / 1000) + " s");
        blueText.setText(bluetimetaken);

        if (System.currentTimeMillis() - blueTime > 10000) {
            blueList.clear();
            TextView textView = findViewById(R.id.bluetoothBox);
            textView.setText("");
        }

        netList.clear();
        netTime = System.currentTimeMillis();
        if (telephonyManager != null) {
            telephonyManager.requestCellInfoUpdate(this.getMainExecutor(), cellInfoCallback);
        }
    }

    private long enabling = 0;
    // Bluetooth results method
    public void startBluetooth() {
        if (bluetoothAdapter != null){
            if((System.currentTimeMillis() - enabling) > 2000) {
                if (!bluetoothAdapter.isEnabled()) {
                    enabling = System.currentTimeMillis();
                    bluetoothAdapter.enable();
                } else { // Second pass if not enabled to start with, always true
                    if (!bluetoothAdapter.isDiscovering()) {
                        blueList.clear();
                        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                        registerReceiver(bluereceiver, filter);
                        bluetoothAdapter.startDiscovery();
                    }
                }
            }
        } else {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
        }
    }

    // Wifi results method
    private void startWifi() {
        wifiList.clear();
        if (wifiManager != null) {
            if (!wifiManager.isWifiEnabled()) {
                wifiManager.setWifiEnabled(true);
            }
            registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
             try{
                wifiManager.startScan();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // Return to start state on activity change
    public void onPause() {
        super.onPause();
        try {
            unregisterReceiver(bluereceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            unregisterReceiver(wifiReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
        timer.cancel();

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
        if (wifiManager != null) {
            if (wifistartOn) {
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

    // Change size of the boxes according to their values
    private void changeSize(double dbDouble, String viewString) {
        int dbInt = (int) Math.round(dbDouble);
        int id = getResources().getIdentifier(viewString, "id", getPackageName());
        TextView box = findViewById(id);
        ViewGroup.LayoutParams params = box.getLayoutParams();
        int newHeight = (40 + (20 * (((dbInt + 92) / 2) / 8)));
        params.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, newHeight, getResources().getDisplayMetrics());
        box.setLayoutParams(params);
    }

    public void onResume() {
        super.onResume();
        running = false;
        timer = new Timer();
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

    public void goSwitch(View view) {
        Intent intent = new Intent(this, allActivityGraphs.class);
        startActivity(intent);
    }


}
