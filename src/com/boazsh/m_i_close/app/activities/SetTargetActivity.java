package com.boazsh.m_i_close.app.activities;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.boazsh.m_i_close.app.R;
import com.boazsh.m_i_close.app.helpers.AutoCompleteTask;
import com.boazsh.m_i_close.app.helpers.MICloseUtils;
import com.boazsh.m_i_close.app.services.LocationService;


public class SetTargetActivity extends MICloseBaseActivity {

	protected static final String HEBREW_CODE = "iw";

	private SeekBar mDistanceSeekBar;
	private AutoCompleteTextView mAddressAutoCompleteTextView;
	private TextView mGoTextView;
	private TextView mKilometersTextView;
	
	private String mLocationString;
	private int mSeekBarMultiFactor;
	private Address mTargetAddress;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_set_target);
        
		Log.d(MICloseUtils.APP_LOG_TAG, "SetTargetActivity was started");
		
        mTargetAddress = null;
		mLocationString = "";
		
		int seekBarInitValue = getIntegerResource(R.integer.seekBar_init_value);
		mSeekBarMultiFactor = getIntegerResource(R.integer.seekBar_multi_factor);
		
		mDistanceSeekBar = (SeekBar) findViewById(R.id.targetDistanceSeekBar);
		mGoTextView = (TextView) findViewById(R.id.goTextView);
		mAddressAutoCompleteTextView = (AutoCompleteTextView) findViewById(R.id.targetAddressEditText);
		mKilometersTextView = (TextView) findViewById(R.id.metersNumberTextView);
		
		String lng = Locale.getDefault().getLanguage();
	
		if (lng.equals(HEBREW_CODE)) {
			
			Log.d(MICloseUtils.APP_LOG_TAG, "Hebrew language is active");
			
			TextView setDistance = (TextView) findViewById(R.id.setAlarmTextView);
			setDistance.setGravity(android.view.Gravity.RIGHT);
			
			TextView metersTextView = (TextView) findViewById(R.id.metersTextView);
			
			LayoutParams metersTextViewLayoutParams = (LayoutParams) metersTextView.getLayoutParams();
			
			metersTextViewLayoutParams.addRule(RelativeLayout.ALIGN_RIGHT, 0);
			metersTextViewLayoutParams.addRule(RelativeLayout.ALIGN_LEFT, R.id.targetAddressEditText);
			
			LayoutParams metersNumberTextViewLayoutParams = (LayoutParams) mKilometersTextView.getLayoutParams();
			
			metersNumberTextViewLayoutParams.addRule(RelativeLayout.LEFT_OF, 0);
			metersNumberTextViewLayoutParams.addRule(RelativeLayout.ALIGN_LEFT, 0);
			metersNumberTextViewLayoutParams.addRule(RelativeLayout.RIGHT_OF, R.id.metersTextView);
			metersNumberTextViewLayoutParams.setMargins(5, 0, 0, 0);
			
			metersTextView.setLayoutParams(metersTextViewLayoutParams);
			mKilometersTextView.setLayoutParams(metersNumberTextViewLayoutParams);
		}
		
		
		mAddressAutoCompleteTextView.setThreshold(3);
		mAddressAutoCompleteTextView.addTextChangedListener(getAddressAutoCompleteTextViewTextChangedListener());
		mAddressAutoCompleteTextView.setOnEditorActionListener(getAddressAutoCompleteTextViewOnEditorActionListener());
		
		mDistanceSeekBar.setProgress(seekBarInitValue / mSeekBarMultiFactor);
		mKilometersTextView.setText(String.valueOf(seekBarInitValue));
		mGoTextView.setOnClickListener(getGoTextViewOnClickListener());
		mDistanceSeekBar.setOnSeekBarChangeListener(getDistanceSeekBarOnSeekBarChangeListener());
	}
	
	
	private OnEditorActionListener getAddressAutoCompleteTextViewOnEditorActionListener() {
		
		return new OnEditorActionListener() {
			   
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_GO) {
					mGoTextView.performClick();
			    }
			       return false;
			}
		};
	}

    
	private View.OnClickListener getGoTextViewOnClickListener() {
		
		return new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				
				if (!MICloseUtils.isLocationAvailable(SetTargetActivity.this)) {
					return;
				}

				mLocationString = mAddressAutoCompleteTextView.getText().toString();
				
				if (mLocationString.equals("")) {
	    			
	    			showToast(R.string.you_need_to_type_address, true);
	    			return;
	    		}

				Log.d(MICloseUtils.APP_LOG_TAG, "Geocoding input address");
				GeocodeTask geocodeTask = new GeocodeTask();
				geocodeTask.execute();
				
				boolean result = false;
				
				try {
					
					int timeout = getResources().getInteger(R.integer.geocode_timout);
					result = geocodeTask.get(timeout, TimeUnit.SECONDS);
					
				} catch (TimeoutException e) {
					
					Log.w(MICloseUtils.APP_LOG_TAG, "Geocode timeout reached!");
					showToast(R.string.connection_timeout, true);
					return;
					
				} catch (Exception e) {
					
					Log.e(MICloseUtils.APP_LOG_TAG, "Geocode task failed!");
					e.printStackTrace();
				}
				
				if (result == false || mTargetAddress == null) {

					return;
				}

				int targetDistance = (mDistanceSeekBar.getProgress() + 1)
						* mSeekBarMultiFactor;
				
				mMICloseStore.clear().commit();
				Log.d(MICloseUtils.APP_LOG_TAG, "Store data cleared");

				mMICloseStore.setTargetDistance(targetDistance)
							.setTargetLatitude(mTargetAddress.getLatitude())
							.setTargetLongitude(mTargetAddress.getLongitude())
							.commit();

				
				startProximityAlarmSchedule();
				
				Intent alarmActivityIntent = new Intent(SetTargetActivity.this, AlarmActivity.class);
				
				Log.d(MICloseUtils.APP_LOG_TAG, "Starting AlarmActivity");
				startActivity(alarmActivityIntent);
			}
		};
	}
	
	
	
	
	private TextWatcher getAddressAutoCompleteTextViewTextChangedListener() {
		
		return new TextWatcher() {

			public void afterTextChanged(Editable s) {}

			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

			public void onTextChanged(CharSequence s, int start, int before, int count) {

				AutoCompleteTask autoCompleteTask = new AutoCompleteTask(getApplicationContext(), mAddressAutoCompleteTextView);
				autoCompleteTask.execute();

			}
		};
	}
	
	private SeekBar.OnSeekBarChangeListener getDistanceSeekBarOnSeekBarChangeListener() {
		
		return new SeekBar.OnSeekBarChangeListener() {

			public void onProgressChanged(SeekBar seekBar,
					int progress, boolean fromUser) {

				mKilometersTextView.setText(String
						.valueOf((progress + 1) * mSeekBarMultiFactor));
			}

			public void onStartTrackingTouch(SeekBar seekBar) {

			}

			public void onStopTrackingTouch(SeekBar seekBar) {

			}
		};
	}
	
	private void startProximityAlarmSchedule() {
		
		Intent locationServiceIntent = new Intent(SetTargetActivity.this, LocationService.class);
		
		PendingIntent pendingIntent = PendingIntent.getService(	SetTargetActivity.this, 
																MICloseUtils.LOCATION_SERVICE_INTENT_ID, 
																locationServiceIntent,
																PendingIntent.FLAG_CANCEL_CURRENT);

        AlarmManager alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        int proximityRequestInterval = getResources().getInteger(R.integer.proximity_request_interval);
        
        alarmMgr.setRepeating(	AlarmManager.RTC_WAKEUP,
        						System.currentTimeMillis(), 
        						proximityRequestInterval  + 500, 
        						pendingIntent);
        
        Log.d(MICloseUtils.APP_LOG_TAG, "AlarmManager repeating proximity task was set");
	}

    
    public class GeocodeTask extends AsyncTask<Void, Integer, Boolean> {

    	
    	private Context mApplicationContext;
    	private Geocoder mGeocoder;
    	List<Address> mAddresses;
		private ProgressDialog mProgressDialog;

    	
    	@Override
    	protected void onPreExecute() {
    		
    		mApplicationContext = getApplicationContext();
    		mGeocoder = new Geocoder(mApplicationContext);
    		mAddresses = null;
    		
    		mProgressDialog = new ProgressDialog(SetTargetActivity.this);
			mProgressDialog.setTitle(R.string.processing);
			mProgressDialog.setMessage(getResources().getString(R.string.please_wait));
			mProgressDialog.setCancelable(false);
			mProgressDialog.setIndeterminate(true);
			mProgressDialog.show();
    	}
    	
    	@Override
    	protected Boolean doInBackground(Void... params) {

    		Log.d(MICloseUtils.APP_LOG_TAG, "Geocode AsyncTask is running");
    		
    		try {
    			mAddresses = mGeocoder.getFromLocationName(mLocationString, 1);

    		} catch (IllegalArgumentException e) {

    			Log.e(MICloseUtils.APP_LOG_TAG, "Geocode input address is not valid");
    			showToast(R.string.internal_error, true);
    			return false;

    		} catch (IOException e) {

    			Log.e(MICloseUtils.APP_LOG_TAG, "Geocode network connection is not available");
    			showToast(R.string.no_internet_connection, true);
    			return false;
    		}

    		if (mAddresses.size() == 0) {
    			Log.w(MICloseUtils.APP_LOG_TAG, "Geocode address not found");
    			showToast(R.string.adress_not_found, true);
    			return false;
    		}

    		mTargetAddress = mAddresses.get(0);
    		return true;
    	}

    	@Override
    	protected void onPostExecute(Boolean result) {
    		
    		if (mProgressDialog!=null) {
    			
				mProgressDialog.dismiss();
			}
    	}
    }
}
