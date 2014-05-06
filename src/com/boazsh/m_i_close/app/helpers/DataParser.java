package com.boazsh.m_i_close.app.helpers;

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

public class DataParser extends AsyncTask<Void, Integer, Void> {

	private static final String TAG_RESULT = "predictions";
	
	private ArrayList<String> mSuggedtedAddresses;
	private String mRequestUrl;
	private Context mApplicationContext;
	private AutoCompleteTextView mAddress_AutoCompleteTextView;
	
	public DataParser (Context applicationContext, String requestUrl, AutoCompleteTextView autoCompleteTextView) {
		
		mSuggedtedAddresses = new ArrayList<String>();
		mRequestUrl = requestUrl;
		mApplicationContext = applicationContext;
		mAddress_AutoCompleteTextView = autoCompleteTextView;
	}
	
	@Override
	protected Void doInBackground(Void... params) {

		JSONObject json;
		JSONParser jParser = new JSONParser();
		JSONArray addresses = null;

		json = jParser.getJSONFromUrl(mRequestUrl.toString());
		
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
