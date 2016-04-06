package com.aware.plugin.location_context;

import com.aware.ESM;
import com.aware.providers.Applications_Provider;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.location.Address;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.location.Geocoder;

import com.aware.utils.Http;
import com.aware.utils.Scheduler;
import com.aware.utils.Scheduler.Schedule;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Hashtable;


/**
 * Created by Nicky on 22-Feb-16.
 */
public class GpsObserver extends ContentObserver {

    //Required Global Variables
    Context context;
    private static final String TAG = "AWARE-PLUGIN";
    String prevLat, prevLng, prevApp = "No Apps Running", name = Build.MODEL, url = "http://mohamey.me/aware.php";
    ArrayList<Data> backlog;
    protected LocationManager locationManager;
    ArrayList<Schedule> scheduler;
    JSONArray EsmQuestions = new JSONArray();

    public GpsObserver(Handler handler,Context myContext) {
        super(handler);
        context=myContext;

        //When network is unavailable, log locations here
        backlog = new ArrayList<Data>();
        scheduler = new ArrayList<Schedule>();

        //Get the location
        String[] gps = getGPSLocation();
        String app = getAppForeground();
        if(!gps[0].equals("N/A") && !gps[1].equals("N/A")){
            try{
                String[] params = {url, name, gps[0], gps[1], app, System.currentTimeMillis()+""};
                sendData(params);
            }catch(Exception e){
                Log.e(TAG, "There was an error sending data", e);
            }
        }

        prevApp = app;
        prevLat = formatCoord(gps[0]);
        prevLng = formatCoord(gps[1]);
    }

    public void onChange(boolean selfChange) {
        super.onChange(selfChange);

        //Get gps location and app
        String[] gps = getGPSLocation();
        String app = getAppForeground();
        if(!gps[0].equals("N/A") && !gps[1].equals("N/A")) {
            if((!formatCoord(gps[0]).equals(prevLat)) && (!formatCoord(gps[1]).equals(prevLng)) && (!app.equals(prevApp))){
                try {
                    String[] params = {url, name, gps[0], gps[1], app, System.currentTimeMillis() + ""};
                    sendData(params);
                } catch (Exception e) {
                    Log.e(TAG, "Error sending data to database", e);
                }
                prevLat = gps[0];
                prevLng=gps[1];
                prevApp=app;
            }else{
                Log.i(TAG, "Tried sending duplicate location");
            }
        }

    }

    //Convert Coordinates to address
    public String gpsToAddress(String lat, String lng){
        Geocoder geocoder = new Geocoder(context);
        String result = "unknown address";
        try{
            Address location = geocoder.getFromLocation(Double.parseDouble(lat), Double.parseDouble(lng), 1).get(0);
            result = location.getAddressLine(0);
            int count = 1;
            String temp = "";
            while((temp = location.getAddressLine(count)) != null){
                result = result+", "+temp;
                count++;
            }
            Log.i(TAG, result);
        }catch(Exception except){
            Log.e(TAG, "There was an error getting the locations", except);
        }
        return result;
    }

    //Get foreground applications
    public String getAppForeground(){
        String appName = "No apps Running";
        Cursor appCursor = context.getContentResolver().query(Applications_Provider.Applications_History.CONTENT_URI, null, null, null, Applications_Provider.Applications_History.APPLICATION_NAME + " DESC LIMIT 1");
        try{
            int i =0;
            if(appCursor.getCount() > 0){
                appCursor.moveToFirst();
                appName=appCursor.getString(appCursor.getColumnIndex("application_name"));
            }
            Log.i(TAG, "App name is: "+appName);
        }catch(Exception e){
            Log.e(TAG, "Problem getting app cursor", e);
        }
        return appName;
    }

    //Get last known location as gps co-ordinates
    public String[] getGPSLocation(){
        String[] result = {"N/A", "N/A"};
        String longitude, latitude;
        try{
            locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            Location temp = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            longitude = temp.getLongitude()+"";
            latitude = temp.getLatitude()+"";
            result[0] = latitude.substring(0, 8);
            result[1] = longitude.substring(0, 8);
            Log.i(TAG, "Locations are: "+result[0]+", "+result[1]);
        }catch(SecurityException e){
            Log.e(TAG, "Insufficient permissions to get location", e);
        }catch(Exception except){
            Log.e(TAG, "Unable to get location", except);
        }
        return result;
    }

