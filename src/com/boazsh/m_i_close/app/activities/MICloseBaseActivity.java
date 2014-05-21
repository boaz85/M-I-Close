package com.boazsh.m_i_close.app.activities;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.boazsh.m_i_close.app.R;
import com.boazsh.m_i_close.app.helpers.MICloseStore;
import com.boazsh.m_i_close.app.helpers.MICloseUtils;

public abstract class MICloseBaseActivity extends Activity {
	
	public static final String MICLOSE_LOG_LABEL = "*** MICLOSE ***";
	
	public static final String MICLOSE_PROVIDER = "MICLOSE";
    public static final String TARGET_LOCATION_KEY = "target_location";
    public static final String TARGET_RADIUS_KEY = "target_distance";
    
    public static final String TARGET_LATITUDE_KEY = "target_latitude";
	public static final String TARGET_LONGITUDE_KEY = "target_longitude";
	public static final String TARGET_DISTANCE_KEY = "target_distance";
	
	public static final String ALARM_SET_KEY = "alarm_set";

	public static final String SHARED_PREFERENCE_NAME = "MICLOSE_SHARED_PREFERENCES";

	protected MICloseStore mMICloseStore;
	
	protected SharedPreferences mSharedPreferences;
	protected Editor mPreferencesEditor;
	protected long mLastBackClickTime;
	

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mMICloseStore = new MICloseStore(MICloseBaseActivity.this);
	}
	

	protected void backPressed() {
		
		int doubleTapTimeout = getResources().getInteger(R.integer.double_tap_timeout);
        long currentTime = System.currentTimeMillis();
        
        if ((currentTime - mLastBackClickTime) < doubleTapTimeout) {
        	
        	Log.d(MICloseUtils.APP_LOG_TAG, "Double \"Back\" click detected");
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
				isLong ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show();
	}

}
