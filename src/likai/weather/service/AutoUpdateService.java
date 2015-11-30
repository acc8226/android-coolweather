package likai.weather.service;

import likai.weather.util.HttpCallbackListener;
import likai.weather.util.HttpUtil;
import likai.weather.util.Utility;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;

/**
 * 创建一个长期在后台运行的定时任务
 * 
 * @author likai
 */
public class AutoUpdateService extends Service {

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				AutoUpdateService.this.updateWeather();
			}
		}).start();
		AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		long triggerAtMillis = SystemClock.elapsedRealtime() + 1000 * 60 * 60 * 8;
		Intent broadIntent = new Intent();
		PendingIntent pdIntent = PendingIntent.getBroadcast(this, 0,
				broadIntent, 0);
		am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtMillis, pdIntent);
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	/**
	 * 更新天气到sp中
	 */
	private void updateWeather() {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		String weatherCode = prefs.getString("weather_code", "");
		String address = "http://www.weather.com.cn/data/cityinfo/"
				+ weatherCode + ".html";
		HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {
			@Override
			public void onFinish(String response) {
				Utility.handleWeatherResponse(AutoUpdateService.this, response);
			}

			@Override
			public void onError(Exception e) {
				e.printStackTrace();
			}
		});

	}

}
