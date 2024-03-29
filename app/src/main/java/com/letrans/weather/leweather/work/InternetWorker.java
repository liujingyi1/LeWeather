package com.letrans.weather.leweather.work;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;

import com.letrans.weather.leweather.WeatherApplication;
import com.letrans.weather.leweather.ui.MainActivity;
import com.letrans.weather.leweather.utils.CityNameXMLParser;
import com.letrans.weather.leweather.utils.MultiWeatherXMLParser;
import com.letrans.weather.leweather.utils.Utils;
import com.letrans.weather.leweather.utils.WeatherXMLParser;
import com.letrans.weather.leweather.utils.WebAccessTools;

public class InternetWorker {
	private static final String TAG = "Gweather.InternetWorker";
	
	private static final String URL_QUERY_WEATHER_PART1 = "http://query.yahooapis.com/v1/public/yql?q=select+*+from+weather.forecast+where+woeid=";
	private static final String URL_QUERY_WEATHER_PART2 = "+and+u='c'";

	private static final String URL_QUERY_MULTI_WEATHER_PART1 = "http://query.yahooapis.com/v1/public/yql?q=select+*+from+weather.forecast+where+woeid+in+(";
	private static final String URL_QUERY_MULTI_WEATHER_PART2 = ")+and+u='c'";
	
	private static final String URL_QUERY_CITY_PART1 = "http://query.yahooapis.com/v1/public/yql?q=select+*+from+geo.places+where+text='";
	private static final String URL_QUERY_CITY_PART2 = "*'+and+lang='zh-CN'";

	private static final String URL_QUERY_LOCATION_PART1 = "https://query.yahooapis.com/v1/public/yql?q=select+*+from+geo.places+where+woeid+in+(select+place.woeid+from+flickr.places+where+api_key="
			+ Utils.KEY_PUBLIC + "and+lat='";
	private static final String URL_QUERY_LOCATION_PART2 = "'+and+lon='";
	private static final String URL_QUERY_LOCATION_PART3 = "')+and+lang='zh-CN'";
	
	enum State {
		IDLE, WORK_WEATHER, WORK_CITY, WORK_LOCATION
	}
	
	enum QueryWeatherType {
		CURRENT, ALL, ADD_NEW
	}
	
	private CallBacks mCallBacks;
	
	public interface CallBacks {
		void queryCityFinished();
		void queryAddWeatherFinished(WeatherInfo weatherInfo);
		void queryAddGpsWeatherFinished(WeatherInfo weatherInfo);
		void queryLocationFinished();
		void refreshAllWeatherFinished();
		void refreshWeatherFinished(WeatherInfo weatherInfo);
	}
	
	public void setCallBacks(CallBacks callBacks) {
		mCallBacks = callBacks;
	}

	private static InternetWorker INSTANCE;

	private Context mContext;
	private State mState = State.IDLE;
	private QueryWeatherType mQueryWeatherType = QueryWeatherType.CURRENT;
	private List<WeatherInfo> mWeatherInfoList;
	private WeatherInfo tempWeatherInfo;

	private QueryWeatherTask mQueryWeatherTask;
	private QueryCityTask mQueryCityTask;
	private QueryLocationTask mQueryLocationTask;
	private QueryMultiWeatherTask mQueryMultiWeatherTask;
	
	private int updateWeatherCount = 0;
	private int updateFinishedWeatherCount = 0;

	private InternetWorker(Context context) {
		mContext = context;
	}
	
	public void init() {
		mWeatherInfoList = WeatherApplication.mModel.getWeatherInfos();
	}

	public static InternetWorker getInstance(Context context) {
		if (null == INSTANCE) {
			INSTANCE = new InternetWorker(context);
		}

		return INSTANCE;
	}
	
	
	
	public boolean queryDefaultWeather(WeatherInfo weatherInfo) {
		Log.v(TAG, "queryDefaultWeather, city: "+weatherInfo.getName());
		if (mState == State.IDLE) {
			mState = State.WORK_WEATHER;
			mQueryWeatherType = QueryWeatherType.CURRENT;
			mWeatherInfoList.add(weatherInfo);
			updateWeatherCount = 1;
			updateFinishedWeatherCount = 0;
			
			if (null != mQueryWeatherTask && 
					mQueryWeatherTask.getStatus() == AsyncTask.Status.RUNNING) {
				mQueryWeatherTask.cancel(true);
			}
			
			mQueryWeatherTask = new QueryWeatherTask(weatherInfo);
			mQueryWeatherTask.execute();
			return true;
		} else {
			Log.v(TAG, "Busy, mState="+mState);
			return false;
		}
	}

