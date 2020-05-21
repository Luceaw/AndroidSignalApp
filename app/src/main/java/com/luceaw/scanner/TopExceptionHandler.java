package com.luceaw.scanner;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

// From https://stackoverflow.com/questions/3643395/how-to-get-android-crash-logs !

public class TopExceptionHandler implements Thread.UncaughtExceptionHandler {
    private Thread.UncaughtExceptionHandler defaultUEH;
    private Activity app;

    TopExceptionHandler(Activity app) {
        // Use default UEH
        this.defaultUEH = Thread.getDefaultUncaughtExceptionHandler();
        this.app = app;
    }

    public void uncaughtException(Thread t, Throwable e) {
        Log.v("Scan_App_Crash_Reporter", "App Crashed");

        StackTraceElement[] arr = e.getStackTrace();
        StringBuilder report = new StringBuilder();
        report.append("-------------------------------\n\n");
        report.append("--------- Crash Report ---------\n\n");
        report.append("-------------------------------\n\n\n\n");
        report.append("--------- Error ---------\n\n");
        report.append(e.toString()).append("\n\n");
        report.append("--------- Stack trace ---------\n\n");
        for (StackTraceElement stackTraceElement : arr) {
            report.append("    ").append(stackTraceElement.toString()).append("\n");
        }
        report.append("-------------------------------\n\n");
        Log.v("Scan_App_Crash_Reporter", "Stack Trace Finished");

        // If the exception was thrown in a background thread inside
        // AsyncTask, then the actual exception can be found with getCause

        Throwable cause = e.getCause();
        if(cause != null) {
            report.append("--------- Cause ---------\n\n");
            report.append(cause.toString()).append("\n\n");
            arr = cause.getStackTrace();
            for (StackTraceElement stackTraceElement : arr) {
                report.append("    ").append(stackTraceElement.toString()).append("\n");
            }
            report.append("-------------------------------\n\n");
        }
        Log.v("Scan_App_Crash_Reporter", "Cause Finished");

        // Save the report to file and prompt for export
        try {
            Log.v("Scan_App_Crash_Reporter", "Start Writing");

            // Error report name
            SimpleDateFormat formatter = new SimpleDateFormat("ddMMMyyyy_HH:mm:ss", Locale.getDefault());
            String date = formatter.format(new Date());
            String filename = app.getExternalFilesDir(null)+"/Networking_Wireless_Signal_Exposure_and_Display_App_Error" + ".txt";
            String title = "App_Error_" + date + ".txt";


            // Save file
            FileOutputStream trace = new FileOutputStream(new File(filename));
            trace.write(report.toString().getBytes());
            trace.close();
            Log.v("Scan_App_Crash_Reporter", "Finished Writing");

            Intent emailFile = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"));

            String[] email = {"luceaw9@gmail.com"};
            String subject = "Error report";
            String body = "Please email this error report to: " + email[0] + "\n\n" + report + "\n";

            emailFile.putExtra(Intent.EXTRA_TITLE, filename);
            emailFile.putExtra(Intent.EXTRA_EMAIL  , email);
            emailFile.putExtra(Intent.EXTRA_SUBJECT, subject);
            emailFile.putExtra(Intent.EXTRA_TEXT   , body);

            try {
                app.startActivity(Intent.createChooser(emailFile, "Oh no, the app crashed. Would you like to email crash data?"));
            } catch (android.content.ActivityNotFoundException ex) {
                Log.v("Scan_App_Crash_Reporter", "No email client");
            }
        }
        catch(Exception ioe) {
           Log.v("Scan_App_Crash_Reporter", ioe.toString());
        }

        defaultUEH.uncaughtException(t, e);
    }
}
