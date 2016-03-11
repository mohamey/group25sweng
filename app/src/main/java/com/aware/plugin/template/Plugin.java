package com.aware.plugin.template;

import android.Manifest;
import android.content.Intent;
import android.database.Cursor;
import android.os.Handler;
import android.util.Log;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.ESM;
import com.aware.providers.ESM_Provider;
import com.aware.providers.Locations_Provider;
import com.aware.utils.Aware_Plugin;
import com.aware.utils.Scheduler;

import org.json.JSONException;

import java.util.ArrayList;


public class Plugin extends Aware_Plugin {

    static ArrayList<Data> data = new ArrayList<Data>();
    Data dummy = new Data("53.3478", "6.2597", "15:00:00");
    data.add(dummy);

    GpsObserver gpsO;
    final String TAG = "AWARE-PLUGIN";
    @Override
    public void onCreate() {
        super.onCreate();

        DEBUG = Aware.getSetting(this, Aware_Preferences.DEBUG_FLAG).equals("true");
        if(DEBUG)
            Log.d("Begin", "Group 25 pluggin running");



        //Initialize our plugin's settings
        if( Aware.getSetting(this, Settings.STATUS_PLUGIN_TEMPLATE).length() == 0 ) {
            Aware.setSetting(this, Settings.STATUS_PLUGIN_TEMPLATE, true);
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
        Aware.setSetting(this,Aware_Preferences.MIN_LOCATION_GPS_ACCURACY, gpsAccuracy);
        Aware.setSetting(this, Aware_Preferences.LOCATION_EXPIRATION_TIME, locationExpTime);
        Aware.startSensor(this, Aware_Preferences.STATUS_LOCATION_GPS);

        //esm setup:
        //Aware.setSetting(this, Aware_Preferences.STATUS_ESM, true); //we will need the ESMs
        Aware.startSensor(this, Aware_Preferences.STATUS_ESM); //ask AWARE to start ESM

        //Aware.setSetting(this, Settings.STATUS_PLUGIN_TEMPLATE, true);


        gpsO = new GpsObserver(new Handler(),this);    //two import options for 'Handler', I went with 'android.os.Handler'.
        getContentResolver().registerContentObserver(Locations_Provider.Locations_Data.CONTENT_URI, true, gpsO);


        //Any active plugin/sensor shares its overall context using broadcasts
        CONTEXT_PRODUCER = new ContextProducer() {
            @Override
            public void onContext() {
                //Broadcast your context here
            }
        };

        //Add permissions you need (Support for Android M) e.g.,
        //REQUIRED_PERMISSIONS.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        REQUIRED_PERMISSIONS.add(Manifest.permission.ACCESS_FINE_LOCATION);
        REQUIRED_PERMISSIONS.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        REQUIRED_PERMISSIONS.add(Manifest.permission.INTERNET);
        REQUIRED_PERMISSIONS.add(Manifest.permission.ACCESS_NETWORK_STATE);

        //To sync data to the server, you'll need to set this variables from your ContentProvider
        //DATABASE_TABLES = Provider.DATABASE_TABLES
        //TABLES_FIELDS = Provider.TABLES_FIELDS
        //CONTEXT_URIS = new Uri[]{ Provider.Table_Data.CONTENT_URI }

        //Cursor context;

        scheduleMorningQuestionnaire(); //see further below

        //assignContext();


        //Activate plugin
        Aware.startPlugin(this, "com.aware.plugin.template");
    }


    //This function gets called every 5 minutes by AWARE to make sure this plugin is still running.
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        //Check if the user has toggled the debug messages
        DEBUG = Aware.getSetting(this, Aware_Preferences.DEBUG_FLAG).equals("true");

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Aware.setSetting(this, Settings.STATUS_PLUGIN_TEMPLATE, false);
        getContentResolver().unregisterContentObserver(gpsO);

        //Deactivate any sensors/plugins you activated here
        //e.g., Aware.setSetting(this, Aware_Preferences.STATUS_ACCELEROMETER, false);
        Aware.setSetting(this,Aware_Preferences.STATUS_LOCATION_GPS, false);
        //Stop plugin
        Aware.stopPlugin(this, "com.aware.plugin.template");
    }

    //Schedule a survey
    private void scheduleMorningQuestionnaire(){
        try {
            Scheduler.Schedule schedule = new Scheduler.Schedule("morning_question");
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 15);
            cal.set(Calendar.MINUTE, 20);
            schedule.setTimer(cal) //we want this schedule every day at 8PM
                .setActionType(Scheduler.ACTION_TYPE_BROADCAST) //we are doing a broadcast
                .setActionClass(ESM.ACTION_AWARE_QUEUE_ESM) //with this action
                .addActionExtra(ESM.EXTRA_ESM, getSurvey()) ;//and this extra
            Scheduler.saveSchedule(getApplicationContext(), schedule);
        }
        catch (JSONException except) {
            Log.e(TAG, "An exception occured scheduling the morning questionnaire", except);
        }
    }

    //Get a survey to deliver to user
    public String getSurvey() {

        String SURVEYQUESTION = "";

        for (int i = 0; i < data.size(); i++) {
            SURVEYQUESTION += "[{'esm':{" +
                    "'esm_type':" + ESM.TYPE_ESM_TEXT + "," +
                    "'esm_title': Question" + (i + 1) + "," +
                    "'esm_instructions': Could you please give a name for the location the coordinates" +
                    data.get(i).lat + data.get(i).lng + "(You left here at " + data.get(i).time + ")," +
                    "'esm_submit': 'Submit.'," +
                    "'esm_expiration_threshold': 300," + //the user has 5 minutes to respond. Set to 0 to disable
                    "'esm_trigger': 'com.aware.plugin.template'" +
                    "}}]";
        }

        return SURVEYQUESTION;
    }

    //Assign Context to certain location
    public void assignContext() {
        try{
            Cursor context = getContentResolver().query(ESM_Provider.ESM_Data.CONTENT_URI, null, null, null, null);
            context.moveToLast();
            for (int i = data.size(); i > 0; i--) {
                context.moveToPrevious();
            }

            if(data.size() > 0){
                int i = 0;
                do{
                    data.get(i).context= context.getString(context.getColumnIndex("esm_user_answer"));
                }while (context.moveToNext());
            }
        }catch(NullPointerException except) {
            Log.e(TAG, "There was a null pointer exception", except);
        }


    }
}


