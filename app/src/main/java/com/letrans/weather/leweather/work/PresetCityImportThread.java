package com.letrans.weather.leweather.work;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.letrans.weather.leweather.R;

public class PresetCityImportThread extends Thread {
	
	private static boolean isRunningCheck = false;
	
	private static final String URI_CITY = "content://com.letrans.weather.leweather/gpresetcity";
	private static final String URI_WEATHRE = "content://com.letrans.weather.leweather/gweather";
	
	private Context mContext;
	
	private String mInsertHotCityWoeid[] = null;
	private String mInsertHotCity[] = null;
	private String mInsertCityWoeid[] = null;
	private String mInsertCity[] = null;
	
	private int hotCityCount = 0;
	private int cityCount = 0;
	

	public PresetCityImportThread(Context context) {
		mContext = context;
		
		mInsertHotCityWoeid = mContext.getResources().getStringArray(R.array.hot_citys_woeid);
		mInsertHotCity = mContext.getResources().getStringArray(R.array.hot_citys);
		mInsertCityWoeid = mContext.getResources().getStringArray(R.array.citys_woeid);
		mInsertCity = mContext.getResources().getStringArray(R.array.citys);
		
		if (mInsertHotCityWoeid != null && mInsertHotCity != null
				&& mInsertHotCityWoeid.length == mInsertHotCity.length) {
			hotCityCount = mInsertHotCityWoeid.length;
		}
		if (mInsertCityWoeid != null && mInsertCity != null
				&& mInsertCityWoeid.length == mInsertCity.length) {
			cityCount = mInsertCityWoeid.length;
		}
	}

	

    @Override
    public void run() {
    	if (hotCityCount != 0 || cityCount != 0) {
    		importPresetCity();
    	}
    }
    
    private void importPresetCity() {
    	if (isRunningCheck) {
    		Log.i("jingyi", "isRunningCheck="+isRunningCheck);
    		return;
    	}
    	isRunningCheck = true;
    	
		String where = WeatherProvider.WOEID+"=" + 
						(hotCityCount != 0 ? mInsertHotCityWoeid[0] : mInsertCityWoeid[0]);
        Cursor cityCursor = mContext.getContentResolver().query(Uri.parse(URI_CITY), new String[] {
        		WeatherProvider.WOEID, WeatherProvider.NAME
        }, where, null, null);
        
		try {
			if (cityCursor != null && cityCursor.getCount() > 0) {
				return;
			} else {
		    	for (int i = 0; i < hotCityCount; i++) {
		    		Log.i("jingyi", "i="+i+" mInsertHotCityWoeid[i]="+mInsertHotCityWoeid[i]);

    				int select = getSelect(mInsertHotCityWoeid[i]);
    				
    				ContentValues values = new ContentValues();
    				values.put(WeatherProvider.WOEID, mInsertHotCityWoeid[i]);
    				values.put(WeatherProvider.NAME, mInsertHotCity[i]);
    				values.put(WeatherProvider.IS_HOT_CITY, 1);
    				values.put(WeatherProvider.IS_SELECT, select);
    				mContext.getContentResolver().insert(Uri.parse(URI_CITY), values);

		    	}
		    	
		    	for (int i = 0; i < cityCount; i++) {
	
					int select = getSelect(mInsertCityWoeid[i]);
					
					ContentValues values = new ContentValues();
					values.put(WeatherProvider.WOEID, mInsertCityWoeid[i]);
					values.put(WeatherProvider.NAME, mInsertCity[i]);
					values.put(WeatherProvider.IS_HOT_CITY, 0);
					values.put(WeatherProvider.IS_SELECT, select);
//					values.put(WeatherProvider.SORY_KEY, (String.valueOf(mInsertCity[i].charAt(0)).toUpperCase()));
					values.put(WeatherProvider.SORY_KEY, getSpells(mInsertCity[i].substring(0,1)).toUpperCase());
					mContext.getContentResolver().insert(Uri.parse(URI_CITY), values);
		    	}
	
			}
		} finally {
			if (cityCursor != null) {
				cityCursor.close();
			}
		}

    	
    	isRunningCheck = false;
    }

    private int getSelect(String woeid) {
		int select = 0;
		String defaultWoeid = mContext.getResources().getString(R.string.default_woeid);
		if (defaultWoeid.compareTo(woeid) == 0) {
    		String where1 = WeatherProvider.WOEID + "=" + woeid;
            Cursor cur = mContext.getContentResolver().query(Uri.parse(URI_WEATHRE), new String[] {
            		WeatherProvider.WOEID
            }, where1, null, null);
            
            if(cur != null) {
            	if (cur.getCount() > 0) {
            		select = 1;
            	}
            	cur.close();
            }
		}
		return select;
    }

	static final int GB_SP_DIFF = 160;
	// 存放国标一级汉字不同读音的起始区位码
	static final int[] secPosValueList = { 1601, 1637, 1833, 2078, 2274, 2302,
			2433, 2594, 2787, 3106, 3212, 3472, 3635, 3722, 3730, 3858, 4027,
			4086, 4390, 4558, 4684, 4925, 5249, 5600 };
	// 存放国标一级汉字不同读音的起始区位码对应读音
	static final char[] firstLetter = { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h',
			'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'w', 'x',
			'y', 'z' };

	public static String getSpells(String characters) {
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < characters.length(); i++) {

			char ch = characters.charAt(i);
			if ((ch >> 7) == 0) {
				// 判断是否为汉字，如果左移7为为0就不是汉字，否则是汉字
			} else {
				char spell = getFirstLetter(ch);
				buffer.append(String.valueOf(spell));
			}
		}
		return buffer.toString();
	}

	// 获取一个汉字的首字母
	public static Character getFirstLetter(char ch) {

		byte[] uniCode = null;
		try {
			uniCode = String.valueOf(ch).getBytes("GBK");
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		if (uniCode[0] < 128 && uniCode[0] > 0) { // 非汉字
			return null;
		} else {
			return convert(uniCode);
		}
	}

	static char convert(byte[] bytes) {
		char result = '-';
		int secPosValue = 0;
		int i;
		for (i = 0; i < bytes.length; i++) {
			bytes[i] -= GB_SP_DIFF;
		}
		secPosValue = bytes[0] * 100 + bytes[1];
		for (i = 0; i < 23; i++) {
			if (secPosValue >= secPosValueList[i]
					&& secPosValue < secPosValueList[i + 1]) {
				result = firstLetter[i];
				break;
			}
		}
		return result;
	}
    
}
