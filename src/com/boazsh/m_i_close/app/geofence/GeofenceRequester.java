package com.boazsh.m_i_close.app.geofence;

import com.boazsh.m_i_close.app.R;
import com.boazsh.m_i_close.app.services.AlarmService;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationStatusCodes;
import com.google.android.gms.location.LocationClient.OnAddGeofencesResultListener;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;


public class GeofenceRequester
                implements
                    OnAddGeofencesResultListener,
                    ConnectionCallbacks,
                    OnConnectionFailedListener {

    private final Activity mActivity;
    private PendingIntent mGeofencePendingIntent;
    private Geofence mCurrentGeofence;
    private LocationClient mLocationClient;
    private boolean mInProgress;

    public GeofenceRequester(Activity activityContext) {

        mActivity = activityContext;
        mGeofencePendingIntent = null;
        mLocationClient = null;
        mInProgress = false;
    }

    
    public void setInProgressFlag(boolean flag) {

        mInProgress = flag;
    }


    public boolean getInProgressFlag() {
        return mInProgress;
    }

    
    public PendingIntent getRequestPendingIntent() {
        return createRequestPendingIntent();
    }

    
    public void addGeofence(Geofence geofence) throws UnsupportedOperationException {

        mCurrentGeofence = geofence;

        if (!mInProgress) {

            mInProgress = true;
            getLocationClient().connect();

        } else {

            throw new UnsupportedOperationException();
        }
    }


    private GooglePlayServicesClient getLocationClient() {
    	
        if (mLocationClient == null) {

            mLocationClient = new LocationClient(mActivity, this, this);
        }
        
        return mLocationClient;
    }
    

    @Override
    public void onAddGeofencesResult(int statusCode, String[] geofenceRequestIds) {

        Intent broadcastIntent = new Intent();
        String msg;

        if (LocationStatusCodes.SUCCESS == statusCode) {

            msg = mActivity.getString(R.string.add_geofences_result_success,
                    Arrays.toString(geofenceRequestIds));

            Log.d(GeofenceUtils.APPTAG, msg);
            broadcastIntent.setAction(GeofenceUtils.ACTION_GEOFENCES_ADDED)
                           .putExtra(GeofenceUtils.EXTRA_GEOFENCE_STATUS, msg);

        } else {

            msg = mActivity.getString(
                    R.string.add_geofences_result_failure,
                    statusCode,
                    Arrays.toString(geofenceRequestIds)
            );

            Log.e(GeofenceUtils.APPTAG, msg);
            broadcastIntent.setAction(GeofenceUtils.ACTION_GEOFENCE_ERROR)
                           .putExtra(GeofenceUtils.EXTRA_GEOFENCE_STATUS, msg);
        }

        LocalBroadcastManager.getInstance(mActivity).sendBroadcast(broadcastIntent);
        requestDisconnection();
    }


    private void requestDisconnection() {

        mInProgress = false;
        getLocationClient().disconnect();
    }


    @Override
    public void onConnected(Bundle arg0) {

        Log.d(GeofenceUtils.APPTAG, mActivity.getString(R.string.connected));

        mGeofencePendingIntent = createRequestPendingIntent();
        ArrayList<Geofence> geofenceList = new ArrayList<Geofence>();
        geofenceList.add(mCurrentGeofence);
        mLocationClient.addGeofences(geofenceList, mGeofencePendingIntent, this);
    }


    @Override
    public void onDisconnected() {

        mInProgress = false;

        Log.d(GeofenceUtils.APPTAG, mActivity.getString(R.string.disconnected));

        mLocationClient = null;
    }


    private PendingIntent createRequestPendingIntent() {

        if (null != mGeofencePendingIntent) {

            return mGeofencePendingIntent;

        } else {

            Intent intent = new Intent(mActivity, AlarmService.class);
            
            Location currentLocation = mLocationClient.getLastLocation();
            GeofenceStore target = new GeofenceStore(mActivity);
            Location targetLocation = target.getSimpleGeofenceLocation();
            
            float distance = currentLocation.distanceTo(targetLocation);
            intent.putExtra("TARGET_DISTANCE_KEY", distance);
            
            return PendingIntent.getService(
                    mActivity,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
        }
    }


    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

        mInProgress = false;

        if (connectionResult.hasResolution()) {

            try {

                connectionResult.startResolutionForResult(mActivity,
                    GeofenceUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST);

            } catch (SendIntentException e) {

                e.printStackTrace();
            }

        } else {

            Intent errorBroadcastIntent = new Intent(GeofenceUtils.ACTION_CONNECTION_ERROR);
            errorBroadcastIntent.putExtra(GeofenceUtils.EXTRA_CONNECTION_ERROR_CODE,
                                        connectionResult.getErrorCode());
            
            LocalBroadcastManager.getInstance(mActivity).sendBroadcast(errorBroadcastIntent);
        }
    }
}
