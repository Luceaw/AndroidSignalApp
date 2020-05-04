package com.example.scanner;

import android.annotation.SuppressLint;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

public class splash_activity extends AppCompatActivity {

    private final Handler mHideHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try
        {
            this.getSupportActionBar().hide();
        }
        catch (NullPointerException e){
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
    }

    public void goHome(View view){
        Intent intent = new Intent (this, MainActivity.class);
        startActivity(intent);
    }

}
