package com.boazsh.m_i_close.app.activities;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;

import com.boazsh.m_i_close.app.R;
import com.boazsh.m_i_close.app.helpers.DataParser;
import com.boazsh.m_i_close.app.services.LocationService;


public class SetTargetActivity extends MICloseBaseActivity {

	
	private static final String WEB_API_KEY = "AIzaSyBG3ZEQRc1AhXgeIV9dsSUo7mY7FfhgjV8";
	
	private SeekBar mDistance_SeekBar;
	private AutoCompleteTextView mAddress_AutoCompleteTextView;
	private TextView mGo_TextView;
	private TextView mKilometers_TextView;
	private String mLocationString;

	private int mTargetDistance;
	private int mSeekBarMultiFactor;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_set_target1);

		mLocationString = "";
		
		mDistance_SeekBar = (SeekBar) findViewById(R.id.targetDistanceSeekBar);
		mGo_TextView = (TextView) findViewById(R.id.goTextView);
		mAddress_AutoCompleteTextView = (AutoCompleteTextView) findViewById(R.id.targetAddressEditText);
		mKilometers_TextView = (TextView) findViewById(R.id.metersNumberTextView);

		mAddress_AutoCompleteTextView.setThreshold(3);
		mAddress_AutoCompleteTextView.addTextChangedListener(new TextWatcher() {

			public void afterTextChanged(Editable s) {

			}

			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {

			}

			public void onTextChanged(CharSequence s, int start, int before,
					int count) {

				String[] search_text;
				String mAutoCompleteRequestUrl;
				
				search_text = mAddress_AutoCompleteTextView.getText().toString().split(",");

				String input;
				
				try {
					
					input = URLEncoder.encode(search_text[0], "utf8");
					
				} catch (Exception e) {
					
					Log.e(MICLOSE_LOG_LABEL, "Failed to encode user address");
					input = null;
				}
				
				mAutoCompleteRequestUrl = "https://maps.googleapis.com/maps/api/place/autocomplete/json";
				mAutoCompleteRequestUrl += "?input=" + input;
				mAutoCompleteRequestUrl += "&sensor=true&language=iw&";
				mAutoCompleteRequestUrl += "key=" + WEB_API_KEY;
				
				if (search_text.length <= 1) {

					Log.d("URL", mAutoCompleteRequestUrl);
					DataParser parse = new DataParser(getApplicationContext(), mAutoCompleteRequestUrl, mAddress_AutoCompleteTextView);
					parse.execute();
				}

			}
		});

		int seekBarInitValue = getIntegerResource(R.integer.seekBar_init_value);
		mSeekBarMultiFactor = getIntegerResource(R.integer.seekBar_multi_factor);

		mDistance_SeekBar.setProgress(seekBarInitValue / mSeekBarMultiFactor);
		mKilometers_TextView.setText(String.valueOf(seekBarInitValue));

		mGo_TextView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

				Address locationAddress = getTargetAddress();

				if (locationAddress == null) {

					return;
				}

				clearSharedPreferences();
				mPreferencesEditor.putFloat(TARGET_LATITUDE_KEY,
						(float) locationAddress.getLatitude());
				mPreferencesEditor.putFloat(TARGET_LONGITUDE_KEY,
						(float) locationAddress.getLongitude());

				mTargetDistance = (mDistance_SeekBar.getProgress() + 1)
						* mSeekBarMultiFactor;

				mPreferencesEditor.putInt(TARGET_DISTANCE_KEY, mTargetDistance);
				mPreferencesEditor.putBoolean(ALARM_SET_KEY, true);
				mPreferencesEditor.apply();

				startLocationAlarmService(locationAddress);

				Intent intent = new Intent(SetTargetActivity.this,
						AlarmActivity.class);
				startActivity(intent);
			}
		});

		mDistance_SeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

					public void onProgressChanged(SeekBar seekBar,
							int progress, boolean fromUser) {

						mKilometers_TextView.setText(String
								.valueOf((progress + 1) * mSeekBarMultiFactor));
					}

					public void onStartTrackingTouch(SeekBar seekBar) {
						// TODO Auto-generated method stub
					}

					public void onStopTrackingTouch(SeekBar seekBar) {

					}
				});
	}

	private Address getTargetAddress() {

		Geocoder geocoder = new Geocoder(getApplicationContext());

		List<Address> addresses = null;

		if (mLocationString.equals("")) {
			mLocationString = mAddress_AutoCompleteTextView.getText()
					.toString();
		}

		if (mLocationString.equals("")) {
			showToast(R.string.you_need_to_type_address, true);
			return null;
		}

		try {
			addresses = geocoder.getFromLocationName(
					mAddress_AutoCompleteTextView.getText().toString(), 1);

		} catch (IllegalArgumentException e) {

			Log.e("****** Location", "Input address is not valid");
			showToast(R.string.internal_error, true);
			return null;

		} catch (IOException e) {

			Log.e("****** Location", "Network connection is not available");
			showToast(R.string.no_internet_connection, true);
			return null;
		}

		if (addresses.size() == 0) {
			showToast(R.string.adress_not_found, true);
			return null;
		}

		return addresses.get(0);
	}

	private void startLocationAlarmService(Address address) {

		Location targetLocation = createLocationObject(address.getLatitude(),
				address.getLongitude());

		Intent serviceIntent = new Intent(SetTargetActivity.this,
				LocationService.class);

		serviceIntent.putExtra(TARGET_LOCATION_KEY, targetLocation);
		serviceIntent.putExtra(TARGET_DISTANCE_KEY, mTargetDistance);

		startService(serviceIntent);
	}

	
}
