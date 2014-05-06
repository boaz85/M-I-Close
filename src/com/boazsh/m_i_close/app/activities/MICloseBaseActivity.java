package com.boazsh.m_i_close.app.activities;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.boazsh.m_i_close.app.R;
import com.boazsh.m_i_close.app.services.LocationService; //boaz

public abstract class MICloseBaseActivity extends Activity {
	
	public static final String MICLOSE_LOG_LABEL = "*** MICLOSE ***";
	
	public static final String MICLOSE_PROVIDER = "MICLOSE";
    public static final String TARGET_LOCATION_KEY = "target_location";
    public static final String TARGET_RADIUS_KEY = "target_distance";
    
    public static final String TARGET_LATITUDE_KEY = "target_latitude";
	public static final String TARGET_LONGITUDE_KEY = "target_longitude";
	public static final String TARGET_DISTANCE_KEY = "target_distance";
	
	public static final String ALARM_SET_KEY = "alarm_set";
	
	private static final double RANGE = 5.0;
	private static final double MAX_ZOOM_MINUS_TWO = 13;
	private static final double DIV_FACTOR = 10000;

	protected SharedPreferences mSharedPreferences;
	protected Editor mPreferencesEditor;
	protected long mLastBackClickTime;
	
	static void setVisible() {
		
		LocationService.appIsVisible = true;
	}
	
	static void setInvisible() {
		
		LocationService.appIsVisible = true;
	}
	
	@Override
	protected void onResume() {
	  super.onResume();
	  setVisible();
	}

	@Override
	protected void onPause() {
	  super.onPause();
	  setInvisible();
	}
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initSharedPreferences();
        mLastBackClickTime = 0;
	}
	
	public static Location createLocationObject(double latitude, double longitude) {
    	
    	Location location = new Location(MICLOSE_PROVIDER);
    	location.setLatitude(latitude);
    	location.setLongitude(longitude);
    	
    	return location;
    }
	
	protected void initSharedPreferences() {
		
		mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		mPreferencesEditor = mSharedPreferences.edit();
	}
	
	protected void clearSharedPreferences() {
		
		if (mSharedPreferences == null) {
			
			initSharedPreferences();
		}
		
		mPreferencesEditor.clear();
		mPreferencesEditor.apply();
	}
	
	protected void backPressed() {
		
		int doubleTapTimeout = getResources().getInteger(R.integer.double_tap_timeout);
        long currentTime = System.currentTimeMillis();
        
        if ((currentTime - mLastBackClickTime) < doubleTapTimeout) {
        	
        	mLastBackClickTime = currentTime;
        	moveTaskToBack(true);
        	
        } else {
        	
        	showToast(R.string.press_back_again, false);
        	mLastBackClickTime = currentTime;
        }
	}
	
	protected int getIntegerResource(int id) {
		
        return getResources().getInteger(id);
    }
	
	protected GoogleMap createMapObject(int mapFragmentId, double latitude, double longitude, int targetDistance) {

        GoogleMap map = ((MapFragment) getFragmentManager().findFragmentById(mapFragmentId)).getMap();
        map.setMyLocationEnabled(true);
        LatLng userLatLng = new LatLng(latitude, longitude);

        int radiusStrokeColor =     getIntegerResource(R.color.yellow);
        int radiusFillColor =       getIntegerResource(R.color.light_blue_trans);

        CircleOptions circleOptions = new CircleOptions();
        circleOptions.fillColor(radiusFillColor).center(userLatLng).strokeWidth(2);
        circleOptions.strokeColor(radiusStrokeColor).radius(targetDistance);
        map.addCircle(circleOptions);

        CameraUpdate center = CameraUpdateFactory.newLatLng(userLatLng);
        map.moveCamera(center);

        double zoomValue = (MAX_ZOOM_MINUS_TWO - ((RANGE / DIV_FACTOR) * targetDistance)) + 2;
        
        CameraUpdate zoom = CameraUpdateFactory.zoomTo((float) zoomValue);
        map.animateCamera(zoom);
        
        return map;
    }
	
	protected void showToast(int stringId, boolean isLong) {
		
		Toast.makeText(getApplicationContext(), getResources().getString(stringId), 
				isLong ? Toast.LENGTH_SHORT : Toast.LENGTH_SHORT).show();
	}
	
	

}
