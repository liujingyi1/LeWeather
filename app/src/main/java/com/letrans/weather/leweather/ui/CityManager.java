package com.letrans.weather.leweather.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

import com.letrans.weather.leweather.R;
import com.letrans.weather.leweather.WeatherApplication;
import com.letrans.weather.leweather.work.WeatherAction;
import com.letrans.weather.leweather.work.WeatherInfo;
import com.letrans.weather.leweather.work.WeatherModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class CityManager extends AppCompatActivity {
    @BindView(R.id.city_list)
    RecyclerView cityList;
    @BindView(R.id.swipe_refresh)
    SwipeRefreshLayout swipeRefreshLayout;

    CityListAdapter cityListAdapter;

    public static final int REQUEST_CODE_CITY_ADD = 1001;
    public static final int REQUEST_CODE_PERMISSION = 101;
    public static final int CITY_COUNT_MAX = 10;

    private static final String FRAGMENT_TAG_DELETE_CITY = "delete_city";
    private static final String FRAGMENT_TAG_LOADING = "loading";
    private static final String FRAGMENT_TAG_LOCATION_FIND = "location_find";

    private List<WeatherInfo> mWeatherInfoList;
    private static final int REFRESH_ALL_WEATHER = 110;
    BroadcastReceiver broadcastReceiver;

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case REFRESH_ALL_WEATHER: {
                    WeatherModel.getInstance(getApplicationContext()).refreshAllWeather();
                    break;
                }
            }
        }
    };

    Timer timer = new Timer();
    TimerTask timerTask = new TimerTask() {
        @Override
        public void run() {
            handler.sendEmptyMessage(REFRESH_ALL_WEATHER);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_city_manager);

        ButterKnife.bind(this);

        cityList.setLayoutManager(new LinearLayoutManager(this));

        getWeatherInfo();

        cityListAdapter = new CityListAdapter(mWeatherInfoList, this);
        cityListAdapter.setOnDeleteListener(new OnDeleteListener() {
            @Override
            public void onDelete(int position) {
                showDeleteDialog(position);
            }
        });
        cityListAdapter.setOnClickItemListener(new OnClickItemListener() {
            @Override
            public void onClick(int position) {

                if ((mWeatherInfoList.size() > position+1) && mWeatherInfoList.get(position+1).isForecast) {
                    return;
                }
                Log.i("jingyi", "11 position="+position);

                int openedItem = -1;
                for (int i = 0; i < mWeatherInfoList.size(); i++) {
                    if (mWeatherInfoList.get(i).isForecast) {
                        openedItem = i;
                        break;
                    }
                }

                Log.i("jingyi", "22 openedItem="+openedItem);

                if (openedItem >= 0) {
                    cityListAdapter.removeItem(openedItem);
                    if (position > openedItem) {
                        position -= 1;
                    }
                }

                Log.i("jingyi", "33 position="+position);

                WeatherInfo weatherInfo = new WeatherInfo();
                weatherInfo.copyInfo(mWeatherInfoList.get(position));
                weatherInfo.isForecast = true;
                cityListAdapter.addItem(position+1, weatherInfo);
            }
        });
        cityList.setAdapter(cityListAdapter);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WeatherAction.ACTION_ADD_WEATHER_FINISH);
        intentFilter.addAction(WeatherAction.ACTION_WEATHER_REFRESHED_ALL);
        intentFilter.addAction(WeatherAction.ACTION_QUERT_GPS_WEATHER_FINISH);
        intentFilter.addAction(WeatherAction.ACTION_REFRESH_COMPLETE);

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.i("jingyi", "onReceive intent="+intent.getAction());
                if (intent.getAction().equals(WeatherAction.ACTION_WEATHER_REFRESHED_ALL)) {
                    cityListAdapter.notifyDataSetChanged();
                    if (swipeRefreshLayout != null) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                }
            }
        };
        registerReceiver(broadcastReceiver, intentFilter);
        timer.schedule(timerTask, 500, 1000*60*30);

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                handler.sendEmptyMessage(REFRESH_ALL_WEATHER);
            }
        });
    }

    @OnClick({R.id.setting, R.id.refresh, R.id.backbutton})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.setting: {
                Intent intent = new Intent(CityManager.this, AddCityActivity.class);
                startActivityForResult(intent, REQUEST_CODE_CITY_ADD);
                break;
            }
            case R.id.refresh: {
                handler.sendEmptyMessage(REFRESH_ALL_WEATHER);
                break;
            }
            case R.id.backbutton: {
                onBackPressed();
                break;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (resultCode) {
            case REQUEST_CODE_CITY_ADD: {
                refreshCityList();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timer.cancel();
        unregisterReceiver(broadcastReceiver);
    }

    private void refreshCityList() {
//        mWeatherInfoList = WeatherApplication.mModel.getWeatherInfos();
        cityListAdapter.notifyDataSetChanged();
    }

    private void getWeatherInfo() {
        mWeatherInfoList = WeatherApplication.mModel.getWeatherInfos();
    }

    private void showDeleteDialog(final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(CityManager.this);
        builder.setTitle("Delete?");
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if ((mWeatherInfoList.size() > position+1) && mWeatherInfoList.get(position+1).isForecast) {
                    cityListAdapter.removeItem(position+1);
                }
                deleteWeather(position);
            }
        }).setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        }).show();

    }

    private void deleteWeather(int position) {
        WeatherModel.getInstance(getApplicationContext()).deleteWeatherInfo(mWeatherInfoList.get(position));
        cityListAdapter.notifyDataSetChanged();
    }

}
