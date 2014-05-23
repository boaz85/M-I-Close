package com.boazsh.m_i_close.app.helpers;

import com.boazsh.m_i_close.app.R;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.provider.Settings;
import android.util.Log;


public class MICloseUtils {

	
	public static final int PROXIMITY_ALERT_INTENT_ID = 654;
	public static final int LOCATION_SERVICE_INTENT_ID = 543;
	public static final int NOTIFICATION_CONTENT_INTENT_ID = 432;
	
	public static final int NOTIFICATION_ID = 4908;
	
	public static final String MICLOSE_DUMMY_PROVIDER = "MICLOSE_PROVIDER";
	
	public static final String ALARM_START_INTENT = "com.boazsh.m_i_close_app.ALARM_START";
	public static final String ALARM_STOP_INTENT = "com.boazsh.m_i_close_app.ALARM_STOP";
	public static final String ALARM_CHANGE_INTENT = "com.boazsh.m_i_close.app.ALARM_CHANGE";
	
	public static final long VIBRATION_PATTERN[] = new long[]{0,1000, 1000};
	
	public static final String APP_LOG_TAG = "### M-I-Close ###";
	

	public static PendingIntent getProximityPendingIntent(Context context) {

            Intent intent = new Intent(ALARM_START_INTENT);

            return PendingIntent.getBroadcast(
            		context,
                    PROXIMITY_ALERT_INTENT_ID,
                    intent,
                    PendingIntent.FLAG_CANCEL_CURRENT);
    }
	
	public static boolean isNetworkAvailable(final Context context) {
		
	    ConnectivityManager connectivityManager 
	          = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
	    
	    if (activeNetworkInfo == null || !activeNetworkInfo.isConnected()) {
	    
	    	Log.w(MICloseUtils.APP_LOG_TAG, "No network connection available!");
		    Builder dialog = new AlertDialog.Builder(context);
			dialog.setMessage(context.getResources().getString(
					R.string.no_network_connection));
	
			
			dialog.setNegativeButton(context.getString(R.string.exit),
					new DialogInterface.OnClickListener() {
	
						@Override
						public void onClick(
								DialogInterface paramDialogInterface,
								int paramInt) {
							
							Log.d(MICloseUtils.APP_LOG_TAG, "User chose to exit");
							System.exit(1);
	
						}
					});
			dialog.show();
			return false;
	    }
	    
	    return true;
	}
	
	
	public static boolean isLocationAvailable(final Context context) {

		LocationManager lm = null;
		
		boolean gps_enabled = false;
		boolean network_enabled = false;
		
		if (lm == null)
			
			lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		
		try {
			
			gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
			
		} catch (Exception ex) {
			Log.e(MICloseUtils.APP_LOG_TAG, "isProviderEnabled failed!");
		}
		
		try {
			
			network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
			
		} catch (Exception ex) {
			Log.e(MICloseUtils.APP_LOG_TAG, "isProviderEnabled failed!");
		}

		if (!gps_enabled && !network_enabled) {
			
			Log.w(MICloseUtils.APP_LOG_TAG, "None of the location providers is enabled");
			
			Builder dialog = new AlertDialog.Builder(context);
			dialog.setMessage(context.getResources().getString(
					R.string.gps_network_not_enabled));

			dialog.setPositiveButton(
					context.getResources().getString(R.string.open_location_settings),
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(
								DialogInterface paramDialogInterface,
								int paramInt) {

							Log.d(MICloseUtils.APP_LOG_TAG, "User refered to location settings");
							Intent myIntent = new Intent(
									Settings.ACTION_LOCATION_SOURCE_SETTINGS);
							context.startActivity(myIntent);
							// get gps
						}
					});
			
			dialog.setNegativeButton(context.getString(R.string.exit),
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(
								DialogInterface paramDialogInterface,
								int paramInt) {
							
							Log.d(MICloseUtils.APP_LOG_TAG, "User chose to exit");
							System.exit(1);

						}
					});
			dialog.show();
			return false;
		}
		
		return true;
	}
	
}
