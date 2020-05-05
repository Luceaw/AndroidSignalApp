package com.example.scanner;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.TelephonyManager;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class AllActivity extends AppCompatActivity {

    private ArrayList<Long> blueList = new ArrayList<>();
    private ArrayList<Long> netList = new ArrayList<>();
    private ArrayList<Long> wifiList = new ArrayList<>();

    long blueTime;
    long netTime;
    long wifiTime;

    private WifiManager wifiManager;
    private BluetoothAdapter bluetoothAdapter;
    private TelephonyManager telephonyManager;

    private TextView wifiExp;
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
    private TextView wifiText;

    private Timer timer = new Timer();
    private boolean running = false;
    private TextView blueText;
    private boolean wifistartOn;
    private TextView netText;


    public void scanAll(View view) throws InterruptedException {
        if (!running) {
            startBluetooth();
            startWifi();
            TimerTask timerTask = new TimerTask() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                if (getNetwork() == 1) {
                                    running = true;
                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            };
            timer.schedule(timerTask, 0, 100);
        }
    }

    private boolean blueStartOn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        running = false;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all);

        try {
            Objects.requireNonNull(this.getSupportActionBar()).hide();
        } catch (NullPointerException ignored) {
        }

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        telephonyManager = (TelephonyManager) getApplicationContext()
                .getSystemService(Context.TELEPHONY_SERVICE);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

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

    }

    public void startBluetooth() throws InterruptedException {

        if (bluetoothAdapter != null) {
            if (!bluetoothAdapter.isEnabled()) {
                bluetoothAdapter.enable();
                long start = System.currentTimeMillis();
                while ((System.currentTimeMillis() - start) < 3000 && (!bluetoothAdapter.isEnabled())) {
                    Thread.sleep(50);
                }
            } else {
                if (!bluetoothAdapter.isDiscovering()) {
                    blueList.clear();
                    IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                    registerReceiver(bluereceiver, filter);
                    bluetoothAdapter.startDiscovery();
                }
            }
        } else {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
        }
    }


    @SuppressLint("MissingPermission")
    public int getNetwork() throws InterruptedException {

        String wifitimetaken = (((System.currentTimeMillis() - wifiTime) / 1000) + " s");
        wifiText.setText(wifitimetaken);

        String bluetimetaken = (((System.currentTimeMillis() - blueTime) / 1000) + " s");
        blueText.setText(bluetimetaken);

        if (System.currentTimeMillis() - blueTime > 10000) {
            blueList.clear();
            TextView textView = findViewById(R.id.bluetoothBox);
            textView.setText("");
            startBluetooth();
        }

        netList.clear();
        netTime = System.currentTimeMillis();
        long time = 0;
        if (!telephonyManager.getAllCellInfo().isEmpty()) {
            for (final CellInfo cellInfo : telephonyManager.getAllCellInfo()) {
                if (cellInfo != null) {
                    long dBm;
                    long millisecondsSinceEvent = (SystemClock.elapsedRealtimeNanos() - cellInfo.getTimeStamp()) / 1000000L;
                    long timeOfEvent = System.currentTimeMillis() - millisecondsSinceEvent;
                    time = (System.currentTimeMillis() - timeOfEvent) / 1000;

                    if (cellInfo instanceof CellInfoCdma) {
                        dBm = ((CellInfoCdma) cellInfo).getCellSignalStrength().getDbm();
                    } else {
                        if (cellInfo instanceof CellInfoGsm) {
                            dBm = ((CellInfoGsm) cellInfo).getCellSignalStrength().getDbm();
                        } else {
                            if (cellInfo instanceof CellInfoLte) {
                                dBm = ((CellInfoLte) cellInfo).getCellSignalStrength().getDbm();
                            } else {
                                if (cellInfo instanceof CellInfoWcdma) {
                                    dBm = ((CellInfoWcdma) cellInfo).getCellSignalStrength().getDbm();
                                } else {
                                    dBm = -(new scannerAppTools().getValue(cellInfo.toString(), "CellSignalStrength", "level"));
                                    Toast.makeText(this, "Unrecognised cell info!", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    }
                    if (dBm > -250) {
                        netList.add(dBm);
                    }
                }
            }
        }

        if (netList.size() > 0) {

            String timeString = ((int) time + " s");
            netText.setText(timeString);

            double[] result = new scannerAppTools().getMw(netList);
            changeSize(result[0], "networkBox");
            String exposure = (result[0] + " Sum dBm / ~" + result[1] + " nW");
            netExp.setText(exposure);

            return 1;
        } else {
            return 0;
        }
    }

    private void startWifi() {
        wifiList.clear();
        if (wifiManager != null) {
            if (!wifiManager.isWifiEnabled()) {
                wifiManager.setWifiEnabled(true);
            }
            registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
            wifiManager.startScan();
        }
    }

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
