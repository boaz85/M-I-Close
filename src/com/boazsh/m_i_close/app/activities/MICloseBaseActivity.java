package com.boazsh.m_i_close.app.activities;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.widget.Toast;

import com.boazsh.m_i_close.app.R;
import com.boazsh.m_i_close.app.geofence.GeofenceStore;

public abstract class MICloseBaseActivity extends Activity {
	
	public static final String MICLOSE_LOG_LABEL = "*** MICLOSE ***";
	
	public static final String MICLOSE_PROVIDER = "MICLOSE";
    public static final String TARGET_LOCATION_KEY = "target_location";
    public static final String TARGET_RADIUS_KEY = "target_distance";
    
    public static final String TARGET_LATITUDE_KEY = "target_latitude";
	public static final String TARGET_LONGITUDE_KEY = "target_longitude";
	public static final String TARGET_DISTANCE_KEY = "target_distance";
	
	public static final String ALARM_SET_KEY = "alarm_set";

	protected SharedPreferences mSharedPreferences;
	protected Editor mPreferencesEditor;
	protected long mLastBackClickTime;
	

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initSharedPreferences();
	}
	
	protected void initSharedPreferences() {
		
		mSharedPreferences = getSharedPreferences(
				GeofenceStore.SHARED_PREFERENCE_NAME,
                Context.MODE_PRIVATE);
		mPreferencesEditor = mSharedPreferences.edit();
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
	
	
	protected void showToast(int stringId, boolean isLong) {
		
		Toast.makeText(getApplicationContext(), getResources().getString(stringId), 
				isLong ? Toast.LENGTH_SHORT : Toast.LENGTH_SHORT).show();
	}
	
	

}
