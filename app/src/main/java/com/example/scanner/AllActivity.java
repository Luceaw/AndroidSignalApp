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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

public class AllActivity extends AppCompatActivity {

    private ArrayList<Long> blueList = new ArrayList<>();
    private ArrayList<Long> netList = new ArrayList<>();
    private ArrayList<Long> wifiList = new ArrayList<>();

    long blueTime;
    long netTime;
    long wifiTime;

    private static DecimalFormat df = new DecimalFormat("0.00");
    private Timer timer = new Timer();
    private boolean running = false;

    private WifiManager wifiManager;
    private BluetoothAdapter bluetoothAdapter;
    private TelephonyManager telephonyManager;

    private boolean wifistartOn;

    private boolean blueStartOn;
    private int bluecount = 0;

    private final BroadcastReceiver bluereceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            bluecount++;
            blueTime = System.currentTimeMillis();
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                long rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
                blueList.add(rssi);
            }
            if (blueList.size() > 0) {
                String exposure = getMw(blueList, "bluetoothBox");
                TextView blueExp = findViewById(R.id.bluetoothBox);
                blueExp.setText(exposure);
            }
        }
    };

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
                            } finally {
                            }
                        }
                    });
                }
            };
            timer.schedule(timerTask, 0, 100);
        }
    }

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
                String exposure = getMw(wifiList, "wifiBox");
                TextView textView2 = findViewById(R.id.wifiBox);
                textView2.setText(exposure);

            }
            try {
                startWifi();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        try {
            Objects.requireNonNull(this.getSupportActionBar()).hide();
        } catch (NullPointerException ignored) {
        }

        running = false;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        telephonyManager = (TelephonyManager) getApplicationContext()
                .getSystemService(Context.TELEPHONY_SERVICE);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        if (wifiManager != null) {
            wifistartOn = wifiManager.isWifiEnabled();
        }
        if (bluetoothAdapter != null) {
            blueStartOn = bluetoothAdapter.isEnabled();
        }

        blueTime = System.currentTimeMillis();
        wifiTime = System.currentTimeMillis();

    }

    public void startBluetooth() throws InterruptedException {

        if (bluetoothAdapter != null) {
            if (!bluetoothAdapter.isEnabled()) {
                bluetoothAdapter.enable();
                long start = System.currentTimeMillis();
                while ((System.currentTimeMillis() - start) < 3000 && (!bluetoothAdapter.isEnabled())) {
                    Thread.sleep(50);
                }
                Thread.sleep(500);
            }

            if (!bluetoothAdapter.isDiscovering()) {
                blueList.clear();
                IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                registerReceiver(bluereceiver, filter);
                bluetoothAdapter.startDiscovery();
            }
        } else {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_LONG).show();
        }
    }

    @SuppressLint("MissingPermission")
    public int getNetwork() throws InterruptedException {

        String wifitimetaken = (((System.currentTimeMillis() - wifiTime) / 1000) + " s");
        TextView wifiText = findViewById(R.id.wifiTime);
        wifiText.setText(wifitimetaken);

        String bluetimetaken = (((System.currentTimeMillis() - blueTime) / 1000) + " s");
        TextView blueText = findViewById(R.id.blueTime);
        blueText.setText(bluetimetaken);

        if (System.currentTimeMillis() - blueTime > 10000) {
            blueList.clear();
            TextView textView = findViewById(R.id.bluetoothBox);
            textView.setText("");
            startBluetooth();
        }

        netList.clear();
        netTime = System.currentTimeMillis();
        long time = (long) 0;
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
                                    dBm = -getValue(cellInfo.toString(), "CellSignalStrength", "level");
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
            TextView textView = findViewById(R.id.netTime);

            String timeString = ((int) time + " s");
            textView.setText(timeString);
            TextView netExposure = findViewById(R.id.networkBox);
            String exposure = getMw(netList, "networkBox");
            netExposure.setText(exposure);
            return 1;
        } else {
            return 0;
        }
    }

    private void startWifi() throws InterruptedException {
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

    private long getValue(String fullS, String startS, String stopS) {
        int index = fullS.indexOf(startS) + (startS).length();
        int endIndex = fullS.indexOf(stopS, index);
        String segment = fullS.substring(index, endIndex).trim();
        return new Scanner(segment).useDelimiter("\\D+").nextLong();
    }

    private String getService(String name) {
        return name.substring(8).trim();
    }

    public String getMw(ArrayList<Long> list, String viewString) {

        if (list.size() > 0) {
            double Wsum = 0;
            double dBm;
            double mW;
            for (int i = 0; i < list.size(); i++) {
                dBm = list.get(i);
                if (dBm < 0 && dBm > -200) {
                    mW = Math.pow(10, ((dBm - 30) / 10));
                    Wsum += mW;
                }
            }
            String nsum = df.format(Wsum * 1000000000);
            String dBmSum = df.format(10 * (Math.log10(1000 * Wsum)));

            int id = getResources().getIdentifier(viewString, "id", getPackageName());
            float dbFloat = Float.parseFloat(dBmSum);
            int dbInt = Math.round(dbFloat);

            TextView box = findViewById(id);
            ViewGroup.LayoutParams params = box.getLayoutParams();
            int newHeight = (40 + (20 * (((dbInt + 92) / 2) / 8)));

            params.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, newHeight, getResources().getDisplayMetrics());
            box.setLayoutParams(params);

            return (String) (dBmSum + " dBm / " + "\n" + nsum + " nW");
        } else {
            return "null";
        }
    }
}
