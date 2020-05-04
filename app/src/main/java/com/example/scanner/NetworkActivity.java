package com.example.scanner;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

public class NetworkActivity extends AppCompatActivity {

    private ArrayList<List> arrayList = new ArrayList<>();
    private ArrayList<Long> valList = new ArrayList<>();
    private ArrayAdapter adapter;
    long startime;
    private static DecimalFormat df = new DecimalFormat("0.00");
    private Timer timer = new Timer();
    private TimerTask timerTask;
    private boolean running = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try
        {
            this.getSupportActionBar().hide();
        }
        catch (NullPointerException e){
        }
        running = false;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_network);

        ListView listView = findViewById(R.id.networkList);
        adapter = new ArrayAdapter<>(this, R.layout.simple_list_item_1, arrayList);
        listView.setAdapter(adapter);
    }

    public void scanNetworks(View view) {
        running = false;
        if (!running) {
            timerTask = new TimerTask() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // Check if Data is Enabled
                            // Check if null
                            try {
                                if (getResults() == 1) {
                                    running = true;
                                } else {
                                    //didn't work'
                                }

                            } finally {

                            }
                        }
                    });
                }
            };
            timer.schedule(timerTask, 0, 500);
        }
    }

    @SuppressLint("MissingPermission")
    public int getResults() {

        arrayList.clear();
        valList.clear();
        startime = System.currentTimeMillis();
        TelephonyManager telephonyManager = (TelephonyManager) getApplicationContext()
                .getSystemService(Context.TELEPHONY_SERVICE);

        ArrayList<Long> times = new ArrayList<>();
    if(telephonyManager !=null){
    for (final CellInfo cellInfo : telephonyManager.getAllCellInfo()) {
        if(cellInfo !=null) {

            if(telephonyManager.getSignalStrength()!=null){
                Log.v("dbm", telephonyManager.getSignalStrength().getGsmSignalStrength() + "");
            }

            long dBm = -250;
            String name = "";

            if (cellInfo instanceof CellInfoCdma) {
                dBm = ((CellInfoCdma) cellInfo).getCellSignalStrength().getDbm();
                name = "CDMA / 3G / GPS";
            } else{
                if (cellInfo instanceof CellInfoGsm) {
                    dBm = ((CellInfoGsm) cellInfo).getCellSignalStrength().getDbm();
                    name = "2G";
                } else{
                    if (cellInfo instanceof CellInfoLte) {
                        dBm = ((CellInfoLte) cellInfo).getCellSignalStrength().getDbm();
                        name = "LTE / 4G";
                    } else{
                                if (cellInfo instanceof CellInfoWcdma) {
                                    dBm = ((CellInfoWcdma) cellInfo).getCellSignalStrength().getDbm();
                                    name = "W-CDMA / 3G";
                                } else{
                                    dBm = -getValue(cellInfo.toString(), "CellSignalStrength", "level");
                                    name = cellInfo.getClass().getSimpleName();
                                    Toast.makeText(this,"Unrecognised cell info!",Toast.LENGTH_SHORT).show();
                                }
                            }
                }
            }

            if(dBm > -250) {

                long millisecondsSinceEvent = (SystemClock.elapsedRealtimeNanos() - cellInfo.getTimeStamp()) / 1000000L;
                long timeOfEvent = System.currentTimeMillis() - millisecondsSinceEvent;
                times.add(System.currentTimeMillis() - timeOfEvent);
                String status;

                if (cellInfo.isRegistered()) {
                    status = " -- In use -- ";
                } else {
                    status = "";
                }

                List results = Arrays.asList(name, dBm, status);
                arrayList.add(results);
                valList.add(dBm);
                adapter.notifyDataSetChanged();
            }
              /*
                ((CellInfoLte) cellInfo).getCellSignalStrength().getDbm();
                Long dBm = -getValue(cellInfo.toString(), "CellSignalStrength", "level");
                Long rsrp = -getValue(cellInfo.toString(), " rsrp=-", "level");
                String name = cellInfo.getClass().getSimpleName();
                String service = getService(name);

                long millisecondsSinceEvent = (SystemClock.elapsedRealtimeNanos() - cellInfo.getTimeStamp()) / 1000000L;
                long timeOfEvent = System.currentTimeMillis() - millisecondsSinceEvent;
                times.add(System.currentTimeMillis() - timeOfEvent);
                String status = "";

                if (cellInfo.isRegistered()) {
                    status = " -- In use -- ";
                } else {
                    status = "";
                }

                List results = Arrays.asList(service, dBm, status);
                arrayList.add(results);
                valList.add(dBm);
                adapter.notifyDataSetChanged();

               */
        }

    }
}


        long totaltimedif = 0;
        long value;

        if(times.size() >0) {
            for (int i = 0; i < times.size(); i++) {
                value = times.get(i);
                totaltimedif += value;
            }

            TextView textView = findViewById(R.id.timeText);
            String time = ("" + times.get(0) + " ms");
            textView.setText(time);


            // I miss python, R
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
            return 1;
        } else{
            return 0;
        }
    }


    public void goHome(View view){
        Intent intent = new Intent (this, MainActivity.class);
        startActivity(intent);
        timer.cancel();
    }
    public void goAll(View view){
        Intent intent = new Intent (this, allActivityGraphs.class);
        startActivity(intent);
        timer.cancel();
    }
    public void goBluetooth(View view){
        Intent intent = new Intent (this, BluetoothActivity.class);
        startActivity(intent);
        timer.cancel();
    }
    public void goNetwork(View view){
        Intent intent = new Intent (this, NetworkActivity.class);
        startActivity(intent);
        timer.cancel();
    }
    public void goWifi(View view){
        Intent intent = new Intent (this, WifiActivity.class);
        startActivity(intent);
        timer.cancel();
    }

    private long getValue(String fullS, String startS, String stopS)
    {
        int index = fullS.indexOf(startS) + (startS).length();
        int endIndex = fullS.indexOf(stopS,index);

        String segment = fullS.substring(index,endIndex).trim();

        return new Scanner(segment).useDelimiter("\\D+").nextLong();
    }

    private String getService(String name)
    {
        return name.substring(8).trim();
    }


}
