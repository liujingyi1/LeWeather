package com.letrans.weather.leweather;

import android.app.Application;
import android.util.Log;

import com.letrans.weather.leweather.utils.WeatherDataUtil;
import com.letrans.weather.leweather.work.WeatherModel;

public class WeatherApplication extends Application {
    private static final String TAG = "Gweather.WeatherApplication";
    public static WeatherModel mModel = null;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");

        mModel = WeatherModel.getInstance(getApplicationContext());
        mModel.init();

        int defState = WeatherDataUtil.getInstance().getDefaultState(this);

        if (defState == WeatherDataUtil.DEFAULT_STATE_NEED_CHECK) {
            String defwoeid = getResources().getString(R.string.default_woeid);
            Log.d(TAG, "defwoeid="+defwoeid);
            if (defwoeid.isEmpty()) {
                WeatherDataUtil.getInstance().setDefaultState(this, WeatherDataUtil.DEFAULT_STATE_FINISHED);
            } else {
                mModel.addDefaultData();
            }
        }
    }

}
