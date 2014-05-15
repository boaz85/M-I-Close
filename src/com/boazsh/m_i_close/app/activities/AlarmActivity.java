package com.boazsh.m_i_close.app.activities;

import com.boazsh.m_i_close.app.R;
import com.boazsh.m_i_close.app.geofence.GeofenceRemover;
import com.boazsh.m_i_close.app.services.AlarmService;
import com.boazsh.m_i_close.app.geofence.GeofenceWrapper;
import com.boazsh.m_i_close.app.geofence.GeofenceStore;
import com.boazsh.m_i_close.app.services.AlarmServiceMessage;
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
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;


public class AlarmActivity extends MICloseBaseActivity {

	public static final String ALARM_ACTIVITY_INTENT_ACTION = "com.boazsh.m_i_close.app.ALARM_ACTIVITY_BROADCAST";
	public static final String ALARM_STARTED_KEY = "alarm_started";
	public static final String ALARM_DONE_KEY = "alarm_done";
	public static final String BACKUP_FRAGMENT_TAG = "BACKUP";
	
	private static final double RANGE = 5.0;
	private static final double MAX_ZOOM_MINUS_TWO = 13;
	private static final double DIV_FACTOR = 10000;

	private TextView mStopAlarmTextView;
	private TextView mNewAlarmTextView;
	private TextView mCancelAlarmTextView;

	private boolean mIsAlarmOn;
	private boolean mIsAlarmDone;
	private int mTargetDistance;
	
	private GeofenceStore mGeofenceStore;

	private final BroadcastReceiver mAlarmActivitytBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

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
				GeofenceRemover remover = new GeofenceRemover(AlarmActivity.this);
				remover.setInProgressFlag(false);
				remover.removeGeofencesById(GeofenceWrapper.GEOFENCE_ID);
				mGeofenceStore.clearGeofence(GeofenceWrapper.GEOFENCE_ID);
				popOut(mStopAlarmTextView);
				popIn(mNewAlarmTextView, false);

				break;

			default:
				// TODO: Error....
				break;
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_alarm);

		mStopAlarmTextView = (TextView) findViewById(R.id.stopAlarmTextView);
		mNewAlarmTextView = (TextView) findViewById(R.id.newAlarmTextView);
		mCancelAlarmTextView = (TextView) findViewById(R.id.cancelAlarmTextView);

		mGeofenceStore = new GeofenceStore(AlarmActivity.this);
		
		FragmentManager fm = getFragmentManager();
        BackUpFragment backUpFragment = (BackUpFragment) fm.findFragmentByTag(BACKUP_FRAGMENT_TAG);

        // Backup store data for the case when activity recreated after
        // geofence store removed (on orientation change for example).
        if (backUpFragment == null) {
            // add the fragment
        	backUpFragment = new BackUpFragment();
        	
        	double latitude = mGeofenceStore.getGeofence().getLatitude();
    		double longitude = mGeofenceStore.getGeofence().getLongitude();
    		mTargetDistance = (int) mGeofenceStore.getGeofence().getRadius();
            
            backUpFragment.setData(mTargetDistance, latitude, longitude);
            fm.beginTransaction().add(backUpFragment, BACKUP_FRAGMENT_TAG).commit();
        }

        mTargetDistance = backUpFragment.getDistance();
		createMapObject(backUpFragment.getLatitude(), backUpFragment.getLongitude());

		/*
		 * "Alarm Cancel" button clicked.
		 */
		mCancelAlarmTextView.setOnClickListener(new View.OnClickListener() {

			

			@Override
			public void onClick(View arg0) {

				GeofenceRemover remover = new GeofenceRemover(AlarmActivity.this);
				remover.setInProgressFlag(false);
				remover.removeGeofencesById(GeofenceWrapper.GEOFENCE_ID);
				mGeofenceStore.clearGeofence(GeofenceWrapper.GEOFENCE_ID);

				showToast(R.string.alarm_canceled, true);
				startActivity(new Intent(AlarmActivity.this, MainActivity.class));
			}
		});

		mNewAlarmTextView.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {

				Intent setNewAlarmIntent = new Intent(AlarmActivity.this, SetTargetActivity.class);
				startActivity(setNewAlarmIntent);
			}
		});

		mStopAlarmTextView.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {

				// Stop alarm service
				Intent stopAlarmIntent = new Intent("ALARM_STOPPED_ACTION");
				sendBroadcast(stopAlarmIntent);

				GeofenceRemover remover = new GeofenceRemover(AlarmActivity.this);
				remover.setInProgressFlag(false);
				remover.removeGeofencesById(GeofenceWrapper.GEOFENCE_ID);
				mGeofenceStore.clearGeofence(GeofenceWrapper.GEOFENCE_ID);

				popOut(mStopAlarmTextView);
				popIn(mNewAlarmTextView, false);
			}
		});
	}

	@Override
	public void onResume() {
		super.onResume();

		IntentFilter filter = new IntentFilter();
		filter.addAction(ALARM_ACTIVITY_INTENT_ACTION);
		registerReceiver(mAlarmActivitytBroadcastReceiver, filter);

		mIsAlarmOn = mSharedPreferences.getBoolean(ALARM_STARTED_KEY, false);
		mIsAlarmDone = mSharedPreferences.getBoolean(ALARM_DONE_KEY, false);

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
	
	protected GoogleMap createMapObject(double latitude, double longitude) {

        GoogleMap map = ((MapFragment) getFragmentManager().findFragmentById(R.id.alarm_map1)).getMap();
        map.setMyLocationEnabled(true);
        LatLng userLatLng = new LatLng(latitude, longitude);

        int radiusStrokeColor =     getIntegerResource(R.color.yellow);
        int radiusFillColor =       getIntegerResource(R.color.light_blue_trans);

        CircleOptions circleOptions = new CircleOptions();
        circleOptions.fillColor(radiusFillColor).center(userLatLng).strokeWidth(2);
        circleOptions.strokeColor(radiusStrokeColor).radius(mTargetDistance);
        map.addCircle(circleOptions);
        
        
        MarkerOptions markerOptions = new MarkerOptions().position(userLatLng).title("Your Target");
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));
        map.addMarker(markerOptions);

        CameraUpdate center = CameraUpdateFactory.newLatLng(userLatLng);
        map.moveCamera(center);

        double zoomValue = (MAX_ZOOM_MINUS_TWO - ((RANGE / DIV_FACTOR) * mTargetDistance)) + 2;
        
        CameraUpdate zoom = CameraUpdateFactory.zoomTo((float) zoomValue);
        map.animateCamera(zoom);
        
        return map;
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
