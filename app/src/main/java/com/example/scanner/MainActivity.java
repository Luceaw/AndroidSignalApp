package com.example.scanner;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// Home page with buttons for app activities
// Check permissions

public class MainActivity extends AppCompatActivity {

    private String[] permissions = new String[]{
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_PHONE_STATE};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            Objects.requireNonNull(this.getSupportActionBar()).hide();
        } catch (NullPointerException ignored) {
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Check for permissions
        final int MPermissions = 123;
        List<String> permissionsNeeded = new ArrayList<>();

        for (String p : permissions) {
            if (ContextCompat.checkSelfPermission(this, p)
                    != PackageManager.PERMISSION_GRANTED) { permissionsNeeded.add(p); }
        }

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
