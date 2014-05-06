package com.boazsh.m_i_close.app.activities;

import com.boazsh.m_i_close.app.R;
import com.boazsh.m_i_close.app.services.AlarmService;
import com.boazsh.m_i_close.app.services.AlarmServiceMessage;
import com.boazsh.m_i_close.app.services.LocationService;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;

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
	
	private static final double RANGE = 5.0;
	private static final double MAX_ZOOM_MINUS_TWO = 13;
	private static final double DIV_FACTOR = 10000;

	private TextView mStopAlarm_TextView;
	private TextView mNewAlarm_TextView;
	private TextView mCancelAlarm_TextView;

	private boolean mIsAlarmOn;
	private boolean mIsAlarmDone;
	private int mTargetDistance;

	private final BroadcastReceiver mAlarmActivitytBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			int alarmMessageValue = intent.getIntExtra(AlarmService.ALARM_MESSAGE_KEY, -1);

			AlarmServiceMessage alarmServiceMessage = AlarmServiceMessage
					.valueOf(alarmMessageValue);

			switch (alarmServiceMessage) {

			case ALARM_STARTED:

				popOut(mCancelAlarm_TextView);
				popIn(mStopAlarm_TextView, true);

				break;

			case ALARM_STOPPED:

				popOut(mStopAlarm_TextView);
				popIn(mNewAlarm_TextView, false);

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
		setContentView(R.layout.activity_alarm1);

		mStopAlarm_TextView = (TextView) findViewById(R.id.stopAlarmTextView);
		mNewAlarm_TextView = (TextView) findViewById(R.id.newAlarmTextView);
		mCancelAlarm_TextView = (TextView) findViewById(R.id.cancelAlarmTextView);

		double latitude = mSharedPreferences.getFloat(TARGET_LATITUDE_KEY, -100);
		double longitude = mSharedPreferences.getFloat(TARGET_LONGITUDE_KEY,-100);
		mTargetDistance = mSharedPreferences.getInt(TARGET_DISTANCE_KEY, -1);
		// TODO: Validate values!

		createMapObject(latitude, longitude);

		/*
		 * "Alarm Cancel" button clicked.
		 */
		mCancelAlarm_TextView.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {

				clearSharedPreferences();
				showToast(R.string.alarm_canceled, true);
				stopService(new Intent(AlarmActivity.this, LocationService.class));
				startActivity(new Intent(AlarmActivity.this, MainActivity.class));
			}
		});

		mNewAlarm_TextView.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {

				Intent setNewAlarmIntent = new Intent(AlarmActivity.this, SetTargetActivity.class);
				startActivity(setNewAlarmIntent);
			}
		});

		mStopAlarm_TextView.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {

				// Stop alarm service
				Intent stopAlarmIntent = new Intent(ALARM_ACTIVITY_INTENT_ACTION);
				sendBroadcast(stopAlarmIntent);

				// Stop location service
				Intent stopServiceIntent = new Intent(AlarmActivity.this, LocationService.class);
				stopService(stopServiceIntent);

				popOut(mStopAlarm_TextView);
				popIn(mNewAlarm_TextView, false);
			}
		});
	}

	@Override
	public void onResume() {
		super.onResume();

		IntentFilter filter = new IntentFilter();
		filter.addAction(AlarmService.ALARM_SERVICE_INTENT_ACTION);
		registerReceiver(mAlarmActivitytBroadcastReceiver, filter);

		mIsAlarmOn = mSharedPreferences.getBoolean(ALARM_STARTED_KEY, false);
		mIsAlarmDone = mSharedPreferences.getBoolean(ALARM_DONE_KEY, false);

		if (mIsAlarmOn) {

			mCancelAlarm_TextView.setVisibility(View.INVISIBLE);
			mNewAlarm_TextView.setVisibility(View.INVISIBLE);

			popIn(mStopAlarm_TextView, true);

		} else if (mIsAlarmDone) {

			mCancelAlarm_TextView.setVisibility(View.INVISIBLE);
			mStopAlarm_TextView.setVisibility(View.INVISIBLE);
			
			popIn(mNewAlarm_TextView, false);

		} else {

			mNewAlarm_TextView.setVisibility(View.INVISIBLE);
			mStopAlarm_TextView.setVisibility(View.INVISIBLE);
			
			popIn(mCancelAlarm_TextView, true);
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

        CameraUpdate center = CameraUpdateFactory.newLatLng(userLatLng);
        map.moveCamera(center);

        double zoomValue = (MAX_ZOOM_MINUS_TWO - ((RANGE / DIV_FACTOR) * mTargetDistance)) + 2;
        
        CameraUpdate zoom = CameraUpdateFactory.zoomTo((float) zoomValue);
        map.animateCamera(zoom);
        
        return map;
    }
}
