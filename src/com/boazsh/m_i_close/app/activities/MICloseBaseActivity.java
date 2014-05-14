package com.boazsh.m_i_close.app.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
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
				isLong ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show();
	}
	
	protected boolean isLocationAvailable() {

		LocationManager lm = null;
		
		boolean gps_enabled = false;
		boolean network_enabled = false;
		
		if (lm == null)
			
			lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		
		try {
			
			gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
			
		} catch (Exception ex) {
			//TODO Handle this
		}
		
		try {
			
			network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
			
		} catch (Exception ex) {
			
			//TODO Handle this
		}

		if (!gps_enabled && !network_enabled) {
			
			Builder dialog = new AlertDialog.Builder(this);
			dialog.setMessage(getResources().getString(
					R.string.gps_network_not_enabled));

			dialog.setPositiveButton(
					getResources().getString(R.string.open_location_settings),
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(
								DialogInterface paramDialogInterface,
								int paramInt) {
							// TODO Auto-generated method stub
							Intent myIntent = new Intent(
									Settings.ACTION_LOCATION_SOURCE_SETTINGS);
							startActivity(myIntent);
							// get gps
						}
					});
			
			dialog.setNegativeButton(getString(R.string.exit),
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(
								DialogInterface paramDialogInterface,
								int paramInt) {
							System.exit(1);

						}
					});
			dialog.show();
			return false;
		}
		
		return true;
	}

}
