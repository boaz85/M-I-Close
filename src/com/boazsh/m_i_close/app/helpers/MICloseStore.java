package com.boazsh.m_i_close.app.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class MICloseStore {
	
	
	private static final String SHARED_PREFERENCE_NAME = "MIClose_Shared_Preferences";
	
	private static final String TARGET_DISTANCE_KEY = "TARGET_DISTANCE";
	private static final String TARGET_LATITUDE_KEY = "TARGET_LATITUDE";
	private static final String TARGET_LONGITUDE_KEY = "TARGET_LONGITUDE";
	
	private static final String ALARM_SET_KEY = "ALARM_SET";
	private static final String ALARM_STARTED_KEY = "ALARM_STARTED";
	private static final String ALARM_DONE_KEY = "ALARM_DONE";
	
	private static final int DEFAULT_NUM_VALUE = -1;
	
	private SharedPreferences mSharedPreferences;
	private Editor mPreferencesEditor;

	public MICloseStore(Context context) {
		
		mSharedPreferences = context.getSharedPreferences(
    			SHARED_PREFERENCE_NAME,
                Context.MODE_PRIVATE);
		
		mPreferencesEditor = mSharedPreferences.edit();
	}
	
	public void commit() {
		mPreferencesEditor.commit();
	}
	
	public MICloseStore clear() {
		mPreferencesEditor.clear();
		return this;
	}
	
	public int getTargetDistance() {
		return mSharedPreferences.getInt(TARGET_DISTANCE_KEY, DEFAULT_NUM_VALUE);
	}
	
	public float getTargetLatitude() {
		return mSharedPreferences.getFloat(TARGET_LATITUDE_KEY, DEFAULT_NUM_VALUE);
	}
	
	public float getTargetLongitude() {
		return mSharedPreferences.getFloat(TARGET_LONGITUDE_KEY, DEFAULT_NUM_VALUE);
	}
	
	public boolean isAlarmSet() {
		return mSharedPreferences.getBoolean(ALARM_SET_KEY, false);
	}
	
	public boolean isAlarmStarted() {
		return mSharedPreferences.getBoolean(ALARM_STARTED_KEY, false);
	}
	
	public boolean isAlarmDone() {
		return mSharedPreferences.getBoolean(ALARM_DONE_KEY, false);
	}
	
	public MICloseStore setTargetDistance(int distance) {
		
		mPreferencesEditor.putInt(TARGET_DISTANCE_KEY, distance);
		return this;
	}
	
	public MICloseStore setTargetLatitude(double latitude) {
		
		mPreferencesEditor.putFloat(TARGET_LATITUDE_KEY, (float) latitude);
		return this;
	}
	
	public MICloseStore setTargetLongitude(double longitude) {
		
		mPreferencesEditor.putFloat(TARGET_LONGITUDE_KEY, (float) longitude);
		return this;
	}
	
	public MICloseStore setAlarmSet(boolean alarmSet) {
		
		mPreferencesEditor.putBoolean(ALARM_SET_KEY, alarmSet);
		return this;
	}
	
	public MICloseStore setAlarmStarted(boolean alarmStarted) {
		
		mPreferencesEditor.putBoolean(ALARM_STARTED_KEY, alarmStarted);
		return this;
	}
	
	public MICloseStore setAlarmDone(boolean alarmDone) {
		
		mPreferencesEditor.putBoolean(ALARM_DONE_KEY, alarmDone);
		return this;
	}

	public MICloseStore removeTargetDistance() {
		
		mPreferencesEditor.remove(TARGET_DISTANCE_KEY);
		return this;
	}
	
	public MICloseStore removeTargetLatitude() {
		
		mPreferencesEditor.remove(TARGET_LATITUDE_KEY);
		return this;
	}
	
	public MICloseStore removeTargetLongitude() {
		
		mPreferencesEditor.remove(TARGET_LONGITUDE_KEY);
		return this;
	}
	
	public MICloseStore removeAlarmSet() {
		
		mPreferencesEditor.remove(ALARM_SET_KEY);
		return this;
	}
	
	public MICloseStore removeAlarmStarted() {
		
		mPreferencesEditor.remove(ALARM_STARTED_KEY);
		return this;
	}
	
	public MICloseStore removeAlarmDone() {
		
		mPreferencesEditor.remove(ALARM_DONE_KEY);
		return this;
	}
}
