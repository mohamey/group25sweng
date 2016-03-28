package com.aware.plugin.template;

import android.Manifest;
import android.content.Intent;
import android.database.Cursor;
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

    private ArrayList<Data> dataArrayList()
    {
        ArrayList<Data> dataArray = new ArrayList<Data>();
        Data dummy = new Data("53.3478", "6.2597", "15:00:00");
        Data dummy1 = new Data("53.3478", "6.2597", "17:00:00");

        dataArray.add(dummy);
        dataArray.add(dummy1);
        return dataArray;
    }

    GpsObserver gpsO;
    final String TAG = "AWARE-PLUGIN";
    @Override
    public void onCreate() {
        super.onCreate();
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
        Aware.setSetting(this,Aware_Preferences.MIN_LOCATION_GPS_ACCURACY, gpsAccuracy);
        Aware.setSetting(this, Aware_Preferences.LOCATION_EXPIRATION_TIME, locationExpTime);
        Aware.startSensor(this, Aware_Preferences.STATUS_LOCATION_GPS);

        //esm setup:
        Aware.setSetting(this, Aware_Preferences.STATUS_ESM, true); //we will need the ESMs
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
        Aware.stopSensor(this, Aware_Preferences.STATUS_ESM); //turn off ESM for our plugin
        Aware.setSetting(this, Settings.STATUS_PLUGIN_LCP, false);
        getContentResolver().unregisterContentObserver(gpsO);

        //Deactivate any sensors/plugins you activated here
        //e.g., Aware.setSetting(this, Aware_Preferences.STATUS_ACCELEROMETER, false);
        Aware.setSetting(this,Aware_Preferences.STATUS_LOCATION_GPS, false);
        //Stop plugin
        Aware.stopPlugin(this, "com.aware.plugin.template");
    }

    //Schedule a survey
    private void scheduleMorningQuestionnaire(){
        try{
            ArrayList<String>  MORNINGJSON1 = new ArrayList<String>();
            MORNINGJSON1 = arrayListQuestion(dataArrayList());
            for(int i = 0; i<MORNINGJSON1.size(); i++)
            {
                Scheduler.Schedule schedule = new Scheduler.Schedule("morning_question");
                String question = MORNINGJSON1.get(i);
                Calendar calendar = new GregorianCalendar();
                schedule.setTimer(calendar);
                schedule.setActionType(Scheduler.ACTION_TYPE_BROADCAST);
                schedule.setActionClass(ESM.ACTION_AWARE_QUEUE_ESM);
                schedule.addActionExtra(ESM.EXTRA_ESM, question);
                Scheduler.saveSchedule(getApplicationContext(), schedule);

            }
        }catch(JSONException e){
            Log.e(TAG, "Json Exception scheduling questionnaire!", e);
        }

    }

    private ArrayList<String> arrayListQuestion(ArrayList<Data> dataList){
        ArrayList<String>  MORNINGJSON1 = new ArrayList<String>();
        ArrayList<String>  MORNINGJSON12 = new ArrayList<String>();
        MORNINGJSON12.add("[{'esm':{" +
                "'esm_type':" + ESM.TYPE_ESM_TEXT + "," +
                "'esm_title': 'Location Context'," +
                "'esm_instructions': 'Please give context to where you were at HELLO '," +
                "'esm_submit': 'Submit.'," +
                "'esm_expiration_threshold': 300," + //the user has 5 minutes to respond. Set to 0 to disable
                "'esm_trigger': 'com.aware.plugin.goodmorning'" +
                "}}]");
        MORNINGJSON12.add("[{'esm':{" +
                "'esm_type':" + ESM.TYPE_ESM_TEXT + "," +
                "'esm_title': 'Location Context'," +
                "'esm_instructions': 'Please give context to where you were at test12344 '," +
                "'esm_submit': 'Submit.'," +
                "'esm_expiration_threshold': 300," + //the user has 5 minutes to respond. Set to 0 to disable
                "'esm_trigger': 'com.aware.plugin.goodmorning'" +
                "}}]");
        MORNINGJSON12.add("[{'esm':{" +
                "'esm_type':" + ESM.TYPE_ESM_TEXT + "," +
                "'esm_title': 'Location Context'," +
                "'esm_instructions': 'Please give context to where you were at TEST2321312312 '," +
                "'esm_submit': 'Submit.'," +
                "'esm_expiration_threshold': 300," + //the user has 5 minutes to respond. Set to 0 to disable
                "'esm_trigger': 'com.aware.plugin.goodmorning'" +
                "}}]");


        for(int i = 0; i<dataList.size(); i++)
        {
            MORNINGJSON1.add("[{'esm':{" +
                    "'esm_type':" + ESM.TYPE_ESM_TEXT + "," +
                    "'esm_title': 'Location Context'," +
                    "'esm_instructions': 'Please give context to where you were at '," + i +
                    "'esm_submit': 'Submit.'," +
                    "'esm_expiration_threshold': 300," + //the user has 5 minutes to respond. Set to 0 to disable
                    "'esm_trigger': 'com.aware.plugin.goodmorning'" +
                    "}}]");
        }
        return MORNINGJSON12;

    }
    //Get a survey to deliver to user
    // public String getSurvey() {

    //     String SURVEYQUESTION = "";

    //     for (int i = 0; i < data.size(); i++) {
    //         SURVEYQUESTION += "[{'esm':{" +
    //                 "'esm_type': BITCHIN," +
    //                 "'esm_title': Question: WHERE THEM HOES AT," +
    //                 "'esm_instructions': Could you please give a name for the location for the coordinates" +
    //                 "69.6969, 66.6666 (You left here at 19:99)," +
    //                 "'esm_submit': 'Submit.'," +
    //                 "'esm_expiration_threshold': 300," + //the user has 5 minutes to respond. Set to 0 to disable
    //                 "'esm_trigger': 'com.aware.plugin.template'" +
    //                 "}}]";
    //     }

    //     return SURVEYQUESTION;
    // }

    //Assign Context to certain location
    /*public void assignContext() {
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


    }*/
}


