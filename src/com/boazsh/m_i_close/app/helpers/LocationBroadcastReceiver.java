package com.boazsh.m_i_close.app.helpers;

import com.boazsh.m_i_close.app.services.AlarmService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;



/*This BroadcastReceiver is used to allow the alarm service start
 * also when the app is closed. The geofence API sometimes fail
 * to start the service directly.
 */
public class LocationBroadcastReceiver extends BroadcastReceiver {
	public LocationBroadcastReceiver() {
	}

	@Override
	public void onReceive(Context context, Intent intent) {

		intent.setClass(context, AlarmService.class);
		context.startService(intent);
	}
}
