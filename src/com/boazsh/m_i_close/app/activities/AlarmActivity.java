package com.boazsh.m_i_close.app.activities;

import com.boazsh.m_i_close.app.R;
import com.boazsh.m_i_close.app.helpers.MICloseUtils;
import com.boazsh.m_i_close.app.services.AlarmService;
import com.boazsh.m_i_close.app.services.AlarmServiceMessage;
import com.boazsh.m_i_close.app.services.LocationService;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;


public class AlarmActivity extends MICloseBaseActivity {

	public static final String BACKUP_FRAGMENT_TAG = "BACKUP";
	
	private static final double RANGE = 5.0;
	private static final double MAX_ZOOM_MINUS_TWO = 13;
	private static final double DIV_FACTOR = 10000;

	private TextView mStopAlarmTextView;
	private TextView mNewAlarmTextView;
	private TextView mCancelAlarmTextView;

	private boolean mIsAlarmOn;
	private boolean mIsAlarmDone;

	private final BroadcastReceiver mAlarmActivitytBroadcastReceiver = new AlarmChangeBroadcastReceiver();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_alarm);

		Log.d(MICloseUtils.APP_LOG_TAG, "AlarmActivity was started");
		
		mStopAlarmTextView = (TextView) findViewById(R.id.stopAlarmTextView);
		mNewAlarmTextView = (TextView) findViewById(R.id.newAlarmTextView);
		mCancelAlarmTextView = (TextView) findViewById(R.id.cancelAlarmTextView);

		FragmentManager fm = getFragmentManager();
        BackUpFragment backUpFragment = (BackUpFragment) fm.findFragmentByTag(BACKUP_FRAGMENT_TAG);

        // Backup store data for the case when activity recreated after
        // geofence store removed (on orientation change for example).
        if (backUpFragment == null) {
            // add the fragment
        	Log.d(MICloseUtils.APP_LOG_TAG, "Orientation change was happen. Backing up store data");
        	backUpFragment = new BackUpFragment();
            
            backUpFragment.setData(	mMICloseStore.getTargetDistance(), 
            						mMICloseStore.getTargetLatitude(), 
            						mMICloseStore.getTargetLongitude());
            
            fm.beginTransaction().add(backUpFragment, BACKUP_FRAGMENT_TAG).commit();
        }

        int distance = backUpFragment.getDistance();
		createMapObject(backUpFragment.getLatitude(), backUpFragment.getLongitude(), distance);

		
		/*
		 * "Alarm Cancel" button clicked.
		 */
		mCancelAlarmTextView.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {

				Log.d(MICloseUtils.APP_LOG_TAG, "\"Alarm Cancel\" was clicked");
				
				removeTargetStore();
				
				//Stop AlarmManager Proximity requests schedule
				Intent locationStopIntent = new Intent(AlarmActivity.this, LocationService.class);
				Log.d(MICloseUtils.APP_LOG_TAG, "Stopping AlarmManager proximity requests");
				AlarmService.stopProximityAlarmSchedule(AlarmActivity.this, locationStopIntent);
	
				//Make sure there is no proximity request left
				LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        		Log.d(MICloseUtils.APP_LOG_TAG, "Removing proximity alert");
        		locationManager.removeProximityAlert(MICloseUtils.getProximityPendingIntent(AlarmActivity.this));
        		
				//Stop LocationService itself
        		Log.d(MICloseUtils.APP_LOG_TAG, "Stopping location service");
        		stopService(locationStopIntent);
  
				showToast(R.string.alarm_canceled, true);
				Log.d(MICloseUtils.APP_LOG_TAG, "Starting SetTargetActivity");
				startActivity(new Intent(AlarmActivity.this, SetTargetActivity.class));
			}
		});

		mNewAlarmTextView.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {

				Log.d(MICloseUtils.APP_LOG_TAG, "\"New Alarm\" was clicked");
				Intent setNewAlarmIntent = new Intent(AlarmActivity.this, SetTargetActivity.class);
				
				Log.d(MICloseUtils.APP_LOG_TAG, "Starting SetTargetActivity");
				startActivity(setNewAlarmIntent);
			}
		});

		mStopAlarmTextView.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {

				Log.d(MICloseUtils.APP_LOG_TAG, "\"Stop Alarm\" was clicked");
				// Stop alarm service
				Intent stopAlarmIntent = new Intent(MICloseUtils.ALARM_STOP_INTENT);
				
				removeTargetStore();
				
				Log.d(MICloseUtils.APP_LOG_TAG, "Sending ALARM_STOP_INTENT");
				sendBroadcast(stopAlarmIntent);
			}
		});
	}

	@Override
	public void onResume() {
		super.onResume();

		IntentFilter filter = new IntentFilter();
		filter.addAction(MICloseUtils.ALARM_CHANGE_INTENT);
		registerReceiver(mAlarmActivitytBroadcastReceiver, filter);

		mIsAlarmOn = mMICloseStore.isAlarmStarted();
		mIsAlarmDone = mMICloseStore.isAlarmDone();

		if (mIsAlarmOn) {

			mCancelAlarmTextView.setVisibility(View.INVISIBLE);
			mNewAlarmTextView.setVisibility(View.INVISIBLE);

			popIn(mStopAlarmTextView, true);

		} else if (mIsAlarmDone) {

			mCancelAlarmTextView.setVisibility(View.INVISIBLE);
			mStopAlarmTextView.setVisibility(View.INVISIBLE);
			
			popIn(mNewAlarmTextView, false);

		} else {

			mNewAlarmTextView.setVisibility(View.INVISIBLE);
			mStopAlarmTextView.setVisibility(View.INVISIBLE);
			
			popIn(mCancelAlarmTextView, true);
		}
	}

	@Override
	public void onDestroy() {
		unregisterReceiver(mAlarmActivitytBroadcastReceiver);

		super.onDestroy();
	}

	@Override
	public void onBackPressed() {

		backPressed();
	}
	
	private void removeTargetStore() {
		
		mMICloseStore.removeTargetLatitude()
					 .removeTargetLongitude()
					 .removeTargetDistance()
					 .commit();
	}

	private void popIn(View view, boolean isUp) {

		view.setVisibility(View.VISIBLE);
		Animation popIn = AnimationUtils.loadAnimation(AlarmActivity.this,
				isUp ? R.anim.top_button_in : R.anim.bottom_button_in);
		view.startAnimation(popIn);
	}

	private void popOut(View view) {

		Animation popOut = AnimationUtils.loadAnimation(AlarmActivity.this,
				R.anim.top_button_out);
		view.startAnimation(popOut);
		view.setVisibility(View.INVISIBLE);
	}
	
	protected GoogleMap createMapObject(double latitude, double longitude, int distance) {

		Log.d(MICloseUtils.APP_LOG_TAG, "Start building map");
        GoogleMap map = ((MapFragment) getFragmentManager().findFragmentById(R.id.alarm_map1)).getMap();
        map.setMyLocationEnabled(true);
        LatLng userLatLng = new LatLng(latitude, longitude);

        int radiusStrokeColor =     getIntegerResource(R.color.radius_stroke);
        int radiusFillColor =       getIntegerResource(R.color.radius_fill);

        CircleOptions circleOptions = new CircleOptions();
        circleOptions.fillColor(radiusFillColor).center(userLatLng).strokeWidth(2);
        circleOptions.strokeColor(radiusStrokeColor).radius(distance);
        map.addCircle(circleOptions);
        
        String yourTarget = getResources().getString(R.string.your_target);
        MarkerOptions markerOptions = new MarkerOptions().position(userLatLng).title(yourTarget);
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));
        map.addMarker(markerOptions);

        CameraUpdate center = CameraUpdateFactory.newLatLng(userLatLng);
        map.moveCamera(center);

        double zoomValue = (MAX_ZOOM_MINUS_TWO - ((RANGE / DIV_FACTOR) * distance)) + 2;
        
        CameraUpdate zoom = CameraUpdateFactory.zoomTo((float) zoomValue);
        map.animateCamera(zoom);
        
        return map;
    }
	
	
	public class AlarmChangeBroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {

			Log.d(MICloseUtils.APP_LOG_TAG, "AlarmChangeBroadcastReceiver was started");
			
			int alarmMessageValue = intent.getIntExtra(AlarmService.ALARM_MESSAGE_KEY, -1);

			AlarmServiceMessage alarmServiceMessage = AlarmServiceMessage
					.valueOf(alarmMessageValue);

			switch (alarmServiceMessage) {

			case ALARM_STARTED:

				popOut(mCancelAlarmTextView);
				popIn(mStopAlarmTextView, true);

				break;

			case ALARM_STOPPED:

				//TODO Make the remover data member
				
				removeTargetStore();
				popOut(mStopAlarmTextView);
				popIn(mNewAlarmTextView, false);

				break;

			default:
				// TODO: Error....
				break;
			}
		}
	}
	
	public static class BackUpFragment extends Fragment {

	    private int mTargetDistance;
	    private double mLongitude;
	    private double mLatitude;

	    @Override
	    public void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);

	        setRetainInstance(true);
	    }

	    public void setData(int distance, double latitude, double longitude) {
	    	mTargetDistance = distance;
	    	mLatitude = latitude;
	    	mLongitude = longitude;
	    }

	    public int getDistance() {
	        return mTargetDistance;
	    }
	    
	    public double getLatitude() {
	        return mLatitude;
	    }
	    
	    public double getLongitude() {
	        return mLongitude;
	    }
	}
}
