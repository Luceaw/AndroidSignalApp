package com.luceaw.scanner;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.io.File;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import static android.content.Intent.EXTRA_CHOSEN_COMPONENT;

public class MyReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String selectedAppPackage = String.valueOf(Objects.requireNonNull(intent.getExtras()).get(EXTRA_CHOSEN_COMPONENT));
        Log.v("Networking_Wireless_Signal_Exposure_and_Display",selectedAppPackage);

        String filename = "Networking_Wireless_Signal_Exposure_and_Display_Temp_File" + ".csv";
        File deleteFile = new File(context.getExternalFilesDir(null), filename);
        Log.v("Networking_Wireless_Signal_Exposure_and_Display",deleteFile+"");

        TimerTask deleteTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    if (deleteFile.exists()) {
                        if (deleteFile.delete()) {
                            Log.v("Networking_Wireless_Signal_Exposure_and_Display_Deleter: Successful", deleteFile.getPath());
                        } else {
                            Log.v("Networking_Wireless_Signal_Exposure_and_Display_Deleter: Not Successful", deleteFile.getPath());
                        }
                    } else {
                        Log.v("Networking_Wireless_Signal_Exposure_and_Display_Deleter: None existent", deleteFile.getPath());
                    }
                } catch (Exception e) {
                    Log.v("Networking_Wireless_Signal_Exposure_and_Display_Deleter: Exception", deleteFile.getPath());
                    e.printStackTrace();
                }
            }
        };
        Timer timerObj = new Timer();
        timerObj.schedule(deleteTask, 30000);
    }


}