	public boolean updateWeather(WeatherInfo weatherInfo) {
		Log.v(TAG, "updateWeather, city: "+weatherInfo.getName());
		if (mState == State.IDLE) {
			mState = State.WORK_WEATHER;
			mQueryWeatherType = QueryWeatherType.CURRENT;
			updateWeatherCount = 1;
			updateFinishedWeatherCount = 0;
			
			if (null != mQueryWeatherTask && 
					mQueryWeatherTask.getStatus() == AsyncTask.Status.RUNNING) {
				mQueryWeatherTask.cancel(true);
			}
			
			mQueryWeatherTask = new QueryWeatherTask(weatherInfo);
			mQueryWeatherTask.execute();
			return true;
		} else {
			Log.v(TAG, "Busy, mState="+mState);
			return false;
		}
	}
	
	public boolean updateWeather() {
		Log.v(TAG, "updateWeather");
		if (mState == State.IDLE) {
			mState = State.WORK_WEATHER;
			if (mWeatherInfoList.isEmpty()) {
				mCallBacks.refreshAllWeatherFinished();
				mState = State.IDLE;
			} else {
				mQueryMultiWeatherTask = new QueryMultiWeatherTask(mWeatherInfoList);
				mQueryMultiWeatherTask.execute();
			}
			
			return true;
		} else {
			Log.v(TAG, "Busy, mState="+mState);
			return false;
		}
	}
	
	public boolean addWeatherByCity(CityInfo cityInfo, boolean isGps) {
		Log.v(TAG, "addWeatherByCity");
		if (mState == State.IDLE) {
			mState = State.WORK_WEATHER;
			mQueryWeatherType = QueryWeatherType.ADD_NEW;
			updateWeatherCount = 1;
			updateFinishedWeatherCount = 0;
			
			if (null != mQueryWeatherTask && 
					mQueryWeatherTask.getStatus() == AsyncTask.Status.RUNNING) {
				mQueryWeatherTask.cancel(true);
			}
			
			WeatherInfo info = new WeatherInfo();
			info.setWoeid(cityInfo.getWoeid());
			info.setName(cityInfo.getName());
			info.setGps(isGps);
			
			mQueryWeatherTask = new QueryWeatherTask(info);
			mQueryWeatherTask.execute();
			return true;
		} else {
			Log.v(TAG, "Busy, mState="+mState);
			return false;
		}
	}

	//Use for Add New Weather
	public boolean queryWeather(WeatherInfo info) {
		Log.v(TAG, "queryWeather");
		if (mState == State.IDLE) {
			mState = State.WORK_WEATHER;
			mQueryWeatherType = QueryWeatherType.ADD_NEW;
			updateWeatherCount = 1;
			updateFinishedWeatherCount = 0;
			
			if (null != mQueryWeatherTask && 
					mQueryWeatherTask.getStatus() == AsyncTask.Status.RUNNING) {
				mQueryWeatherTask.cancel(true);
			}
			
			mQueryWeatherTask = new QueryWeatherTask(info);
			mQueryWeatherTask.execute();
			return true;
		} else {
			Log.v(TAG, "Busy, mState="+mState);
			return false;
		}
	}
	
	class QueryWeatherTask extends AsyncTask<Void, Void, Void> {
		private WeatherInfo mWeatherInfo;

		public QueryWeatherTask(WeatherInfo weatherInfo) {
			mWeatherInfo = weatherInfo;
		}

