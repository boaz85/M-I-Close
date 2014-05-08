package com.boazsh.m_i_close.app.activities;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;

import com.boazsh.m_i_close.app.R;
import com.boazsh.m_i_close.app.geofence.GeofenceRemover;
import com.boazsh.m_i_close.app.geofence.GeofenceRequester;
import com.boazsh.m_i_close.app.geofence.GeofenceUtils;
import com.boazsh.m_i_close.app.geofence.GeofenceWrapper;
import com.boazsh.m_i_close.app.geofence.GeofenceStore;
import com.boazsh.m_i_close.app.helpers.Constants;
import com.boazsh.m_i_close.app.helpers.DataParser;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.Geofence;

public class SetTargetActivity extends MICloseBaseActivity {

	
	private static final String WEB_API_KEY = "AIzaSyBG3ZEQRc1AhXgeIV9dsSUo7mY7FfhgjV8";
	private static final long GEOFENCE_EXPIRATION = Geofence.NEVER_EXPIRE;

	private SeekBar mDistance_SeekBar;
	private AutoCompleteTextView mAddress_AutoCompleteTextView;
	private TextView mGo_TextView;
	private TextView mKilometers_TextView;
	private String mLocationString;

	private int mTargetDistance;
	private int mSeekBarMultiFactor;
    private GeofenceUtils.REQUEST_TYPE mRequestType;
    private GeofenceStore mGeofenceStore;
    private Geofence mCurrentGeofence;
    private GeofenceRequester mGeofenceRequester;
    private GeofenceRemover mGeofenceRemover;
    private GeofenceWrapper mGeofenceWrapper;
    private GeofenceStatusReceiver mBroadcastReceiver;
    private IntentFilter mIntentFilter;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_set_target);
		
		mBroadcastReceiver = new GeofenceStatusReceiver();
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(GeofenceUtils.ACTION_GEOFENCES_ADDED);
        mIntentFilter.addAction(GeofenceUtils.ACTION_GEOFENCE_ERROR);
        
        mGeofenceStore = new GeofenceStore(this);
        mCurrentGeofence = null;
        mGeofenceRequester = new GeofenceRequester(this);
        mGeofenceRemover = new GeofenceRemover(this);
		mLocationString = "";
		
		int seekBarInitValue = getIntegerResource(R.integer.seekBar_init_value);
		mSeekBarMultiFactor = getIntegerResource(R.integer.seekBar_multi_factor);
		
		mDistance_SeekBar = (SeekBar) findViewById(R.id.targetDistanceSeekBar);
		mGo_TextView = (TextView) findViewById(R.id.goTextView);
		mAddress_AutoCompleteTextView = (AutoCompleteTextView) findViewById(R.id.targetAddressEditText);
		mKilometers_TextView = (TextView) findViewById(R.id.metersNumberTextView);
		
		mAddress_AutoCompleteTextView.setThreshold(3);
		mAddress_AutoCompleteTextView.addTextChangedListener(getAddress_AutoCompleteTextViewTextChangedListener());
		mDistance_SeekBar.setProgress(seekBarInitValue / mSeekBarMultiFactor);
		mKilometers_TextView.setText(String.valueOf(seekBarInitValue));
		mGo_TextView.setOnClickListener(getGo_TextViewOnClickListener());
		mDistance_SeekBar.setOnSeekBarChangeListener(getDistance_SeekBarOnSeekBarChangeListener());
	}
	
	@Override
	public void onResume() {
		super.onResume();
		registerReceiver(mBroadcastReceiver, mIntentFilter);
	}
	
	@Override
	public void onPause() {
		
		unregisterReceiver(mBroadcastReceiver);
		super.onDestroy();
	}
	
	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {

        switch (requestCode) {

            case GeofenceUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST :

                switch (resultCode) {
                    
                    case Activity.RESULT_OK:

                        if (GeofenceUtils.REQUEST_TYPE.ADD == mRequestType) {

                            mGeofenceRequester.setInProgressFlag(false);
                            mGeofenceRequester.addGeofence(mCurrentGeofence);

                        } else if (GeofenceUtils.REQUEST_TYPE.REMOVE == mRequestType ){

                            mGeofenceRemover.setInProgressFlag(false);
                            mGeofenceRemover.removeGeofencesById(GeofenceWrapper.GEOFENCE_ID);
                        }
                        
                        break;

                    default:

                        Log.d(GeofenceUtils.APPTAG, getString(R.string.no_resolution));
                        //TODO: Show error!
                }

            default:

               Log.d(GeofenceUtils.APPTAG,
                       getString(R.string.unknown_activity_request_code, requestCode));
             //TODO: Show error!

               break;
        }
    }
    
	private View.OnClickListener getGo_TextViewOnClickListener() {
		
		return new View.OnClickListener() {
			@Override
			public void onClick(View v) {

				Address locationAddress = geocodeAddress();

				if (locationAddress == null) {

					return;
				}

				mTargetDistance = (mDistance_SeekBar.getProgress() + 1)
						* mSeekBarMultiFactor;

		        mRequestType = GeofenceUtils.REQUEST_TYPE.ADD;

		        if (!servicesConnected()) {

		        	//TODO: Show error
		            return;
		        }
		        
				if (mGeofenceStore.getGeofence() != null) {

					mGeofenceRemover.setInProgressFlag(false);
					mGeofenceRemover.removeGeofencesById(GeofenceWrapper.GEOFENCE_ID);
				}
				
				mPreferencesEditor.clear();
				mPreferencesEditor.apply();

		        mGeofenceWrapper = new GeofenceWrapper(
										            locationAddress.getLatitude(),
										            locationAddress.getLongitude(),
										            mTargetDistance,
										            GEOFENCE_EXPIRATION,
										            Geofence.GEOFENCE_TRANSITION_ENTER);

		        mGeofenceStore.setGeofence(mGeofenceWrapper);
		        mCurrentGeofence = mGeofenceWrapper.getGeofenceObject();
		        
		        try {
		        	
		            mGeofenceRequester.addGeofence(mCurrentGeofence);
		            
		        } catch (UnsupportedOperationException e) {

		            Toast.makeText(SetTargetActivity.this, R.string.add_geofences_already_requested_error,
		                        Toast.LENGTH_LONG).show();
		        }
		        
		        Intent intent = new Intent(SetTargetActivity.this, AlarmActivity.class);
				startActivity(intent);
				
			}
		};
	}
	
	
	private TextWatcher getAddress_AutoCompleteTextViewTextChangedListener() {
		
		return new TextWatcher() {

			public void afterTextChanged(Editable s) {

			}

			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {

			}

			public void onTextChanged(CharSequence s, int start, int before,
					int count) {

				getAutoCompleteList();

			}
		};
	}
	
	private SeekBar.OnSeekBarChangeListener getDistance_SeekBarOnSeekBarChangeListener() {
		
		return new SeekBar.OnSeekBarChangeListener() {

			public void onProgressChanged(SeekBar seekBar,
					int progress, boolean fromUser) {

				mKilometers_TextView.setText(String
						.valueOf((progress + 1) * mSeekBarMultiFactor));
			}

			public void onStartTrackingTouch(SeekBar seekBar) {

			}

			public void onStopTrackingTouch(SeekBar seekBar) {

			}
		};
	}
	
	
	private void getAutoCompleteList() {
		
		String[] search_text;
		String autoCompleteRequestUrl;
		String input;
		
		search_text = mAddress_AutoCompleteTextView.getText().toString().split(",");

		try {
			
			input = URLEncoder.encode(search_text[0], "utf8");
			
		} catch (Exception e) {
			
			Log.e(MICLOSE_LOG_LABEL, "Failed to encode user address");
			input = null;
		}
		
		autoCompleteRequestUrl = "https://maps.googleapis.com/maps/api/place/autocomplete/json";
		autoCompleteRequestUrl += "?input=" + input;
		autoCompleteRequestUrl += "&sensor=true&language=iw&";
		autoCompleteRequestUrl += "key=" + WEB_API_KEY;
		
		if (search_text.length <= 1) {

			Log.d("URL", autoCompleteRequestUrl);
			DataParser parse = new DataParser(getApplicationContext(), autoCompleteRequestUrl, mAddress_AutoCompleteTextView);
			parse.execute();
		}
	}
	

	private Address geocodeAddress() {

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

			Log.e(Constants.APP_LOG_TAG, "Input address is not valid");
			showToast(R.string.internal_error, true);
			return null;

		} catch (IOException e) {

			Log.e(Constants.APP_LOG_TAG, "Network connection is not available");
			showToast(R.string.no_internet_connection, true);
			return null;
		}

		if (addresses.size() == 0) {
			showToast(R.string.adress_not_found, true);
			return null;
		}

		return addresses.get(0);
	}
	
	public boolean servicesConnected() {

        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

        if (ConnectionResult.SUCCESS == resultCode) {

            Log.d(GeofenceUtils.APPTAG, getString(R.string.play_services_available));
            return true;

        } else {

            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(resultCode, this, 0);
            
            if (dialog != null) {
            	
                ErrorDialogFragment errorFragment = new ErrorDialogFragment();
                errorFragment.setDialog(dialog);
                //errorFragment.show(getSupportFragmentManager(), GeofenceUtils.APPTAG);
            }
            
            return false;
        }
    }

	
    public class GeofenceStatusReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();

            if (TextUtils.equals(action, GeofenceUtils.ACTION_GEOFENCE_ERROR)) {

                handleGeofenceError(context, intent);

            } else if (TextUtils.equals(action, GeofenceUtils.ACTION_GEOFENCES_ADDED)) {

                handleGeofenceStatus(context, intent);

            } else {
            	
                Log.e(GeofenceUtils.APPTAG, getString(R.string.invalid_action_detail, action));
                Toast.makeText(context, R.string.invalid_action, Toast.LENGTH_LONG).show();
            }
        }


        private void handleGeofenceStatus(Context context, Intent intent) {
        	
        	Intent alarmActivityIntent = new Intent(SetTargetActivity.this,
					AlarmActivity.class);
        	
			startActivity(alarmActivityIntent);
        }

        private void handleGeofenceError(Context context, Intent intent) {
            String msg = intent.getStringExtra(GeofenceUtils.EXTRA_GEOFENCE_STATUS);
            Log.e(GeofenceUtils.APPTAG, msg);
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
        }
    }

    public static class ErrorDialogFragment extends DialogFragment {

        // Global field to contain the error dialog
        private Dialog mDialog;

        public ErrorDialogFragment() {
            super();
            mDialog = null;
        }

        public void setDialog(Dialog dialog) {
            mDialog = dialog;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return mDialog;
        }
    }

}
