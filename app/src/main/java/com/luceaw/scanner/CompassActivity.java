package com.luceaw.scanner;

import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.telephony.CellInfo;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.BarGraphSeries;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.PointsGraphSeries;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

    /*
    Compass and low-pass filter method broadly from:
    https://github.com/iutinvg/compass/blob/master/LICENSE.txt
     */

public class CompassActivity extends AppCompatActivity implements SensorEventListener {

    // Hardcoded time to turn the screen off when the user sets the screen to stay on //

    public long failSafeTime = 600000; // Time in ms equivalent to 10 minutes

    // Just as a fail-safe //

    private ArrayList<Long> blueList = new ArrayList<>();
    long blueTime;
    long netTime;
    long wifiTime;
    private StringBuilder combinedData = new StringBuilder();
    private ArrayList<Long> wifiList = new ArrayList<>();
    private WifiManager wifiManager;
    private BluetoothAdapter bluetoothAdapter;
    private TelephonyManager telephonyManager;

    private PointsGraphSeries<DataPoint> blueSeries;
    private PointsGraphSeries<DataPoint> netSeries;
    private PointsGraphSeries<DataPoint> wifiSeries;
    private BarGraphSeries<DataPoint> rotationSeries;

    private ArrayList netList = new ArrayList<>();
    private Timer timer = new Timer();
    private boolean running = false;

    private ArrayList<Float> blueDegree = new ArrayList<>();
    private ArrayList<Long> blueValue = new ArrayList<>();
    private ArrayList<Float> netDegree = new ArrayList<>();
    private ArrayList<Long> netValue = new ArrayList<>();
    private ArrayList<Float> wifiDegree = new ArrayList<>();
    private ArrayList<Long> wifiValue = new ArrayList<>();
    private float currentAzimuth = (float) 0;
    private float fixedAzimuth = (float) 0;

    private TextView compassText;
    private ImageView compassImage;

    private long startTime;

    // Broadcast receiver for Bluetooth Scan
    BroadcastReceiver bluereceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            blueTime = System.currentTimeMillis();
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                long rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
                String name = intent.getStringExtra(BluetoothDevice.EXTRA_NAME);

                // Add single reading to combined data list
                updateCombinedData(0, name, Long.toString(rssi), "N/A");

                // Add sum reading to combined data list
                blueList.add(rssi);
                double[] sums = new scannerAppTools().getMw(blueList);
                int sumdBm = (int) Math.round(sums[0]);
                updateCombinedData(0, "Sum_Bluetooth_Signal", Integer.toString(sumdBm), "N/A");

