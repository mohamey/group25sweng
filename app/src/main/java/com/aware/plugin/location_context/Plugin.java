package com.aware.plugin.location_context;

import android.Manifest;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.ESM;
import com.aware.providers.Locations_Provider;
import com.aware.utils.Aware_Plugin;
import com.aware.utils.Scheduler;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;


public class Plugin extends Aware_Plugin {

    ArrayList<Scheduler.Schedule> scheduler;
    GpsObserver gpsO;
    final String TAG = "AWARE-PLUGIN";
    @Override
    public void onCreate() {
        super.onCreate();
        scheduler = new ArrayList<Scheduler.Schedule>();
        DEBUG = Aware.getSetting(this, Aware_Preferences.DEBUG_FLAG).equals("true");
        if(DEBUG)
            Log.d("Begin", "Group 25 plugin running");



        //Initialize our plugin's settings
        if( Aware.getSetting(this, Settings.STATUS_PLUGIN_LCP).length() == 0 ) {
            Aware.setSetting(this, Settings.STATUS_PLUGIN_LCP, true);
        }

        //some temporary variables
        int gpsFrequency = 300;
        int gpsAccuracy = 150;
        int locationExpTime = 300;

        //Activate programmatically any sensors/plugins you need here
        //e.g., Aware.setSetting(this, Aware_Preferences.STATUS_ACCELEROMETER,true);
        //NOTE: if using plugin with dashboard, you can specify the sensors you'll use there.
        //GPS sensor:
        Aware.setSetting(this,Aware_Preferences.STATUS_LOCATION_GPS, true);
        Aware.setSetting(this,Aware_Preferences.FREQUENCY_LOCATION_GPS, gpsFrequency);
        Aware.setSetting(this, Aware_Preferences.MIN_LOCATION_GPS_ACCURACY, gpsAccuracy);
        Aware.setSetting(this, Aware_Preferences.LOCATION_EXPIRATION_TIME, locationExpTime);
        Aware.startSensor(this, Aware_Preferences.STATUS_LOCATION_GPS);

        //esm setup:
        Aware.setSetting(this, Aware_Preferences.STATUS_ESM, true); //we will need the ESMs
        Aware.startSensor(this, Aware_Preferences.STATUS_ESM); //ask AWARE to start ESM
        Aware.setSetting(this, Aware_Preferences.STATUS_APPLICATIONS, true);
        Aware.setSetting(this, Aware_Preferences.STATUS_LOCATION_GPS, true);
        Aware.setSetting(this, Aware_Preferences.STATUS_LOCATION_NETWORK, true);
        Aware.startSensor(this, Aware_Preferences.STATUS_APPLICATIONS);
        Aware.startSensor(this, Aware_Preferences.STATUS_LOCATION_GPS);
        Aware.startSensor(this, Aware_Preferences.STATUS_LOCATION_NETWORK);

        gpsO = new GpsObserver(new Handler(),this);    //two import options for 'Handler', I went with 'android.os.Handler'.
        getContentResolver().registerContentObserver(Locations_Provider.Locations_Data.CONTENT_URI, true, gpsO);


        //Any active plugin/sensor shares its overall context using broadcasts
        CONTEXT_PRODUCER = new ContextProducer() {
            @Override
            public void onContext() {
                //Broadcast your context here
            }
        };

        //Add permissions you need (Support for Android M)
        REQUIRED_PERMISSIONS.add(Manifest.permission.ACCESS_FINE_LOCATION);
        REQUIRED_PERMISSIONS.add(Manifest.permission.ACCESS_NETWORK_STATE);

        //Cursor context;

        //Activate plugin
        Aware.startPlugin(this, "com.aware.plugin.location_context");
    }


    //This function gets called every 5 minutes by AWARE to make sure this plugin is still running.
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //Check if the user has toggled the debug messages
        DEBUG = Aware.getSetting(this, Aware_Preferences.DEBUG_FLAG).equals("true");
        Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_ESM, true);
        Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_APPLICATIONS, true);
        Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_LOCATION_GPS, true);
        Aware.setSetting(getApplicationContext(), Aware_Preferences.STATUS_LOCATION_NETWORK, true);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Aware.stopSensor(this, Aware_Preferences.STATUS_ESM); //turn off ESM for our plugin
        Aware.setSetting(this, Settings.STATUS_PLUGIN_LCP, false);
        getContentResolver().unregisterContentObserver(gpsO);

        //Deactivate any sensors/plugins you activated here
        //e.g., Aware.setSetting(this, Aware_Preferences.STATUS_ACCELEROMETER, false);
        Aware.setSetting(this,Aware_Preferences.STATUS_LOCATION_GPS, false);
        //Stop plugin
        Aware.stopPlugin(this, "com.aware.plugin.location_context");
    }
}


