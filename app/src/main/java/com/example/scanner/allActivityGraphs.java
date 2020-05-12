package com.example.scanner;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.telephony.CellInfo;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.View;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.PointsGraphSeries;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class allActivityGraphs extends AppCompatActivity {

    private ArrayList<Long> blueList = new ArrayList<>();
    long blueTime;
    long netTime;
    long wifiTime;
    private StringBuilder combinedData = new StringBuilder();
    private ArrayList<Long> wifiList = new ArrayList<>();
    private WifiManager wifiManager;
    private BluetoothAdapter bluetoothAdapter;
    private TelephonyManager telephonyManager;

    private LineGraphSeries<DataPoint> blueSeries;
    BroadcastReceiver bluereceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            blueTime = System.currentTimeMillis();
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                long rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
                blueList.add(rssi);
            }
            if (blueList.size() > 0) {
                double[] sums = new scannerAppTools().getMw(blueList);
                int yVal = (int) Math.round(sums[0]);
                int xVal = Math.round(System.currentTimeMillis() - startTime) / 1000;
                blueSeries.appendData(new DataPoint(xVal, yVal), true, 1000);
                combinedData.append("\n").append(xVal + ",").append(yVal);
            }
        }
    };
    private LineGraphSeries<DataPoint> netSeries;
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
                time = (long) times.get(1);
                double[] sums = new scannerAppTools().getMw(netList);
                int yVal = (int) Math.round(sums[0]);
                int xVal = Math.round(System.currentTimeMillis() - startTime) / 1000;
                netSeries.appendData(new DataPoint(xVal, yVal), true, 4000);
                if (time < 101) {
                    netPointSeries.appendData(new DataPoint(xVal, yVal), true, 1000);
                    combinedData.append("\n").append(xVal + ",,").append(yVal);
                }
            }
        }
    };
    private LineGraphSeries<DataPoint> wifiSeries;

    private ArrayList netList = new ArrayList<>();
    private PointsGraphSeries<DataPoint> netPointSeries;
    private Timer timer = new Timer();
    private boolean running = false;

    private long startTime;
    private boolean blueStartOn;
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
                double[] sums = new scannerAppTools().getMw(wifiList);
                int yVal = (int) Math.round(sums[0]);
                int xVal = Math.round(System.currentTimeMillis() - startTime) / 1000;
                wifiSeries.appendData(new DataPoint(xVal, yVal), true, 1000);
                combinedData.append("\n").append(xVal + ",,,").append(yVal);
            }
            startWifi();
        }
    };
    private Switch screenSwitch;
    private EditText xMaxText;
    private GraphView graph;

    private boolean wifistartOn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        running = false;
        startTime = System.currentTimeMillis();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_graphs);

        try {
            Objects.requireNonNull(this.getSupportActionBar()).hide();
        } catch (NullPointerException ignored) {
        }

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        telephonyManager = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        if (bluetoothAdapter != null) {
            blueStartOn = bluetoothAdapter.isEnabled();
        }
        if (wifiManager != null) {
            wifistartOn = wifiManager.isWifiEnabled();
        }

        blueTime = System.currentTimeMillis();
        wifiTime = System.currentTimeMillis();

        graph = findViewById(R.id.graph);
        blueSeries = new LineGraphSeries<>(new DataPoint[]{new DataPoint(-1, -121), new DataPoint(-0, -121)});
        netSeries = new LineGraphSeries<>(new DataPoint[]{new DataPoint(-1, -121), new DataPoint(-0, -121)});
        netPointSeries = new PointsGraphSeries<>(new DataPoint[]{new DataPoint(-1, -121), new DataPoint(-0, -121)});
        wifiSeries = new LineGraphSeries<>(new DataPoint[]{new DataPoint(-1, -121), new DataPoint(-0, -121)});

        blueSeries.setColor(Color.BLUE);
        netSeries.setColor(Color.rgb(181, 0, 147));
        netPointSeries.setColor(Color.rgb(255, 0, 207));
        wifiSeries.setColor(Color.YELLOW);

        blueSeries.setThickness(10);
        netSeries.setThickness(10);
        wifiSeries.setThickness(10);

        graph.addSeries(blueSeries);
        graph.addSeries(netSeries);
        graph.addSeries(netPointSeries);
        graph.addSeries(wifiSeries);

        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(60);

        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinY(-120);
        graph.getViewport().setMaxY(-20);

        graph.getGridLabelRenderer().setHorizontalAxisTitle("Time (s)");
        graph.getGridLabelRenderer().setVerticalAxisTitle("Sum dBm (C/W) / RSSI (B)");

        graph.getGridLabelRenderer().setVerticalAxisTitleColor(Color.GREEN);
        graph.getGridLabelRenderer().setHorizontalAxisTitleColor(Color.GREEN);

        graph.getGridLabelRenderer().setHorizontalLabelsColor(Color.WHITE);
        graph.getGridLabelRenderer().setVerticalLabelsColor(Color.WHITE);
        graph.getGridLabelRenderer().setNumVerticalLabels(10);
        graph.getGridLabelRenderer().setNumHorizontalLabels(10);
        graph.getGridLabelRenderer().setGridColor(Color.WHITE);
        graph.getGridLabelRenderer().setHorizontalLabelsAngle(135);

        int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 19, getResources().getDisplayMetrics());
        netPointSeries.setSize((float) height / 5);

        graph.getGridLabelRenderer().setHorizontalAxisTitleTextSize(height);
        graph.getGridLabelRenderer().setVerticalAxisTitleTextSize(height);

        graph.getGridLabelRenderer().reloadStyles();

        screenSwitch = findViewById(R.id.screenSwitch);
        xMaxText = findViewById(R.id.graphMax);
        xMaxText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                setGraph();
            }
        });

        combinedData.append("Time,Sum_Bluetooth_RSSI,Sum_Cell_dBm,Sum_Wifi_dBm");

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
            int xVal = Math.round(System.currentTimeMillis() - startTime) / 1000;
            blueSeries.appendData(new DataPoint(xVal, -119), true, 1000);
        }
    }

    public void screenOn(View view) {
        if (screenSwitch != null) {
            if (screenSwitch.isChecked()) {
                view.setKeepScreenOn(true);
            } else {
                view.setKeepScreenOn(false);
            }
        }
    }


    public void exportData(View view) {
        try {
            FileOutputStream out = this.openFileOutput("data.csv", Context.MODE_PRIVATE);
            out.write(combinedData.toString().getBytes());
            out.close();

            Context context = getApplicationContext();
            File savedfile = new File(getFilesDir(), "data.csv");
            Uri path = FileProvider.getUriForFile(context, "com.example.scanner.fileprovider", savedfile);
            Intent fileIntent = new Intent(Intent.ACTION_SEND);
            fileIntent.setType("text/csv");
            fileIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            fileIntent.putExtra(Intent.EXTRA_STREAM, path);
            startActivity(Intent.createChooser(fileIntent, "Export data"));
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    public int isInt(String strNum) {
        int d;
        if (strNum == null) {
            return 60;
        }
        try {
            d = Integer.parseInt(strNum);
        } catch (NumberFormatException nfe) {
            return 60;
        }
        return d;
    }

    public void setGraph() {

        if (xMaxText.getText() != null) {
            String text = xMaxText.getText().toString();
            graph.getViewport().setMinX(0);
            graph.getViewport().setMaxX(isInt(text));
        }

    }

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
                            getNetwork();
                            running = true;
                            try {
                                if (System.currentTimeMillis() - blueTime > 10000) {
                                    blueList.clear();
                                    startBluetooth();
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

    public void getNetwork() {
        netList.clear();
        netTime = System.currentTimeMillis();
        if (telephonyManager != null) {
            telephonyManager.requestCellInfoUpdate(this.getMainExecutor(), cellInfoCallback);
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
        Intent intent = new Intent(this, AllActivity.class);
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
        Intent intent = new Intent(this, AllActivity.class);
        startActivity(intent);
    }


}
