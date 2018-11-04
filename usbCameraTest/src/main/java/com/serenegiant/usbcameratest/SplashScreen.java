package com.serenegiant.usbcameratest;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.widget.Toast;

import java.util.Locale;

public class SplashScreen extends AppCompatActivity {

    private static int SPLASH_TIME_OUT = 1000;
    int PERMISSION_ALL = 1;
    String[] PERMISSIONS = {
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    Activity activity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
        activity=this;

        new Handler().postDelayed(new Runnable() {

            /*
             * Showing splash screen with a timer. This will be useful when you
             * want to show case your app logo / company
             */

            @Override
            public void run() {
                // This method will be executed once the timer is over
                // Start your app main activity
                if(hasPermissions(activity,PERMISSIONS))
                {
                    proceedAfterPermission();
                }
                else {
                    ActivityCompat.requestPermissions(activity, PERMISSIONS, PERMISSION_ALL);
                }

            }
        }, SPLASH_TIME_OUT);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(grantResults[0]==PackageManager.PERMISSION_GRANTED && grantResults[1]==PackageManager.PERMISSION_GRANTED)
        {
            proceedAfterPermission();
        }
        else {
            Toast.makeText(activity, "you need these permissions to proceed further..", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void proceedAfterPermission()
    {
        String language= SharedPreferenceHelper.getSharedPreferenceString(SplashScreen.this , "language" , "en");
        setLocale(language);
        if (isLoggedIn()) {
            Intent i = new Intent(SplashScreen.this, MainActivity.class);
            startActivity(i);
        } else {
            Intent i = new Intent(SplashScreen.this, SigninActivity.class);
            startActivity(i);

        }

        // close this activity
        finish();
    }

    private boolean isLoggedIn() {
        boolean b = SharedPreferenceHelper.getSharedPreferenceBoolean(SplashScreen.this, "isLoggedIn", false);
        return b;
    }
    public void setLocale(String lang) {

        Locale myLocale = new Locale(lang);
        Resources res = getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        Configuration conf = res.getConfiguration();
        conf.locale = myLocale;
        res.updateConfiguration(conf, dm);
    }

    public  boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }






}