		@Override
		protected Void doInBackground(Void... params) {
			Log.d(TAG, "QueryWeather - doing");
			String url = URL_QUERY_WEATHER_PART1 + mWeatherInfo.getWoeid()
					+ URL_QUERY_WEATHER_PART2;
			String content = new WebAccessTools(mContext).getWebContent(url);
			parseWeather(content, mWeatherInfo);
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			Log.d(TAG, "QueryWeather - DONE");
			if (tempWeatherInfo == null || tempWeatherInfo.getForecasts() == null
					|| tempWeatherInfo.getForecasts().size() < MainActivity.FORECAST_DAY) {
				Log.w(TAG, "QueryWeather Failed");
			} else {
				tempWeatherInfo.setName(mWeatherInfo.getName());
				mWeatherInfo.copyInfo(tempWeatherInfo);
			}
			
			updateFinishedWeatherCount++;
			
			Log.d(TAG, "updateFinishedWeatherCount:" + updateFinishedWeatherCount
					+ ", updateWeatherCount:" + updateWeatherCount);
			
			if (updateFinishedWeatherCount == updateWeatherCount) {
				
				if (QueryWeatherType.ALL == mQueryWeatherType) {
					Log.d(TAG, "ALL");
					mCallBacks.refreshAllWeatherFinished();
				} else if (QueryWeatherType.CURRENT == mQueryWeatherType) {
					Log.d(TAG, "CURRENT");
					mCallBacks.refreshWeatherFinished(mWeatherInfo);
				} else if (QueryWeatherType.ADD_NEW == mQueryWeatherType){
					Log.d(TAG, "ADD_NEW");
					if (mWeatherInfo.isGps()) {
						Log.d(TAG, "GPS");
						if (mCallBacks != null) {
							mCallBacks.queryAddGpsWeatherFinished(mWeatherInfo);
						}
					} else {
						Log.d(TAG, "Normal");
						if (mCallBacks != null) {
							mCallBacks.queryAddWeatherFinished(mWeatherInfo);
						}
					}
				}
				
				mState = State.IDLE;
			}
		}

	}
	
