package com.letrans.weather.leweather.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.letrans.weather.leweather.R;

public class MainActivity extends AppCompatActivity {

    public static final int FORECAST_DAY = 5;
    public static final String SETTINGS_SP = "settings_sp";
    public static final String SETTINGS_AUTO_REFRESH_ENABLE = "settings_auto_enable";
    public static final String SETTINGS_AUTO_REFRESH = "settings_auto_refresh";
    public static final String SETTINGS_WIFI_ONLY = "settings_wifi_only";
    public static final String SETTINGS_TEMPERATURE_TYPE = "settings_temperature_type";
    public static final String SETTINGS_NOTIFICATION = "settings_notification";
    public static final int SETTINGS_AUTO_REFRESH_INVALID = -1;
    public static final int SETTINGS_AUTO_REFRESH_6H = 6;
    public static final int SETTINGS_AUTO_REFRESH_12H = 12;
    public static final int SETTINGS_AUTO_REFRESH_24H = 24;

    public static final long TIME_6H = 6 * 60 * 60 * 1000L;
    public static final long TIME_12H = 12 * 60 * 60 * 1000L;
    public static final long TIME_24H = 24 * 60 * 60 * 1000L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //判断是否具有权限
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED) {
            //判断是否需要向用户解释为什么需要申请该权限
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_NETWORK_STATE)) {
            }
            //请求权限
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_NETWORK_STATE},
                    110);
        }

        Intent intent = new Intent(MainActivity.this, CityManager.class);
        startActivity(intent);
        finish();
    }
}
