package com.boazsh.m_i_close.app.helpers;

import java.net.URLEncoder;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import com.boazsh.m_i_close.app.R;
import com.boazsh.m_i_close.app.activities.SetTargetActivity;

public class AutoCompleteTask extends AsyncTask<Void, Integer, Void> {

	private static final String TAG_RESULT = "predictions";
	private static final String WEB_API_KEY = "AIzaSyBG3ZEQRc1AhXgeIV9dsSUo7mY7FfhgjV8";
	
	private ArrayList<String> mSuggedtedAddresses;
	String mAutoCompleteRequestUrl;
	private Context mApplicationContext;
	private AutoCompleteTextView mAddress_AutoCompleteTextView;
	
	public AutoCompleteTask (Context applicationContext, AutoCompleteTextView autoCompleteTextView) {
		
		mSuggedtedAddresses = new ArrayList<String>();
		mApplicationContext = applicationContext;
		mAddress_AutoCompleteTextView = autoCompleteTextView;
	}
	
	@Override
	protected void onPreExecute() {
		
		String[] search_text;
		
		String input;
		
		search_text = mAddress_AutoCompleteTextView.getText().toString().split(",");

		try {
			
			input = URLEncoder.encode(search_text[0], "utf8");
			
		} catch (Exception e) {
			
			Log.e(SetTargetActivity.MICLOSE_LOG_LABEL, "Failed to encode user address");
			input = null;
		}
		
		mAutoCompleteRequestUrl = "https://maps.googleapis.com/maps/api/place/autocomplete/json";
		mAutoCompleteRequestUrl += "?input=" + input;
		mAutoCompleteRequestUrl += "&sensor=true&language=iw&";
		mAutoCompleteRequestUrl += "key=" + WEB_API_KEY;
		
		if (search_text.length <= 1) {

			Log.d("URL", mAutoCompleteRequestUrl);
			
		} else {
			
			cancel(false);
		}
	}
	
	@Override
	protected Void doInBackground(Void... params) {

		JSONObject json;
		JSONParser jParser = new JSONParser();
		JSONArray addresses = null;

		json = jParser.getJSONFromUrl(mAutoCompleteRequestUrl);
		
		if (json != null) {
			
			try {

				addresses = json.getJSONArray(TAG_RESULT);

				for (int i = 0; i < addresses.length(); i++) {
					
					JSONObject c = addresses.getJSONObject(i);
					String description = c.getString("description");
					Log.d("description", description);
					mSuggedtedAddresses.add(description);
				}
				
			} catch (JSONException e) {
				
				e.printStackTrace();
			}
		}

		return null;
	}

	@Override
	protected void onPostExecute(Void result) {
		
		ArrayAdapter<String> addressArrayAdapter;
		
		addressArrayAdapter = new ArrayAdapter<String>(mApplicationContext,
				R.layout.list_item, mSuggedtedAddresses);

		mAddress_AutoCompleteTextView.setAdapter(addressArrayAdapter);
	}
}
