package com.boazsh.m_i_close.app.services;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.Notification.Builder;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.Vibrator;
import android.util.Log;

import com.boazsh.m_i_close.app.R;
import com.boazsh.m_i_close.app.activities.AlarmActivity;
import com.boazsh.m_i_close.app.helpers.MICloseStore;
import com.boazsh.m_i_close.app.helpers.MICloseUtils;


public class AlarmService extends Service {

	public static final String ALARM_MESSAGE_KEY = "alarm_message";
	
	private IBinder mBinder = new LocalBinder();
	private MediaPlayer mPlayer;
	private NotificationManager mNotificationManager;
	private Vibrator mVibrator;

	private final BroadcastReceiver mAlarmServiceBroadcastReceiver = new AlarmStopBroadcastReceiver();
	private MICloseStore mMICloseStore;

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);

				mMICloseStore.setAlarmStarted(true).commit();
	
        		LocationManager mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        		
        		Location currentLocation;
        		Location targetLocation;
        		float distance;
        		
        		if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
        			
        			currentLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        			
        		} else if (mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
        			
        			currentLocation = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        			
        		} else {
        			
        			currentLocation = null;
        		}

        		if (currentLocation != null) {
        			
	        		targetLocation = new Location(MICloseUtils.MICLOSE_DUMMY_PROVIDER);
	        		
	        		targetLocation.setLatitude(mMICloseStore.getTargetLatitude());
	        		targetLocation.setLongitude(mMICloseStore.getTargetLongitude());
        		
	        		distance = currentLocation.distanceTo(targetLocation);
	        		
        		} else {
        			
        			distance = -1;
        		}

        		
        		//Stop alarm manager invocations
        		Intent locationStopIntent = new Intent(AlarmService.this, LocationService.class);
        		Log.d(MICloseUtils.APP_LOG_TAG, "Stopping AlarmManager proximity requests");
        		stopProximityAlarmSchedule(AlarmService.this, locationStopIntent);
        		
        		//Stop LocationService itself
        		Log.d(MICloseUtils.APP_LOG_TAG, "Stopping location service");
        		stopService(locationStopIntent);
        		
        		//Make sure there is no proximity request left
        		Log.d(MICloseUtils.APP_LOG_TAG, "Removing proximity alert");
        		mLocationManager.removeProximityAlert(MICloseUtils.getProximityPendingIntent(AlarmService.this));
  
            	

            	showNotification(distance);
            	setAlarm();

            	Intent broadcastAlarmIntent = new Intent(MICloseUtils.ALARM_CHANGE_INTENT);
    			broadcastAlarmIntent.putExtra(ALARM_MESSAGE_KEY, AlarmServiceMessage.ALARM_STARTED.getValue());
    			
    			Log.d(MICloseUtils.APP_LOG_TAG, "Sending ALARM_CHANGE_INTENT");
    			sendBroadcast(broadcastAlarmIntent);

    			return START_STICKY;
		
        }
	

	@Override
	public void onCreate() {
		super.onCreate();

		Log.d(MICloseUtils.APP_LOG_TAG, "AlarmService was started");
		
		mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		
		IntentFilter filter = new IntentFilter();
		filter.addAction(MICloseUtils.ALARM_STOP_INTENT);
		registerReceiver(mAlarmServiceBroadcastReceiver, filter);
		
		mMICloseStore = new MICloseStore(AlarmService.this);
	}
	
	public static void stopProximityAlarmSchedule(Context context, Intent alarmIntent) {

		PendingIntent pendingIntent = PendingIntent.getService(	context, 
																MICloseUtils.LOCATION_SERVICE_INTENT_ID, 
																alarmIntent,
																PendingIntent.FLAG_CANCEL_CURRENT);
		
		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		alarmManager.cancel(pendingIntent);
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
		
		mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		mVibrator.vibrate(MICloseUtils.VIBRATION_PATTERN, 1);
	}

	@Override
	public void onDestroy() {

		unregisterReceiver(mAlarmServiceBroadcastReceiver);
		super.onDestroy();
	}

	@SuppressLint("NewApi") 
	private void showNotification(float distance) {

		Intent contentIntent = new Intent(this, AlarmActivity.class);
		Intent stopIntent = new Intent(MICloseUtils.ALARM_STOP_INTENT);

		PendingIntent contentPIntent = PendingIntent.getActivity(	this, 
																	MICloseUtils.NOTIFICATION_CONTENT_INTENT_ID, 
																	contentIntent, 0);
		
		PendingIntent stopPIIntent = PendingIntent.getBroadcast(this, 0, stopIntent, 0);

		int apiLevel = android.os.Build.VERSION.SDK_INT;
		String notificationTitle = getResources().getString(R.string.you_are_getting_close);
		
		Builder notificationBuilder = new Notification.Builder(this)
		.setContentTitle(notificationTitle).setContentText(buildNotificationMessage(distance))
		.setSmallIcon(R.drawable.miclose)
		.setContentIntent(contentPIntent).setAutoCancel(false);
		
		if (apiLevel >= 16) {
			
			String stopAlarm = getResources().getString(R.string.stop_alarm_notification);
			notificationBuilder.addAction(R.drawable.miclose_small, stopAlarm, stopPIIntent);
		}
		
		mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		Notification notification = notificationBuilder.build();
		
		Log.d(MICloseUtils.APP_LOG_TAG, "Showing notification");
		mNotificationManager.notify(MICloseUtils.NOTIFICATION_ID, notification);
	}
    
    private String buildNotificationMessage(float distance) {

    	String text;
    	
    	if (distance == -1) {
    		
    		text = getResources().getString(R.string.your_destination_is_close);
    		
    	} else {
    	
			text = getResources().getString(R.string.your_destination_is_only);
			text += " " + String.valueOf((int) distance) + " ";
			text += getResources().getString(R.string.meters_from_here);
    	}
		
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

	private class AlarmStopBroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {

			Log.d(MICloseUtils.APP_LOG_TAG, "AlarmStopBroadcastReceiver was started");
			
			Log.d(MICloseUtils.APP_LOG_TAG, "Stopping vibration");
			mVibrator.cancel();
			
			Log.d(MICloseUtils.APP_LOG_TAG, "Stopping alarm (sound)");
			mPlayer.stop();

			mMICloseStore.setAlarmStarted(false)
						 .setAlarmDone(true)
						 .setAlarmSet(false)
						 .commit();

			Log.d(MICloseUtils.APP_LOG_TAG, "Canceling notification");
			mNotificationManager.cancel(MICloseUtils.NOTIFICATION_ID);

			Intent broadcastAlarmIntent = new Intent(
					MICloseUtils.ALARM_CHANGE_INTENT);
			
			broadcastAlarmIntent.putExtra(ALARM_MESSAGE_KEY,
					AlarmServiceMessage.ALARM_STOPPED.getValue());
			
			Log.d(MICloseUtils.APP_LOG_TAG, "Sending ALARM_CHANGE_INTENT");
			sendBroadcast(broadcastAlarmIntent);

			Log.d(MICloseUtils.APP_LOG_TAG, "Stopping AlarmService (Self)");
			stopSelf();
		}
	}

}