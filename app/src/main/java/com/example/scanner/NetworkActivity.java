package com.example.scanner;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.CellInfo;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;
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
    private boolean hasPermission = true;
    private  ListView listView;


    // Cell In for Callback for retrieved results
    public TelephonyManager.CellInfoCallback cellInfoCallback = new TelephonyManager.CellInfoCallback() {
        @Override
        public void onCellInfo(@NonNull List<CellInfo> cellInfo) {
            long time;
            adapter.notifyDataSetChanged();
            adapter.notifyDataSetChanged();

            arrayList.clear();
            valList.clear();
            adapter.notifyDataSetInvalidated();
            adapter.notifyDataSetChanged();

            // Process results
            ArrayList[] dBms = new scannerAppTools().telephonyDBm(cellInfo);
            valList = dBms[0];
            ArrayList times = dBms[1];
            ArrayList names = dBms[2];
            ArrayList status = dBms[3];

            // If returned data is complete
            if (names.size() == valList.size() && valList.size() == status.size()
                    && times.size() > 0 && valList.size() > 0) {

                time = (long) times.get(1);
                String string = (time + " ms");
                timeTaken.setText(string);

                // Calculate sum values
                double[] sums = new scannerAppTools().getMw(valList);
                int dBmSum = (int) Math.round(sums[0]);
                int nsum = (int) Math.round(sums[1]);

                // Set exposure
                String exposure = ("~" + dBmSum + " dBm / " + "\n" + nsum + " ~nW");
                exposurebox.setText(exposure);

                // Set list values and update list
                for (int i = 0; i < names.size(); i++) {
                    Object name = names.get(i);
                    Object dBm = valList.get(i);
                    Object inUse = status.get(i);
                    String concat = (name + ": " + dBm + " dBm " + inUse);
                    arrayList.add(concat);
                }
                adapter.notifyDataSetChanged();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        running = false;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_network);

        try {
            Objects.requireNonNull(this.getSupportActionBar()).hide();
        } catch (NullPointerException ignored) {
        }

        arrayList.add("Press Retrieve to get results");
        listView = findViewById(R.id.networkList);
        adapter = new ArrayAdapter<>(this, R.layout.simple_list_item_1, arrayList);
        listView.setAdapter(adapter);
        listView.setEnabled(false);

        timeTaken = findViewById(R.id.timeText);
        exposurebox = findViewById(R.id.exposureBox);

        // Check permissions
        List<String> permissionsNeeded = new MainActivity().permissionsNeeded(this);
        if(new MainActivity().missingPermissions(permissionsNeeded, this)) {
            hasPermission = false;
        }

    }

    public void scanNetworks(View view) {

        // If app has permissions
        if(hasPermission) {
            // Does not actually scan so just retrieve the already-present results on a loop.
            if (!running) {
                TimerTask timerTask = new TimerTask() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                getResults();
                                running = true;
                            }
                        });
                    }
                };
                timer.schedule(timerTask, 0, 500);
            }
        } else {
            Toast.makeText(this, "Missing permissions!! Return to home to allow", Toast.LENGTH_SHORT).show();
        }
    }

    public void getResults() {
        // Request a scan from loop; rate limited so partially in vain.
        arrayList.clear();
        valList.clear();
        startime = System.currentTimeMillis();
        TelephonyManager telephonyManager = (TelephonyManager) getApplicationContext()
                .getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager != null) {
            telephonyManager.requestCellInfoUpdate(this.getMainExecutor(), cellInfoCallback);
        }
    }


    // Cancel timer if activity changes.
    @Override
    public void onPause() {
        super.onPause();
        timer.cancel();
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
