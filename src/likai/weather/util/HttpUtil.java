package likai.weather.util;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

public class HttpUtil {

	public static void sendHttpRequest(final String address,
			final HttpCallbackListener listener) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				HttpClient client = new DefaultHttpClient();
				HttpGet httpGet = new HttpGet(address);
				try {
					HttpResponse response = client.execute(httpGet);
					if (response.getStatusLine().getStatusCode() == 200) {
						HttpEntity entity = response.getEntity();
						String result = EntityUtils.toString(entity, "utf-8");
						listener.onFinish(result);
					}
				} catch (ClientProtocolException e) {
					e.printStackTrace();
					listener.onError(e);
				} catch (IOException e) {
					e.printStackTrace();
					listener.onError(e);
				}
			}
		}).start();

	}

}
