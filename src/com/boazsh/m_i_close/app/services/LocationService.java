package com.boazsh.m_i_close.app.services;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.util.Log;

import com.boazsh.m_i_close.app.R;
import com.boazsh.m_i_close.app.helpers.MICloseStore;
import com.boazsh.m_i_close.app.helpers.MICloseUtils;


public class LocationService extends IntentService {

	public LocationService() {
		super("");
	}

	private LocationManager mLocationManager;
	private PendingIntent mLocationReachedPendingIntent;
	private MICloseStore mMICloseStore;
	
	@Override
    protected void onHandleIntent(Intent workIntent) {

		Log.d(MICloseUtils.APP_LOG_TAG, "LocationService was started");
		
		mMICloseStore = new MICloseStore(LocationService.this);
		int proximityAlertTimeout = getResources().getInteger(R.integer.proximity_request_interval);

		mLocationReachedPendingIntent = MICloseUtils.getProximityPendingIntent(LocationService.this);
		mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		
		mLocationManager.addProximityAlert(
				mMICloseStore.getTargetLatitude(),
				mMICloseStore.getTargetLongitude(),
				mMICloseStore.getTargetDistance(),
				proximityAlertTimeout, 
	            mLocationReachedPendingIntent
	       );
		
		Log.d(MICloseUtils.APP_LOG_TAG, "Proximity alert was added");
    }
}