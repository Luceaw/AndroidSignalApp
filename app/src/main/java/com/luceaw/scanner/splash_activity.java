package com.luceaw.scanner;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Objects;

public class splash_activity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try
        {
            Objects.requireNonNull(this.getSupportActionBar()).hide();
        } catch (NullPointerException ignored) {
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);



    }





    public void goHome(View view){
        Intent intent = new Intent (this, MainActivity.class);
        startActivity(intent);
    }


}
