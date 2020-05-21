package com.luceaw.scanner;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// Home page with buttons for app activities and also checks permissions

public class MainActivity extends AppCompatActivity {

    public String[] appPermissions = new String[]{
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_PHONE_STATE};


    // Check for permissions
    public List<String> permissionsNeeded(Context context){
        List<String> permissionsNeeded = new ArrayList<>();
        for (String p : appPermissions) {
            if (ContextCompat.checkSelfPermission(context, p)
                    != PackageManager.PERMISSION_GRANTED) { permissionsNeeded.add(p); }
        }
        return permissionsNeeded;
    }

    // Make toasts for missing permissions

    public boolean missingPermissions(List<String> permissionsNeeded, Context context){
        if(!permissionsNeeded.isEmpty()) {
            for(String permission : permissionsNeeded) {
                String message = "Missing Permission: " + permission + " App can't work!!";
                Toast toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
                View view = toast.getView();
                view.getBackground().setColorFilter(Color.YELLOW, PorterDuff.Mode.SRC_IN);
                toast.show();
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            Objects.requireNonNull(this.getSupportActionBar()).hide();
        } catch (NullPointerException ignored) {
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Thread.setDefaultUncaughtExceptionHandler(new TopExceptionHandler(this));


        final int MPermissions = 123;
        List<String> permissionsNeeded = permissionsNeeded(this);
        if(!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionsNeeded.toArray(new String[0]), MPermissions);
        }
    }


    public void goAll(View view){
        Intent intent = new Intent (this, allActivityGraphs.class);
        startActivity(intent);
    }
    public void goBluetooth(View view){
        Intent intent = new Intent (this, BluetoothActivity.class);
        startActivity(intent);
    }
    public void goNetwork(View view){
        Intent intent = new Intent (this, NetworkActivity.class);
        startActivity(intent);
    }
    public void goWifi(View view){
        Intent intent = new Intent (this, WifiActivity.class);
        startActivity(intent);
    }

}
