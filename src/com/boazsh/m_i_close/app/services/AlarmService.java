package com.boazsh.m_i_close.app.services;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.preference.PreferenceManager;

import com.boazsh.m_i_close.app.R;
import com.boazsh.m_i_close.app.activities.AlarmActivity;
import com.boazsh.m_i_close.app.activities.MICloseBaseActivity;

public class AlarmService extends MICloseBaseService {

	public static final int NOTIFICATION_ID = 4908;
	public static final String ALARM_SERVICE_INTENT_ACTION = "com.boazsh.m_i_close.app.ALARM_SERVICE_BROADCAST";
	public static final String ALARM_MESSAGE_KEY = "alarm_message";

	private MediaPlayer mPlayer;

	private NotificationManager mNotificationManager;

	private final BroadcastReceiver mAlarmServiceBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			mPlayer.stop();

			mPreferencesEditor.putBoolean(AlarmActivity.ALARM_STARTED_KEY, false);
			mPreferencesEditor.putBoolean(AlarmActivity.ALARM_DONE_KEY, true);
			mPreferencesEditor.apply();

			mNotificationManager.cancel(NOTIFICATION_ID);

			Intent broadcastAlarmIntent = new Intent(ALARM_SERVICE_INTENT_ACTION);
			broadcastAlarmIntent.putExtra(ALARM_MESSAGE_KEY, AlarmServiceMessage.ALARM_STOPPED.getValue());
			sendBroadcast(broadcastAlarmIntent);

			stopSelf();
		}
	};
	

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);

		setAlarm();

		mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		mPreferencesEditor = mSharedPreferences.edit();
		mPreferencesEditor.putBoolean(AlarmActivity.ALARM_STARTED_KEY, true);
		mPreferencesEditor.apply();

		Intent broadcastAlarmIntent = new Intent(ALARM_SERVICE_INTENT_ACTION);
		broadcastAlarmIntent.putExtra(ALARM_MESSAGE_KEY, AlarmServiceMessage.ALARM_STARTED.getValue());
		sendBroadcast(broadcastAlarmIntent);

		int targetDistance = intent.getIntExtra(MICloseBaseActivity.TARGET_DISTANCE_KEY, -1);
		// TODO: check value

		showNotification(targetDistance);

		return START_STICKY;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		IntentFilter filter = new IntentFilter();
		filter.addAction(AlarmActivity.ALARM_ACTIVITY_INTENT_ACTION);
		registerReceiver(mAlarmServiceBroadcastReceiver, filter);
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
	private void showNotification(int distance) {

		Intent contentIntent = new Intent(this, AlarmActivity.class);
		Intent stopIntent = new Intent(AlarmActivity.ALARM_ACTIVITY_INTENT_ACTION);

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
		
		Notification notification = notificationBuilder.build();
		mNotificationManager.notify(NOTIFICATION_ID, notification);
	}
	
	private String buildNotificationMessage(int distance) {

		String text = getResources().getString(R.string.your_destination_is_only);
		text += " " + String.valueOf(distance) + " ";
		text += getResources().getString(R.string.meters_from_here);
		
		return text;
	}

}