package com.letrans.weather.leweather.ui;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.letrans.weather.leweather.R;
import com.letrans.weather.leweather.utils.Utils;
import com.letrans.weather.leweather.utils.WeatherDataUtil;
import com.letrans.weather.leweather.work.WeatherInfo;
import com.letrans.weather.leweather.work.WeatherModel;

import java.util.List;

public class CityListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    List<WeatherInfo> list;
    Context context;
    OnDeleteListener deleteListener;
    OnClickItemListener clickListener;
    private static int IS_FORECAST = 111;
    private static int IS_NOT_FORECAST = 222;

    public CityListAdapter(List<WeatherInfo> data, Context context) {
        this.list = data;
        this.context = context;
    }

    public void setOnDeleteListener(OnDeleteListener listener) {
        this.deleteListener = listener;
    }

    public void setOnClickItemListener(OnClickItemListener listener) {
        this.clickListener = listener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder holder;
        if (IS_FORECAST == viewType) {
            holder = new ForecastHolder(LayoutInflater.from(context)
                    .inflate(R.layout.city_item_forecast_view, parent, false));
        } else {
            holder = new CityViewHolder(LayoutInflater.from(context)
                    .inflate(R.layout.city_item_view, parent, false));
        }

        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, final int position) {
        if (holder.getItemViewType() == IS_FORECAST) {
            ForecastHolder cityViewHolder = (ForecastHolder)holder;
            initForecastItemView(cityViewHolder.forecast1, list.get(position).getForecasts().get(1));
            initForecastItemView(cityViewHolder.forecast2, list.get(position).getForecasts().get(2));
            initForecastItemView(cityViewHolder.forecast3, list.get(position).getForecasts().get(3));
            initForecastItemView(cityViewHolder.forecast4, list.get(position).getForecasts().get(4));
        } else {
            CityViewHolder cityViewHolder = (CityViewHolder)holder;
            WeatherInfo info = list.get(position);
            cityViewHolder.cityName.setText(info.getName());
            cityViewHolder.timeView.setText(Utils.getTime());
            cityViewHolder.weatherName.setText(info.getCondition().getText());
            cityViewHolder.tempView.setText(info.getCondition().getTemp());
            String iconCode = info.getCondition().getIconCode();
            Log.i("jingyi", "info.getName()="+info.getName()+" position="+position);

            int code = Integer.parseInt(info.getCondition().getCode());
            int resId;
            boolean isnight = WeatherDataUtil.getInstance().isNight();
            resId = WeatherDataUtil.getInstance()
                    .getWeatherImageResourceByCode(code, isnight, WeatherDataUtil.SMALL_WHITE);
            if (WeatherDataUtil.INVALID_WEAHTER_RESOURCE == resId) {
                resId = WeatherDataUtil.getInstance()
                        .getWeatherImageResourceByText(
                                list.get(position).getCondition().getText(), isnight, WeatherDataUtil.SMALL_WHITE);
            }
            if (resId > 0) {
                cityViewHolder.weatherIcon.setImageDrawable(context.getResources().getDrawable(resId));
            } else {
                cityViewHolder.weatherIcon.setImageDrawable(context.getResources().getDrawable(R.drawable.icon_nodata_white));
            }
        }
    }

    private void initForecastItemView(LinearLayout parent,WeatherInfo.Forecast forecast) {
        TextView dateText = (TextView) parent.findViewById(R.id.forecast_date);
        ImageView iconImage = (ImageView) parent.findViewById(R.id.forecast_icon);
        TextView weatherText = (TextView) parent.findViewById(R.id.forecast_weather);

        int code = Integer.parseInt(forecast.getCode());
        int resId = WeatherDataUtil.getInstance().getWeatherImageResourceByCode(
                code, false, WeatherDataUtil.SMALL_WHITE);
        boolean isnight = WeatherDataUtil.getInstance().isNight();
        if (WeatherDataUtil.INVALID_WEAHTER_RESOURCE == resId) {
            resId = WeatherDataUtil.getInstance()
                    .getWeatherImageResourceByText(forecast.getText(), isnight,
                            WeatherDataUtil.SMALL_WHITE);
        }
        iconImage.setImageResource(resId);
        dateText.setText(forecast.getDateShort()+" "+forecast.getDay());
        weatherText.setText(forecast.getLow() + "℃" +"-"
                + forecast.getHigh() + "℃");
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    @Override
    public int getItemViewType(int position) {
        return list.get(position).isForecast ? IS_FORECAST : IS_NOT_FORECAST;
    }

    public class ForecastHolder extends RecyclerView.ViewHolder {
        LinearLayout forecast1;
        LinearLayout forecast2;
        LinearLayout forecast3;
        LinearLayout forecast4;

        public ForecastHolder(View view) {
            super(view);
             forecast1 = (LinearLayout)view.findViewById(R.id.forecast_item_1);
             forecast2 = (LinearLayout)view.findViewById(R.id.forecast_item_2);
             forecast3 = (LinearLayout)view.findViewById(R.id.forecast_item_3);
             forecast4 = (LinearLayout)view.findViewById(R.id.forecast_item_4);
        }
    }

    public class CityViewHolder extends RecyclerView.ViewHolder {
        TextView cityName;
        TextView timeView;
        TextView weatherName;
        ImageView weatherIcon;
        TextView tempView;
        View rootView;

        public  CityViewHolder(View view) {
            super(view);
            rootView = view;
            rootView.setBackgroundColor(Color.parseColor(Utils.getColor()));
            rootView.setLongClickable(true);
            rootView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    deleteListener.onDelete(getAdapterPosition());
                    return false;
                }
            });
            rootView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    clickListener.onClick(getAdapterPosition());
                }
            });
            cityName = (TextView) itemView.findViewById(R.id.city_name);
            timeView = (TextView) itemView.findViewById(R.id.timeview);
            weatherName = (TextView) itemView.findViewById(R.id.weather_name);
            tempView = (TextView) itemView.findViewById(R.id.temp_view);
            weatherIcon = (ImageView)itemView.findViewById(R.id.weather_icon);
        }
    }

    public boolean addItem(int position, WeatherInfo msg) {
        if (position <= list.size() && position >= 0) {
            list.add(position, msg);
            notifyItemInserted(position);
            return true;
        }
        return false;
    }

    public boolean removeItem(int position) {
        if (position < list.size() && position >= 0) {
            list.remove(position);
            notifyItemRemoved(position);
            return true;
        }
        return false;
    }

    public void clearAll() {
        list.clear();
        notifyDataSetChanged();
    }
}
