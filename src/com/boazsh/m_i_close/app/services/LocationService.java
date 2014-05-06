package com.boazsh.m_i_close.app.services;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.boazsh.m_i_close.app.activities.MICloseBaseActivity;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

public class LocationService extends MICloseBaseService implements
		GooglePlayServicesClient.ConnectionCallbacks,
		GooglePlayServicesClient.OnConnectionFailedListener, LocationListener {

	private static final String LOG_LABEL = "#### LOCATION SERVICE ####";
	public static final long UPDATE_INTERVAL = 10000;

	private LocationClient mLocationClient;
	private LocationRequest mLocationRequest;
	private Location mTargetLocation;
	private int mTargetDistance;
	private boolean mInProgress;
	private Boolean servicesAvailable = false;

	public static boolean appIsVisible = true;

	@Override
	public void onCreate() {
		super.onCreate();

		mInProgress = false;
		mTargetLocation = null;
		mTargetDistance = -1;

		servicesAvailable = servicesConnected();

		/*
		 * Create a new location client, using the enclosing class to handle
		 * callbacks.
		 */
		mLocationClient = new LocationClient(this, this, this);

	}
	
	class ConnectTask implements Runnable {

		@Override
		public void run() {

			setUpLocationClientIfNeeded();
			
			if (!mLocationClient.isConnected() || !mLocationClient.isConnecting() && !mInProgress) {
				
				mInProgress = true;
				mLocationClient.connect();
			}
		}


		private void setUpLocationClientIfNeeded() {
			
			if (mLocationClient == null) {
				
				mLocationClient = new LocationClient(	LocationService.this,
														LocationService.this, 
														LocationService.this);
			}
		}

	}

	// TODO: Move this to SetTargetActivity or MainActivity?
	private boolean servicesConnected() {

		// Check that Google Play services is available
		int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

		// If Google Play services is available
		return (ConnectionResult.SUCCESS == resultCode);
	}

	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);

		if (!servicesAvailable || mLocationClient.isConnected() || mInProgress)
			return START_STICKY;

		if (intent != null) {

			mTargetLocation = intent.getParcelableExtra(MICloseBaseActivity.TARGET_LOCATION_KEY);
			mTargetDistance = intent.getIntExtra(MICloseBaseActivity.TARGET_DISTANCE_KEY, -1);

			// TODO: Validate values!
		}

		ConnectTask task = new ConnectTask();
		task.run();

		return START_STICKY;
	}


	@Override
	public void onLocationChanged(Location currentLocation) {

		Log.d(LOG_LABEL, "Location updated. Lat: " + 
				currentLocation.getLatitude() + ", Lon: " + currentLocation.getLongitude());

		int distanceFromTarget = (int) currentLocation.distanceTo(mTargetLocation);

		if ((distanceFromTarget < mTargetDistance)) {

			Log.d(LOG_LABEL, "Distance from target reached:"
					+ distanceFromTarget + " meters.");

			Intent alarmServiceIntent = new Intent(LocationService.this, AlarmService.class);
			alarmServiceIntent.putExtra(MICloseBaseActivity.TARGET_DISTANCE_KEY, distanceFromTarget);
			startService(alarmServiceIntent);

			stopSelf();
		}
	}

	@Override
	public void onDestroy() {
	
		Log.d(LOG_LABEL, "Destroying LocationClient.");

		mInProgress = false;
		
		if (servicesAvailable && mLocationClient != null) {
			
			mLocationClient.removeLocationUpdates(this);
			mLocationClient = null;
		}

		super.onDestroy();
	}


	@Override
	public void onConnected(Bundle bundle) {

		Log.d(LOG_LABEL, "LocationClient connected.");
		
		mLocationRequest = LocationRequest.create();
		mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
		mLocationRequest.setInterval(3000);
		mLocationRequest.setFastestInterval(3000);
		
		mLocationClient.requestLocationUpdates(mLocationRequest, this);
	}


	@Override
	public void onDisconnected() {

		Log.d(LOG_LABEL, "LocationClient disconnected.");

		mInProgress = false;
		mLocationClient = null;
	}


	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {

		Log.w(LOG_LABEL, "LocationClient connection failed");

		mInProgress = false;

		/*
		 * Google Play services can resolve some errors it detects. If the error
		 * has a resolution, try sending an Intent to start a Google Play
		 * services activity that can resolve error.
		 */
		
		//TODO: Implement!
		if (connectionResult.hasResolution()) {

			// If no resolution is available, display an error dialog
		} else {

		}
	}

}