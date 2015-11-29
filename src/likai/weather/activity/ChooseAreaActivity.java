package likai.weather.activity;

import java.util.ArrayList;
import java.util.List;

import likai.weather.R;
import likai.weather.db.CoolWeatherDB;
import likai.weather.model.City;
import likai.weather.model.County;
import likai.weather.model.Province;
import likai.weather.util.HttpCallbackListener;
import likai.weather.util.HttpUtil;
import likai.weather.util.Utility;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ChooseAreaActivity extends Activity {

	public static final int LEVEL_PROVINCE = 0;
	public static final int LEVEL_CITY = 1;
	public static final int LEVEL_COUNTY = 2;
	private ProgressDialog progressDialog;
	private TextView titleText;
	private ListView listView;
	private ArrayAdapter<String> adapter;
	private CoolWeatherDB mCoolWeatherDB;
	private List<String> mDataList = new ArrayList<String>();
	/**
	 * 省列表
	 */
	private List<Province> mProvinceList;
	/**
	 * 市列表
	 */
	private List<City> mCityList;
	/**
	 * 县列表
	 */
	private List<County> mCountyList;
	/**
	 * 选中的省份
	 */
	private Province mSelectedProvince;
	/**
	 * 选中的城市
	 */
	private City mSelectedCity;
	/**
	 * 当前选中的级别
	 */
	private int currentLevel;
	
	/**
	*  是否从WeatherActivity 中 跳 转过来。
	*/
	private boolean isFromWeatherActivity;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		isFromWeatherActivity = getIntent().getBooleanExtra("from_weather_activity", false);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		if (prefs.getBoolean("city_selected", false) && !isFromWeatherActivity) {
			Intent intent = new Intent(this, WeatherActivity.class);
			startActivity(intent);
			finish();
			return;
		}
		setContentView(R.layout.choose_area);
		listView = (ListView) findViewById(R.id.list_view);
		titleText = (TextView) findViewById(R.id.title_text);
		adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, mDataList);
		listView.setAdapter(adapter);
		mCoolWeatherDB = CoolWeatherDB.getInstance(this);
		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View view, int index,
					long arg3) {
				if (currentLevel == LEVEL_PROVINCE) {
					mSelectedProvince = mProvinceList.get(index);
					ChooseAreaActivity.this.queryCities();
				} else if (currentLevel == LEVEL_CITY) {
					mSelectedCity = mCityList.get(index);
					ChooseAreaActivity.this.queryCounties();
				}else if (currentLevel == LEVEL_COUNTY) {
					String countyCode = mCountyList.get(index).getCountyCode();
					Intent intent = new Intent(ChooseAreaActivity.this,
							WeatherActivity.class);
					intent.putExtra("county_code", countyCode);
					startActivity(intent);
					finish();
				}
			}
		});
		this.queryProvinces(); // 加载省级数据
	}

	/**
	 * 查询全国所有的省，优先从数据库查询，如果没有查询到再去服务器上查询。
	 */
	private void queryProvinces() {
		mProvinceList = mCoolWeatherDB.loadProvinces();
		if (mProvinceList.size() > 0) {
			mDataList.clear();
			for (Province province : mProvinceList) {
				mDataList.add(province.getProvinceName());
			}
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			titleText.setText("中国");
			currentLevel = LEVEL_PROVINCE;
		} else {
			queryFromServer(null, "province");
		}
	}

	/**
	 * 查询选中省内所有的市，优先从数据库查询，如果没有查询到再去服务器上查询。
	 */
	private void queryCities() {
		mCityList = mCoolWeatherDB.loadCities(mSelectedProvince.getId());
		if (mCityList.size() > 0) {
			mDataList.clear();
			for (City city : mCityList) {
				mDataList.add(city.getCityName());
			}
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			titleText.setText(mSelectedProvince.getProvinceName());
			currentLevel = LEVEL_CITY;
		} else {
			queryFromServer(mSelectedProvince.getProvinceCode(), "city");
		}
	}

	/**
	 * 查询选中市内所有的县，优先从数据库查询，如果没有查询到再去服务器上查询。
	 */
	private void queryCounties() {
		mCountyList = mCoolWeatherDB.loadCounties(mSelectedCity.getId());
		if (mCountyList.size() > 0) {
			mDataList.clear();
			for (County county : mCountyList) {
				mDataList.add(county.getCountyName());
			}
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			titleText.setText(mSelectedCity.getCityName());
			currentLevel = LEVEL_COUNTY;
		} else {
			queryFromServer(mSelectedCity.getCityCode(), "county");
		}
	}

	/**
	 * 根据传入的代号和类型从服务器上查询省市县数据。
	 * */
	private void queryFromServer(final String code, final String type) {
		String address;
		if (!TextUtils.isEmpty(code)) {
			address = "http://www.weather.com.cn/data/list3/city" + code
					+ ".xml";
		} else {
			address = "http://www.weather.com.cn/data/list3/city.xml";
		}
		showProgressDialog();
		HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {
			@Override
			public void onFinish(String response) {
				boolean result = false;
				if ("province".equals(type)) {
					result = Utility.handleProvincesResponse(mCoolWeatherDB,
							response);
				} else if ("city".equals(type)) {
					result = Utility.handleCitiesResponse(mCoolWeatherDB,
							response, mSelectedProvince.getId());
				} else if ("county".equals(type)) {
					result = Utility.handleCountiesResponse(mCoolWeatherDB,
							response, mSelectedCity.getId());
				}
				if (result) {
					// 通过runOnUiThread()方法回到主线程处理逻辑
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							closeProgressDialog();
							if ("province".equals(type)) {
								queryProvinces();
							} else if ("city".equals(type)) {
								queryCities();
							} else if ("county".equals(type)) {
								queryCounties();
							}
						}
					});
				}
			}

			@Override
			public void onError(Exception e) {
				// 通过runOnUiThread()方法回到主线程处理逻辑
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						closeProgressDialog();
						Toast.makeText(ChooseAreaActivity.this, "加载失败",
								Toast.LENGTH_SHORT).show();
					}
				});
			}
		});
	}

	/**
	 * 显示进度对话框
	 */
	private void showProgressDialog() {
		if (progressDialog == null) {
			progressDialog = new ProgressDialog(this);
			progressDialog.setMessage("正在加载...");
			progressDialog.setCanceledOnTouchOutside(false);
		}
		progressDialog.show();
	}

	/**
	 * 关闭进度对话框
	 */
	private void closeProgressDialog() {
		if (progressDialog != null) {
			progressDialog.dismiss();
		}
	}

	/**
	 * 捕获Back按键，根据当前的级别来判断，此时应该返回市列表、省列表、还是直接退出。
	 * 
	 * }
	 */
	@Override
	public void onBackPressed() {
		if (currentLevel == LEVEL_COUNTY) {
			queryCities();
		} else if (currentLevel == LEVEL_CITY) {
			queryProvinces();
		} else {
			if (isFromWeatherActivity) {
				Intent intent = new Intent(this, WeatherActivity.class);
				startActivity(intent);
			}
			finish();
		}
	}
}
