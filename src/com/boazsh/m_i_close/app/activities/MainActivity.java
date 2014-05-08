package com.boazsh.m_i_close.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.boazsh.m_i_close.app.R;


//TODO: Allow picking address from map - Next Release
//TODO: Show target on map - Done
//TODO: Bug: Semicircles are not totally rounded on Moshe's phone - fixed(?)
//TODO: Powersave mode
//TODO: Auto complete address field - Done
//TODO: Show distance from target - Next Release


public class MainActivity extends MICloseBaseActivity {


    private TextView mSetAlarm_TextView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        
        boolean isAlarmSet = mSharedPreferences.getBoolean(ALARM_SET_KEY, false);

        if (isAlarmSet) {
        	Intent goToAlarmWaitActivityIntent = new Intent(MainActivity.this, AlarmActivity.class);
        	startActivity(goToAlarmWaitActivityIntent);
        }
        
        mSetAlarm_TextView = (TextView) findViewById(R.id.setAlarmTextView);
        
        mSetAlarm_TextView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                Intent setAlarmIntent = new Intent(MainActivity.this, SetTargetActivity.class);
                startActivity(setAlarmIntent);

            }

        });
    }
    
    @Override
    public void onBackPressed() {
    	
    	backPressed();
    }
}
