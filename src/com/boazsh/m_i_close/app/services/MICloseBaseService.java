package com.boazsh.m_i_close.app.services;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;

public abstract class MICloseBaseService extends Service {

	public static final int NOTIFICATION_ID = 4908;
	public static final String ALARM_SERVICE_INTENT_ACTION = "com.boazsh.m_i_close.app.ALARM_SERVICE_BROADCAST";
	public static final String ALARM_MESSAGE_KEY = "alarm_message";

	protected SharedPreferences mSharedPreferences;
	protected Editor mPreferencesEditor;

	IBinder mBinder = new LocalBinder();

	public class LocalBinder extends Binder {
		public MICloseBaseService getServerInstance() {
			return MICloseBaseService.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);

		mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		mPreferencesEditor = mSharedPreferences.edit();

		return START_STICKY;
	}
}