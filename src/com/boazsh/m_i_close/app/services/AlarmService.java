package com.boazsh.m_i_close.app.services;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.boazsh.m_i_close.app.R;
import com.boazsh.m_i_close.app.activities.AlarmActivity;
import com.boazsh.m_i_close.app.geofence.GeofenceUtils;
import com.boazsh.m_i_close.app.geofence.LocationServiceErrorMessages;
import com.boazsh.m_i_close.app.geofence.GeofenceStore;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationClient;

public class AlarmService extends Service {

	public static final int NOTIFICATION_ID = 4908;
	public static final String ALARM_SERVICE_INTENT_ACTION = "com.boazsh.m_i_close.app.ALARM_SERVICE_BROADCAST";
	public static final String ALARM_MESSAGE_KEY = "alarm_message";
	
	IBinder mBinder = new LocalBinder();
	private MediaPlayer mPlayer;
	private NotificationManager mNotificationManager;
	protected SharedPreferences mSharedPreferences;
	protected Editor mPreferencesEditor;

	private final BroadcastReceiver mAlarmServiceBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			mPlayer.stop();

			mPreferencesEditor.putBoolean(AlarmActivity.ALARM_STARTED_KEY, false);
			mPreferencesEditor.putBoolean(AlarmActivity.ALARM_DONE_KEY, true);
			mPreferencesEditor.apply();

			mNotificationManager.cancel(NOTIFICATION_ID);

			Intent broadcastAlarmIntent = new Intent(AlarmActivity.ALARM_ACTIVITY_INTENT_ACTION);
			broadcastAlarmIntent.putExtra(ALARM_MESSAGE_KEY, AlarmServiceMessage.ALARM_STOPPED.getValue());
			sendBroadcast(broadcastAlarmIntent);

			stopSelf();
		}
	};
	

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);

		// Create a local broadcast Intent
        Intent broadcastIntent = new Intent();

        // First check for errors
        if (LocationClient.hasError(intent)) {

            // Get the error code
            int errorCode = LocationClient.getErrorCode(intent);

            // Get the error message
            String errorMessage = LocationServiceErrorMessages.getErrorString(this, errorCode);

            // Log the error
            Log.e(GeofenceUtils.APPTAG,
                    getString(R.string.geofence_transition_error_detail, errorMessage)
            );

            //TODO Handle this error in some activity
            // Set the action and error message for the broadcast intent
            broadcastIntent.setAction(GeofenceUtils.ACTION_GEOFENCE_ERROR) //TODO: Change to ==my== action
                           .putExtra(GeofenceUtils.EXTRA_GEOFENCE_STATUS, errorMessage);

            // Broadcast the error *locally* to other components in this app
            LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);

        // If there's no error, get the transition type and create a notification
        } else {
		
		
        	// Get the type of transition (entry or exit)
            int transition = LocationClient.getGeofenceTransition(intent);

            // Test that a valid transition was reported
            if (transition == Geofence.GEOFENCE_TRANSITION_ENTER) {

            	mSharedPreferences = getSharedPreferences(
        				GeofenceStore.SHARED_PREFERENCE_NAME,
                        Context.MODE_PRIVATE);
        		mPreferencesEditor = mSharedPreferences.edit();

        		mPreferencesEditor.putBoolean(AlarmActivity.ALARM_STARTED_KEY, true);
        		mPreferencesEditor.apply();
        		
            	//TODO: Set constant key
            	float distance = intent.getFloatExtra("TARGET_DISTANCE_KEY", -1);
            	//TODO: Check value
            	showNotification(distance);
            	setAlarm();

            	Intent broadcastAlarmIntent = new Intent(AlarmActivity.ALARM_ACTIVITY_INTENT_ACTION);
    			broadcastAlarmIntent.putExtra(ALARM_MESSAGE_KEY, AlarmServiceMessage.ALARM_STARTED.getValue());
    			sendBroadcast(broadcastAlarmIntent);


                Log.d(GeofenceUtils.APPTAG,
                        getString(R.string.geofence_transition_notification_text));

            // An invalid transition was reported
            } else {
                // Always log as an error
                Log.e(GeofenceUtils.APPTAG,
                        getString(R.string.geofence_transition_invalid_type, transition));
            }
        }

		return START_STICKY;
		
        }
	

	@Override
	public void onCreate() {
		super.onCreate();

		mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		IntentFilter filter = new IntentFilter();
		filter.addAction("ALARM_STOPPED_ACTION");
		registerReceiver(mAlarmServiceBroadcastReceiver, filter);
		
		mSharedPreferences = getSharedPreferences(
				GeofenceStore.SHARED_PREFERENCE_NAME,
                Context.MODE_PRIVATE);
		mPreferencesEditor = mSharedPreferences.edit();
	}

	private void setAlarm() {

		Uri alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);

		if (alert == null) {

			alert = RingtoneManager
					.getDefaultUri(RingtoneManager.TYPE_RINGTONE);

			if (alert == null) {

				alert = RingtoneManager
						.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
			}
		}

		mPlayer = MediaPlayer.create(getApplicationContext(), alert);
		mPlayer.start();
	}

	@Override
	public void onDestroy() {

		unregisterReceiver(mAlarmServiceBroadcastReceiver);
		super.onDestroy();
	}

	@SuppressLint("NewApi") 
	private void showNotification(float distance) {

		Intent contentIntent = new Intent(this, AlarmActivity.class);
		Intent stopIntent = new Intent("ALARM_STOPPED_ACTION");

		PendingIntent contentPIntent = PendingIntent.getActivity(this, 345, contentIntent, 0);
		PendingIntent stopPIIntent = PendingIntent.getBroadcast(this, 0, stopIntent, 0);

		int apiLevel = android.os.Build.VERSION.SDK_INT;
		String notificationTitle = getResources().getString(R.string.you_are_getting_close);
		
		Builder notificationBuilder = new Notification.Builder(this)
		.setContentTitle(notificationTitle).setContentText(buildNotificationMessage(distance))
		.setSmallIcon(R.drawable.miclose)
		.setContentIntent(contentPIntent).setAutoCancel(false);
		
		if (apiLevel >= 16) {
			
			notificationBuilder.addAction(R.drawable.miclose_small, "Stop now", stopPIIntent);
		}
		
		mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		Notification notification = notificationBuilder.build();
		mNotificationManager.notify(NOTIFICATION_ID, notification);
	}
    
    private String buildNotificationMessage(float distance) {

		String text = getResources().getString(R.string.your_destination_is_only);
		text += " " + String.valueOf((int) distance) + " ";
		text += getResources().getString(R.string.meters_from_here);
		
		return text;
	}

    

	public class LocalBinder extends Binder {
		public AlarmService getServerInstance() {
			return AlarmService.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

}