                // Add data to GraphView
                blueDegree.add(fixedAzimuth);
                blueValue.add(rssi);
                appendData(blueDegree, blueValue, blueSeries);

            }
        }
    };

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

                // Check is result is recent
                time = (long) times.get(0);
                if (time < 101) {

                    double[] sums = new scannerAppTools().getMw(netList);
                    int sumdBm = (int) Math.round(sums[0]);

                    // Add single reading to combined data list
                    if (netList.size() == names.size()) {
                        for (int i = 0; i < netList.size(); i++) {
                            updateCombinedData(1, names.get(i).toString(), (netList.get(i)).toString(), "N/A");
                            Log.v("Current Azimuth", currentAzimuth + "");
                            netDegree.add(fixedAzimuth);
                            netValue.add((long) netList.get(i));
                        }
                    }

                    // Add sum reading to combined data list
                    updateCombinedData(1, "Sum_Cell_Signal", Integer.toString(sumdBm), Double.toString(sums[1]));
                    appendData(netDegree, netValue, netSeries);

                }
            }
        }
    };

    // Broadcast receiver for Wifi scan
    BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            unregisterReceiver(wifiReceiver);
            List<ScanResult> results = wifiManager.getScanResults();

            wifiList.clear();
            wifiTime = System.currentTimeMillis();
            Log.v("Actual size: ", results.size() + "");

            // Get list of dBms (add to wifiList) and update combined data with single readings
            for (ScanResult scanResult : results) {
                long dBm = scanResult.level;
                String name = scanResult.SSID;
                wifiList.add(dBm);
                updateCombinedData(2, name, Long.toString(dBm), "N/A");

                // Add data to GraphView
                wifiDegree.add(fixedAzimuth);
                wifiValue.add(dBm);
                appendData(wifiDegree, wifiValue, wifiSeries);
            }

            if (wifiList.size() > 0) {
                double[] sums = new scannerAppTools().getMw(wifiList);
                int sumdBm = (int) Math.round(sums[0]);

                // Add sum reading to combined data list
                updateCombinedData(2, "Sum_Wifi_Signal", Integer.toString(sumdBm), Double.toString(sums[1]));

            }
            startWifi();
        }
    };
    private boolean blueStartOn;
    private Switch screenSwitch;
    private boolean wifistartOn;
    private long onTime = Long.MAX_VALUE;
    private boolean debug = false;
    private boolean hasPermission = true;

    private SensorManager sensorManager;

    private float[] mGravity = new float[3];
    private float[] mGeomagnetic = new float[3];
    private float[] Rotation = new float[9];
    private float[] I = new float[9];


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        running = false;
        startTime = System.currentTimeMillis();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compass);

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

        // Make graph
        GraphView graph = findViewById(R.id.graph);
        blueSeries = new PointsGraphSeries<>(new DataPoint[]{new DataPoint(-1, -121), new DataPoint(-0, -121)});
        netSeries = new PointsGraphSeries<>(new DataPoint[]{new DataPoint(-1, -121), new DataPoint(-0, -121)});
        wifiSeries = new PointsGraphSeries<>(new DataPoint[]{new DataPoint(-1, -121), new DataPoint(-0, -121)});
        rotationSeries = new BarGraphSeries<>(new DataPoint[]{new DataPoint(-1, -20)});

        blueSeries.setColor(Color.BLUE);
        netSeries.setColor(Color.rgb(181, 0, 147));
        wifiSeries.setColor(Color.YELLOW);
        rotationSeries.setColor(Color.GREEN);

        float height = (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 19, getResources().getDisplayMetrics()));
        float size = (height) / 10;

        blueSeries.setSize(size);
        netSeries.setSize(size);
        wifiSeries.setSize(size);
        rotationSeries.setDataWidth(size / 3);

        graph.addSeries(blueSeries);
        graph.addSeries(netSeries);
        graph.addSeries(wifiSeries);
        graph.addSeries(rotationSeries);

        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(360);

        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinY(-120);
        graph.getViewport().setMaxY(-20);

        graph.getGridLabelRenderer().setHorizontalAxisTitle("Phone Orientation (Degrees)");
        graph.getGridLabelRenderer().setVerticalAxisTitle("dBm (C/W) / RSSI (B)");

        graph.getGridLabelRenderer().setVerticalAxisTitleColor(Color.GREEN);
        graph.getGridLabelRenderer().setHorizontalAxisTitleColor(Color.GREEN);

        graph.getGridLabelRenderer().setHorizontalLabelsColor(Color.WHITE);
        graph.getGridLabelRenderer().setVerticalLabelsColor(Color.WHITE);

        graph.getGridLabelRenderer().setNumVerticalLabels(10);
        graph.getGridLabelRenderer().setNumHorizontalLabels(10);
        graph.getGridLabelRenderer().setGridColor(Color.WHITE);
        graph.getGridLabelRenderer().setHorizontalLabelsAngle(135);

        graph.getGridLabelRenderer().setHorizontalAxisTitleTextSize(height);
        graph.getGridLabelRenderer().setVerticalAxisTitleTextSize(height);

        graph.getGridLabelRenderer().reloadStyles();

        screenSwitch = findViewById(R.id.screenSwitch);

        combinedData.append("Time (s),Degrees,Signal Type,Name,Signal (dBm),Sum Signal (nW)");

        sensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);


        // Check permissions
        List<String> permissionsNeeded = new MainActivity().permissionsNeeded(this);
        if (new MainActivity().missingPermissions(permissionsNeeded, this)) {
            hasPermission = false;
        }

        // Compass sensors
        startSensors();
        compassText = findViewById(R.id.compassText);
        compassImage = findViewById(R.id.compassImage);

    }

    public void startSensors() {
        if (sensorManager != null) {
            Sensor gsensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            Sensor msensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

            sensorManager.registerListener(this, gsensor, SensorManager.SENSOR_DELAY_GAME);
            sensorManager.registerListener(this, msensor, SensorManager.SENSOR_DELAY_GAME);
        }
    }


    // Button to keep screen on
    public void screenOn(View view) {
        if (screenSwitch != null) {
            if (screenSwitch.isChecked()) {
                if (System.currentTimeMillis() - onTime > failSafeTime && !debug) {
                    view.setKeepScreenOn(false);
                    screenSwitch.setChecked(false);
                } else {
                    onTime = System.currentTimeMillis();
                    view.setKeepScreenOn(true);
                }
            } else {
                view.setKeepScreenOn(false);
            }
        }
    }

    // Export data button
    public void exportData(View view) {
        try {
            // Append date-time to default file name
            SimpleDateFormat formatter = new SimpleDateFormat("ddMMMyyyy_HH:mm:ss", Locale.getDefault());
            String date = formatter.format(new Date());
            String filename = getExternalFilesDir(null) + "/Networking_Wireless_Signal_Exposure_and_Display_Temp_File" + ".csv";
            Log.v("Networking_Wireless_Signal_Exposure_and_Display_Deleter: todelete", new File(filename).getPath());

            String title = "Inaccurate_Signal_Data_" + date + ".csv";

            // Write to root
            FileOutputStream out = new FileOutputStream(new File(filename));
            out.write(combinedData.toString().getBytes());
            out.close();

            // Export the file
            Context context = getApplicationContext();
            Uri path = FileProvider.getUriForFile(context, "com.luceaw.scanner.fileprovider", new File(filename));

            Intent fileIntent = new Intent(Intent.ACTION_SEND);
            fileIntent.setType("text/csv");
            fileIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            fileIntent.putExtra(Intent.EXTRA_STREAM, path);
            fileIntent.putExtra(Intent.EXTRA_TITLE, title);
            fileIntent.putExtra(Intent.EXTRA_SUBJECT, title);

            // Set receiver for action chosen
            Intent receiver = new Intent(context, MyReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, receiver, PendingIntent.FLAG_UPDATE_CURRENT);
            startActivity(Intent.createChooser(fileIntent, "Export data", pendingIntent.getIntentSender()));

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    // Scan all button
    public void scanAll(final View view) {

        // If app has permissions and start button already hasn't been pressed
        if (hasPermission) {
            if (!running) {
                startTime = System.currentTimeMillis();
                startBluetooth();
                startWifi();
                TimerTask timerTask = new TimerTask() {
                    @Override
                    public void run() {
                        runOnUiThread(() -> {
                            getNetwork();
                            startBluetooth(); // No need to wait for bluetooth to turn on just call
                            running = true;
                            if (System.currentTimeMillis() - blueTime > 10000) {
                                blueList.clear();
                            }

                            // Screen on fail-safe
                            if (System.currentTimeMillis() - onTime > failSafeTime && !debug) {
                                try {
                                    if (getWindow().peekDecorView() != null) {
                                        Toast.makeText(view.getContext(), "Turning screen off for fail-safe", Toast.LENGTH_SHORT).show();
                                        View thisview = findViewById(R.id.screenSwitch);
                                        screenOn(thisview);
                                        onTime = Long.MAX_VALUE;
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
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

    private long enabling = 0;

    // Bluetooth results method
    public void startBluetooth() {
        if (bluetoothAdapter != null && (System.currentTimeMillis() - enabling) > 2000) {
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
    }

    // Network results method
    public void getNetwork() {
        netList.clear();
        netTime = System.currentTimeMillis();
        if (telephonyManager != null) {
            telephonyManager.requestCellInfoUpdate(this.getMainExecutor(), cellInfoCallback);
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
            try {
                wifiManager.startScan();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // Return to original states if activity change
    public void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);

        DataPoint[] startAgain = new DataPoint[]{new DataPoint(-1, -121), new DataPoint(-0, -121)};
        blueSeries.resetData(startAgain);
        netSeries.resetData(startAgain);
        wifiSeries.resetData(startAgain);

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

    public void onStop() {
        super.onStop();
        sensorManager.unregisterListener(this);
    }

    public void onResume() {
        super.onResume();
        startSensors();
        running = false;
        DataPoint[] startAgain = new DataPoint[]{new DataPoint(-1, -121), new DataPoint(-0, -121)};
        blueSeries.resetData(startAgain);
        netSeries.resetData(startAgain);
        wifiSeries.resetData(startAgain);
        timer = new Timer();
    }

    // Buttons
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

    public void updateCombinedData(int Type, String name, String dBm, String nW) {
        int timeVal = Math.round(System.currentTimeMillis() - startTime) / 1000;
        combinedData
                .append("\n")
                .append(timeVal).append(",")
                .append(fixedAzimuth).append(",");

        if (Type == 0) { // Bluetooth
            combinedData.append("Bluetooth").append(",");
        } else if (Type == 1) { // Cell
            combinedData.append("Cell").append(",");
        } else if (Type == 2) { // Wifi
            combinedData.append("Wifi").append(",");
        }

        combinedData
                .append(name).append(",")
                .append(dBm).append(",")
                .append(nW);
    }

    public void appendData(List<Float> Degree, List<Long> Value, PointsGraphSeries<DataPoint> Series) {
        // #1 Make a temporary merged list
        List<List<Float>> mergedList = new ArrayList<>();
        if (Degree.size() == Value.size()) {
            for (int i = 0; i < Value.size(); i++) {
                List<Float> tempList = new ArrayList<>();
                tempList.add(Degree.get(i));
                tempList.add((float) Value.get(i));
                mergedList.add(tempList);
            }
        }

        // #2 sort merged list by column 0
        mergedList.sort((l1, l2) -> l1.get(0).compareTo(l2.get(0)));

        // #3 clear graph series and append sorted data to it
        Series.resetData(new DataPoint[]{});
        for (int i = 0; i < mergedList.size(); i++) {
            List tempList = mergedList.get(i);
            Float x = (Float) tempList.get(0);
            Float y = (Float) tempList.get(1);
            Series.appendData(new DataPoint(x, y), false, 1000);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        final float alpha = 0.97f;

        synchronized (this) {
            if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

                mGravity[0] = alpha * mGravity[0] + (1 - alpha)
                        * sensorEvent.values[0];
                mGravity[1] = alpha * mGravity[1] + (1 - alpha)
                        * sensorEvent.values[1];
                mGravity[2] = alpha * mGravity[2] + (1 - alpha)
                        * sensorEvent.values[2];

            }

            if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {

                mGeomagnetic[0] = alpha * mGeomagnetic[0] + (1 - alpha)
                        * sensorEvent.values[0];
                mGeomagnetic[1] = alpha * mGeomagnetic[1] + (1 - alpha)
                        * sensorEvent.values[1];
                mGeomagnetic[2] = alpha * mGeomagnetic[2] + (1 - alpha)
                        * sensorEvent.values[2];

            }

            boolean success = SensorManager.getRotationMatrix(Rotation, I, mGravity,
                    mGeomagnetic);
            if (success) {
                float[] orientation = new float[3];
                SensorManager.getOrientation(Rotation, orientation);
                float azimuth = (float) Math.toDegrees(orientation[0]);
                fixedAzimuth = (Math.round((azimuth + 360) % 360));
                String azimuthText = " " + (int) fixedAzimuth + " °";
                if (compassText != null) {
                    try {
                        compassText.setText(azimuthText);
                        rotationSeries.resetData(new DataPoint[]{new DataPoint(fixedAzimuth, -121)});
                        Animation an = new RotateAnimation(-currentAzimuth, -azimuth,
                                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                                0.5f);
                        currentAzimuth = azimuth;
                        an.setDuration(250);
                        an.setRepeatCount(0);
                        an.setFillAfter(true);

                        compassImage.startAnimation(an);

                    } catch (Exception e) {
                        Log.v("Compass Activity", e + "");
                    }
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