    //Format co-ordinates to make them more readable and uniform
    public String formatCoord(String coord){
        int dotIndex = coord.indexOf('.');
        return coord.substring(0, dotIndex+3);
    }

    //Prepare and send data to database
    void sendData(String [] params){
        //Set up network info
        ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        //Send data when network is available
        if(networkInfo != null && networkInfo.isConnected()) {
            if(!backlog.isEmpty()){
                for(Data object: backlog){
                    sendToDatabase backlogSender = new sendToDatabase(context);
                    backlogSender.loadData(object.dumpArray());
                    backlogSender.execute();
                }
                Log.i(TAG, "Sent data objects from the backlog");
                backlog.clear();
            }
            sendToDatabase std = new sendToDatabase(context);
            std.loadData(params);
            std.execute();
        }else{
            //Stick the data in the backlog
            Data temp = new Data(params);
            backlog.add(temp);
            Log.i(TAG, "Added new Data object to the backlog");
        }
    }

    //Async task to send data to the database
    public class sendToDatabase extends AsyncTask<String, Void, Void>{
    //Parameters passed in for sending data should be [destination, device name, latitude, longitude, time]

        Context myContext;
        Hashtable<String, String> postData;
        String destination;

        public sendToDatabase(Context appContext){
            this.myContext = appContext;
        }

        @Override
        protected Void doInBackground(String... params){
            ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
            if(networkInfo != null && networkInfo.isConnected()){
                sendData();
            }
            return null;
        }

        private void loadData(String[] params){
            try{
                postData = new Hashtable<>();
                postData.put("Device",params[1]);
                postData.put("Latitude",params[2]);
                postData.put("Longitude",params[3]);
                postData.put("Time",longToDate(params[5]));
                postData.put("Application", params[4]);
                postData.put("Address", gpsToAddress(params[2], params[3]));

                //Save destination url
                this.destination = params[0];

                //Add to ESM Scheduler
                Log.i(TAG, "Adding to scheduler");
                addToScheduler(gpsToAddress(params[2], params[3]), longToDate(params[5]));
                Log.i(TAG, "Successfully added esm to scheduler");
            }catch(Exception except){
                Log.e("AWARE-PLUGIN", "There was an error in method loadData", except);
            }

        }

        private void sendData(){
            try{
                Http http = new Http(context);
                http.dataPOST(destination, postData, false);
                Log.i(TAG, "Successfully saved data to the database");
            }catch(Exception except){
                Log.e(TAG, "There was an error sending the data", except);
            }
        }
    }

    //Convert raw timestamp to date time
    public String longToDate(String param){
        long temp = Double.valueOf(param).longValue();
        Date date = new Date(temp);
        Format format = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        return format.format(date);
    }

    //Add new location to ESM Surveys
    public boolean addToScheduler(String address, String time){
        try{
            Schedule temp = new Schedule("Context Questions");
            String question = "Can you give context to this address: "+address+". You were here at: "+time;
            JSONObject esmWrapper = new JSONObject();
            JSONObject esm = new JSONObject();
            try{
                esm.put("esm_type", ESM.TYPE_ESM_TEXT);
                esm.put("esm_title", "Location Context");
                esm.put("esm_instructions", question);
                esm.put("esm_submit", "Submit.");
                esm.put("esm_expiration_threshold", 300);
                esm.put("esm_trigger", "com.aware.plugin.location_context");
                esmWrapper.put("esm", esm);
                Log.i(TAG, "ESM Wrapper contains: "+esmWrapper);
                EsmQuestions.put(esmWrapper);
            }catch(Exception e){
                Log.e(TAG, "Error building esm", e);
            }

            Settings setting = new Settings();
            String preferredHour = setting.getPreferredTime();
            Log.i(TAG, "Preferred Hour is: "+preferredHour);
            temp.addHour(Integer.parseInt(preferredHour));
            temp.setActionType(Scheduler.ACTION_TYPE_BROADCAST);
            temp.setActionClass(ESM.ACTION_AWARE_QUEUE_ESM);
            temp.addActionExtra(ESM.EXTRA_ESM, EsmQuestions.toString());
            Scheduler.saveSchedule(context, temp);
            scheduler.add(temp);
        }catch(Exception e){
            Log.e(TAG, "Error adding new scheduler object", e);
            return false;
        }
        return true;
    }
}
