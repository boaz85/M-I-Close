package com.boazsh.m_i_close.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.boazsh.m_i_close.app.R;
import com.boazsh.m_i_close.app.helpers.MICloseUtils;


//TODO: Allow picking address from map - Next Release
//TODO: Show target on map - Done
//TODO: Bug: Semicircles are not totally rounded on Moshe's phone - fixed(?)
//TODO: Powersave mode
//TODO: Auto complete address field - Done
//TODO: Show distance from target - Next Release


public class MainActivity extends MICloseBaseActivity {


    private TextView mSetAlarmTextView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(MICloseUtils.APP_LOG_TAG, "MainActivity was started");
        
        MICloseUtils.isLocationAvailable(MainActivity.this);
        
        if (mMICloseStore.isAlarmSet()) {
        	
        	Log.d(MICloseUtils.APP_LOG_TAG, "There is already alarm set. Starting SetTargetActivity");
        	Intent goToAlarmWaitActivityIntent = new Intent(MainActivity.this, AlarmActivity.class);
        	startActivity(goToAlarmWaitActivityIntent);
        }
        
        mSetAlarmTextView = (TextView) findViewById(R.id.setAlarmTextView);
        
        mSetAlarmTextView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                Intent setAlarmIntent = new Intent(MainActivity.this, SetTargetActivity.class);
                
                Log.d(MICloseUtils.APP_LOG_TAG, "Starting SetTargetActivity");
                startActivity(setAlarmIntent);
            }

        });
    }
    
    @Override
    public void onBackPressed() {
    	
    	backPressed();
    }
}
