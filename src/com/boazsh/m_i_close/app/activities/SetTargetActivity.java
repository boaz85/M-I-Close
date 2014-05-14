package com.boazsh.m_i_close.app.activities;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.TextUtils;
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
import android.widget.Toast;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.boazsh.m_i_close.app.R;
import com.boazsh.m_i_close.app.geofence.GeofenceRemover;
import com.boazsh.m_i_close.app.geofence.GeofenceRequester;
import com.boazsh.m_i_close.app.geofence.GeofenceUtils;
import com.boazsh.m_i_close.app.geofence.GeofenceWrapper;
import com.boazsh.m_i_close.app.geofence.GeofenceStore;
import com.boazsh.m_i_close.app.helpers.Constants;
import com.boazsh.m_i_close.app.helpers.AutoCompleteTask;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.Geofence;

public class SetTargetActivity extends MICloseBaseActivity {

	private static final long GEOFENCE_EXPIRATION = Geofence.NEVER_EXPIRE;

	private SeekBar mDistanceSeekBar;
	private AutoCompleteTextView mAddressAutoCompleteTextView;
	private TextView mGoTextView;
	private TextView mKilometersTextView;
	
	private String mLocationString;
	private int mSeekBarMultiFactor;
    private GeofenceUtils.REQUEST_TYPE mRequestType;
    private GeofenceStore mGeofenceStore;
    private Geofence mCurrentGeofence;
    private GeofenceRequester mGeofenceRequester;
    private GeofenceRemover mGeofenceRemover;
    private GeofenceStatusReceiver mBroadcastReceiver;
    private IntentFilter mIntentFilter;
	private Address mTargetAddress;
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_set_target);

		mBroadcastReceiver = new GeofenceStatusReceiver();
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(GeofenceUtils.ACTION_GEOFENCES_ADDED);
        mIntentFilter.addAction(GeofenceUtils.ACTION_GEOFENCE_ERROR);
        
        mTargetAddress = null;
        mGeofenceStore = new GeofenceStore(this);
        mCurrentGeofence = null;
        mGeofenceRequester = new GeofenceRequester(this);
        mGeofenceRemover = new GeofenceRemover(this);
		mLocationString = "";
		
		int seekBarInitValue = getIntegerResource(R.integer.seekBar_init_value);
		mSeekBarMultiFactor = getIntegerResource(R.integer.seekBar_multi_factor);
		
		mDistanceSeekBar = (SeekBar) findViewById(R.id.targetDistanceSeekBar);
		mGoTextView = (TextView) findViewById(R.id.goTextView);
		mAddressAutoCompleteTextView = (AutoCompleteTextView) findViewById(R.id.targetAddressEditText);
		mKilometersTextView = (TextView) findViewById(R.id.metersNumberTextView);
		
		String lng = Locale.getDefault().getLanguage();
		if (lng.equals("iw")) {
			
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
                            mGeofenceStore.clearGeofence(GeofenceWrapper.GEOFENCE_ID);
                        }
                        
                        break;

                    default:

                        Log.d(GeofenceUtils.APPTAG, getString(R.string.no_resolution));
                        //TODO: Show error!s
                }

            default:

               Log.d(GeofenceUtils.APPTAG,
                       getString(R.string.unknown_activity_request_code, requestCode));
             //TODO: Show error!

               break;
        }
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
				
				if (!isLocationAvailable()) {
					
					return;
				}

				mLocationString = mAddressAutoCompleteTextView.getText().toString();
				
				if (mLocationString.equals("")) {
	    			
	    			showToast(R.string.you_need_to_type_address, true);
	    			return;
	    		}

				GeocodeTask geocodeTask = new GeocodeTask();
				geocodeTask.execute();
				
				boolean result = false;
				
				try {
					result = geocodeTask.get(40, TimeUnit.SECONDS);
					
				} catch (TimeoutException e) {
					
					showToast(R.string.connection_timeout, true);
					return;
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ExecutionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				if (result == false || mTargetAddress == null) {

					return;
				}

				int targetDistance = (mDistanceSeekBar.getProgress() + 1)
						* mSeekBarMultiFactor;

		        mRequestType = GeofenceUtils.REQUEST_TYPE.ADD;

		        if (!servicesConnected()) {

		        	//TODO: Show error
		            return;
		        }

				mPreferencesEditor.clear();
				mPreferencesEditor.commit();

				GeofenceWrapper geofenceWrapper = new GeofenceWrapper(
									        		mTargetAddress.getLatitude(),
									        		mTargetAddress.getLongitude(),
									        		targetDistance,
										            GEOFENCE_EXPIRATION,
										            Geofence.GEOFENCE_TRANSITION_ENTER);

		        mGeofenceStore.setGeofence(geofenceWrapper);
		        mCurrentGeofence = geofenceWrapper.getGeofenceObject();
		        
		        try {
		        	
		            mGeofenceRequester.addGeofence(mCurrentGeofence);
		            
		        } catch (UnsupportedOperationException e) {

		            Toast.makeText(SetTargetActivity.this, R.string.add_geofences_already_requested_error,
		                        Toast.LENGTH_LONG).show();
		        }
			}
		};
	}
	
	
	private TextWatcher getAddressAutoCompleteTextViewTextChangedListener() {
		
		return new TextWatcher() {

			public void afterTextChanged(Editable s) {

			}

			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {

			}

			public void onTextChanged(CharSequence s, int start, int before,
					int count) {

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

    
    public class GeocodeTask extends AsyncTask<Void, Integer, Boolean> {

    	
    	private Context mApplicationContext;
    	private Geocoder mGeocoder;
    	List<Address> mAddresses;
		private ProgressDialog pd;

    	
    	@Override
    	protected void onPreExecute() {
    		
    		mApplicationContext = getApplicationContext();
    		mGeocoder = new Geocoder(mApplicationContext);
    		mAddresses = null;
    		
    		pd = new ProgressDialog(SetTargetActivity.this);
			pd.setTitle("Processing...");
			pd.setMessage("Please wait.");
			pd.setCancelable(false);
			pd.setIndeterminate(true);
			pd.show();
    	}
    	
    	@Override
    	protected Boolean doInBackground(Void... params) {

    		try {
    			mAddresses = mGeocoder.getFromLocationName(mLocationString, 1);

    		} catch (IllegalArgumentException e) {

    			Log.e(Constants.APP_LOG_TAG, "Input address is not valid");
    			showToast(R.string.internal_error, true);
    			return false;

    		} catch (IOException e) {

    			Log.e(Constants.APP_LOG_TAG, "Network connection is not available");
    			Log.e(Constants.APP_LOG_TAG, e.getStackTrace().toString());
    			showToast(R.string.no_internet_connection, true);
    			return false;
    		}

    		if (mAddresses.size() == 0) {
    			showToast(R.string.adress_not_found, true);
    			return false;
    		}

    		mTargetAddress = mAddresses.get(0);
    		return true;
    	}

    	@Override
    	protected void onPostExecute(Boolean result) {
    		
    		if (pd!=null) {
    			
				pd.dismiss();
			}
    		
    	}
    	
    	protected void showToast(int stringId, boolean isLong) {
    		
    		Toast.makeText(mApplicationContext, mApplicationContext.getResources().getString(stringId), 
    				isLong ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show();
    	}
    }
}
