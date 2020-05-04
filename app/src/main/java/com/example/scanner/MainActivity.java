package com.example.scanner;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Objects;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try
        {
            Objects.requireNonNull(this.getSupportActionBar()).hide();
        } catch (NullPointerException ignored) {
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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
