package com.boazsh.m_i_close.app.helpers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class JSONParser {

	public JSONObject getJSONFromUrl(String url) {
		
		InputStream inputStream = null;
		JSONObject jsonObject = null;
		String jsonString = "";

		try {
			
			DefaultHttpClient httpClient = new DefaultHttpClient();

			HttpResponse httpResponse = httpClient.execute(new HttpPost(url));
			inputStream = httpResponse.getEntity().getContent();

		} catch (UnsupportedEncodingException e) {
			
			e.printStackTrace();
			
		} catch (ClientProtocolException e) {
			
			e.printStackTrace();
			
		} catch (IOException e) {
			
			e.printStackTrace();
		}

		try {
			
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream), 8);
			StringBuilder stringBuilder = new StringBuilder();
			String line = null;
			
			while ((line = bufferedReader.readLine()) != null) {
				
				stringBuilder.append(line + "\n");
			}
			
			inputStream.close();
			jsonString = stringBuilder.toString();
			
		} catch (Exception e) {
			
			Log.e("Buffer Error", "Error converting result " + e.toString());
		}


		try {
			
			jsonObject = new JSONObject(jsonString);
			
		} catch (JSONException e) {
			
			Log.e("JSON Parser", "Error parsing data " + e.toString());
		}

		return jsonObject;
	}
}