	private void parseWeather(String content, WeatherInfo mWeatherInfo) {
		if (content == null || content.isEmpty()) {
			Log.w(TAG, "parseWeather content is Empty");
			tempWeatherInfo = new WeatherInfo();
			tempWeatherInfo.getForecasts().clear();
			return;
		}
		
		tempWeatherInfo = new WeatherInfo();
		
		SAXParserFactory mSAXParserFactory = SAXParserFactory.newInstance();
		try {
			SAXParser mSAXParser = mSAXParserFactory.newSAXParser();
			XMLReader mXmlReader = mSAXParser.getXMLReader();
			WeatherXMLParser handler = new WeatherXMLParser(mContext,
					tempWeatherInfo, mWeatherInfo.getWoeid());
			mXmlReader.setContentHandler(handler);
			StringReader stringReader = new StringReader(content);
			InputSource inputSource = new InputSource(stringReader);
			mXmlReader.parse(inputSource);

		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	class QueryMultiWeatherTask extends AsyncTask<Void, Void, Void> {

		private List<WeatherInfo> mWeatherInfoList;
		private String woeids = "";
		
		public QueryMultiWeatherTask(List<WeatherInfo> weatherInfoList) {
			this.mWeatherInfoList = weatherInfoList;
			int count = weatherInfoList.size();
			for (int i=0; i<count; i++) {
				woeids += weatherInfoList.get(i).getWoeid() + ((i==count-1)?"":",");
			}
		}
		
		@Override
		protected Void doInBackground(Void... params) {
			Log.d(TAG, "QueryWeather - doing");
			String url = URL_QUERY_MULTI_WEATHER_PART1 +woeids
					+ URL_QUERY_MULTI_WEATHER_PART2;
			String content = new WebAccessTools(mContext).getWebContent(url);
			parseMultiWeather(content, mWeatherInfoList);
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			mCallBacks.refreshAllWeatherFinished();
			mState = State.IDLE;
		}
		
	}
	
	private void parseMultiWeather(String content, List<WeatherInfo> mWeatherInfoList) {
		if (content == null || content.isEmpty()) {
			Log.w(TAG, "parseWeather content is Empty");
			return;
		}
		
		
		SAXParserFactory mSAXParserFactory = SAXParserFactory.newInstance();
		try {
			SAXParser mSAXParser = mSAXParserFactory.newSAXParser();
			XMLReader mXmlReader = mSAXParser.getXMLReader();
			MultiWeatherXMLParser handler = new MultiWeatherXMLParser(mContext,
					mWeatherInfoList);
			mXmlReader.setContentHandler(handler);
			StringReader stringReader = new StringReader(content);
			InputSource inputSource = new InputSource(stringReader);
			mXmlReader.parse(inputSource);

		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public boolean queryCity(String name, List<CityInfo> cityInfos) {
		Log.v(TAG, "queryCity");
		if (mState == State.IDLE) {
			mState = State.WORK_CITY;
			if (null != mQueryCityTask && 
					mQueryCityTask.getStatus() == AsyncTask.Status.RUNNING) {
				mQueryCityTask.cancel(true);
			}
			
			mQueryCityTask = new QueryCityTask(cityInfos);
			mQueryCityTask.execute(name);
			return true;
		} else {
			Log.v(TAG, "Busy, mState="+mState);
			return false;
		}
	}
	
	public void stopQueryCity() {
		if (null != mQueryCityTask && 
				mQueryCityTask.getStatus() == AsyncTask.Status.RUNNING) {
			mQueryCityTask.cancel(true);
		}
		if (mState == State.WORK_CITY) {
			mState = State.IDLE;
		}
	}
	
	class QueryCityTask extends AsyncTask<String, Void, Void> {
		private List<CityInfo> mCityInfos;
		
		public QueryCityTask(List<CityInfo> cityInfos) {
			mCityInfos = cityInfos;
		}
		
		@Override
		protected Void doInBackground(String... params) {
			String url = URL_QUERY_CITY_PART1
					+ params[0] + URL_QUERY_CITY_PART2;
			String content = new WebAccessTools(mContext).getWebContent(url);
			parseCity(content, mCityInfos);
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			if(null != mCallBacks) {
				mCallBacks.queryCityFinished();
			}
//			setCityListener(null);
			mState = State.IDLE;
		}
	}
	
	private void parseCity(String content, List<CityInfo> mCityInfos) {
		if (null == content || content.isEmpty()) {
			Log.w(TAG, "parseCity content is Empty");
			return;
		}

		mCityInfos.clear();

		SAXParserFactory mSAXParserFactory = SAXParserFactory.newInstance();
		try {
			SAXParser mSAXParser = mSAXParserFactory.newSAXParser();
			XMLReader mXmlReader = mSAXParser.getXMLReader();
			CityNameXMLParser handler = new CityNameXMLParser(mCityInfos);
			mXmlReader.setContentHandler(handler);
			StringReader stringReader = new StringReader(content);
			InputSource inputSource = new InputSource(stringReader);
			mXmlReader.parse(inputSource);

		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public boolean queryLocation(Location location, ArrayList<CityInfo> mCityInfos) {
		Log.v(TAG, "queryLocation");
		if(State.IDLE == mState) {
			mState = State.WORK_LOCATION;
			if (null != mQueryLocationTask && 
					mQueryLocationTask.getStatus() == AsyncTask.Status.RUNNING) {
				mQueryLocationTask.cancel(true);
			}
			
			mQueryLocationTask = new QueryLocationTask(location, mCityInfos);
			mQueryLocationTask.execute();
			
			return true;
		} else {
			Log.v(TAG, "Busy, mState="+mState);
			return false;
		}
	}
	
	class QueryLocationTask extends AsyncTask<Void, Void, Void> {
		private Location location;
		private ArrayList<CityInfo> mCityInfos;
		
		public QueryLocationTask(Location location, ArrayList<CityInfo> mCityInfos) {
			this.location = location;
			this.mCityInfos = mCityInfos;
		}
		
		@Override
		protected Void doInBackground(Void... params) {
			String url= URL_QUERY_LOCATION_PART1
					+ location.getLatitude()
					+ URL_QUERY_LOCATION_PART2
					+ location.getLongitude()
					+ URL_QUERY_LOCATION_PART3;
			
			String content = new WebAccessTools(mContext).getWebContent(url);
			parseCity(content, mCityInfos);
			if (mCityInfos.size() > 0) {
				mCityInfos.get(0).getLocationInfo()
						.setLat(location.getLatitude());
				mCityInfos.get(0).getLocationInfo()
						.setLon(location.getLongitude());
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			mState = State.IDLE;
			Intent intent = new Intent(WeatherAction.ACTION_QUERT_LOCATION_FINISH);
			mContext.sendBroadcast(intent);
		}
	}
	
	public void stopQueryLocation() {
		if (null != mQueryLocationTask && 
				mQueryLocationTask.getStatus() == AsyncTask.Status.RUNNING) {
			mQueryLocationTask.cancel(true);
		}
		
		if (State.WORK_LOCATION == mState) {
			mState = State.IDLE;
		}
	}
}
