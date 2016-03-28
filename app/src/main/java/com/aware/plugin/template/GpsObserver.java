package com.aware.plugin.template;

import com.aware.providers.Locations_Provider;
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

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;


/**
 * Created by Nicky on 22-Feb-16.
 */
public class GpsObserver extends ContentObserver {

    //Required Global Variables
    Context context;
    private static final String TAG = "AWARE-PLUGIN";
    String prevLat, prevLng, prevApp = "No Apps Running", name = Build.MODEL, url = "http://mohamey.me/aware.php";
    Double prevTime;
    ArrayList<Data> backlog;
    protected LocationManager locationManager;

    //Declare the cursors
    Cursor latCursor, lngCursor, timeCursor, appNameCursor;
    public GpsObserver(Handler handler,Context myContext) {
        super(handler);
        context=myContext;

        //When network is unavailable, log locations here
        backlog = new ArrayList<Data>();

        try{
            //Cursors used to get data from phone
            latCursor = context.getContentResolver().query(Locations_Provider.Locations_Data.CONTENT_URI,null,null,null, Locations_Provider.Locations_Data.LATITUDE+" DESC LIMIT 1");    //from tutorial: "http://www.awareframework.com/how-do-i-read-data/"
            lngCursor = context.getContentResolver().query(Locations_Provider.Locations_Data.CONTENT_URI, null, null, null, Locations_Provider.Locations_Data.LONGITUDE+" DESC LIMIT 1");   //slight change made: added 'context', otherwise getContentResolver didn't work.
            timeCursor = context.getContentResolver().query(Locations_Provider.Locations_Data.CONTENT_URI, null, null, null, Locations_Provider.Locations_Data.TIMESTAMP + " DESC LIMIT 1");  //got idea for this from "http://stackoverflow.com/questions/8017540/cannot-use-the-contentresolver".
            appNameCursor = context.getContentResolver().query(Applications_Provider.Applications_Foreground.CONTENT_URI, null, null, null, Applications_Provider.Applications_Foreground.APPLICATION_NAME + " DESC LIMIT 1");
            Log.i(TAG, "Set Cursors from Constructor");

            if(latCursor != null && lngCursor != null && timeCursor != null){

                latCursor.moveToFirst();
                lngCursor.moveToFirst();
                timeCursor.moveToFirst();
                appNameCursor.moveToFirst();
                //if prior data exists send last location to database
                if((latCursor.getCount() != 0)){
                    try {
                        prevLat = latCursor.getString(latCursor.getColumnIndex("double_latitude"));
                        prevLng = lngCursor.getString(lngCursor.getColumnIndex("double_longitude"));
                        prevTime = timeCursor.getDouble(timeCursor.getColumnIndex("timestamp"));
                        if (appNameCursor.getCount() > 0)
                            prevApp = appNameCursor.getString(appNameCursor.getColumnIndex("application_name"));
                        else
                            Log.i(TAG, "There were no apps running in the foreground");
                    } catch (Exception except) {
                        Log.e(TAG, "Problem logging cursor data", except);
                    }

                    //Send the initial location
                    try {
                        String[] params = {url, name, prevLat, prevLng, prevApp, prevTime + ""};
                        sendData(params);
                        Log.i(TAG, "Sent initial location and app to database");
                    } catch (Exception except) {
                        Log.e(TAG, "There was an error logging the initial location and app value", except);
                    }
                    Log.i(TAG, prevTime+"");
                    Log.i(TAG, gpsToAddress(prevLat, prevLng));

                   //Built in LocationManager - Gives more accurate location results
                   /* try{
                        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
                        Location temp = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                        String longitude = temp.getLongitude()+"";
                        String latitude = temp.getLatitude()+"";
                        Log.i(TAG, latitude);
                        Log.i(TAG, longitude);
                        Log.i(TAG, gpsToAddress(latitude, longitude));
                    }catch(SecurityException e){
                        Log.e(TAG, "There was an error using location manager", e);
                    }*/
                }
            }

        }catch(Exception except){
            Log.e(TAG, "There was an error initialising the cursor", except);
        }

    }

    public void onChange(boolean selfChange) {
        super.onChange(selfChange);
        //Reinitialise cursors
        try{
            latCursor = context.getContentResolver().query(Locations_Provider.Locations_Data.CONTENT_URI,null,null,null, Locations_Provider.Locations_Data.LATITUDE+" DESC LIMIT 1");    //from tutorial: "http://www.awareframework.com/how-do-i-read-data/"
            lngCursor = context.getContentResolver().query(Locations_Provider.Locations_Data.CONTENT_URI, null, null, null, Locations_Provider.Locations_Data.LONGITUDE+" DESC LIMIT 1");   //slight change made: added 'context', otherwise getContentResolver didn't work.
            timeCursor = context.getContentResolver().query(Locations_Provider.Locations_Data.CONTENT_URI, null, null, null, Locations_Provider.Locations_Data.TIMESTAMP+" DESC LIMIT 1");  //got idea for this from "http://stackoverflow.com/questions/8017540/cannot-use-the-contentresolver".
            appNameCursor = context.getContentResolver().query(Applications_Provider.Applications_Foreground.CONTENT_URI, null, null, null, Applications_Provider.Applications_Foreground.APPLICATION_NAME + " DESC LIMIT 1");
        }catch(Exception except){
            Log.e(TAG, "Problem logging cursor data. Ignore if null pointer from app cursor.", except);
        }

        if(latCursor!=null && lngCursor!=null && timeCursor != null && appNameCursor !=null){
            latCursor.moveToFirst();
            lngCursor.moveToFirst();
            timeCursor.moveToFirst();

            String lat = latCursor.getString(latCursor.getColumnIndex("double_latitude"));
            String lng = lngCursor.getString(lngCursor.getColumnIndex("double_longitude"));
            Double time = timeCursor.getDouble(timeCursor.getColumnIndex("timestamp"));
            String app = "No Apps Running";

            //Since its possible app Cursor can be empty, must be handled separately
            if(appNameCursor.getCount() > 0) {
                appNameCursor.moveToFirst();
                app = appNameCursor.getString(appNameCursor.getColumnIndex("application_name"));
            }else{
                Log.i(TAG, "There were no running apps");
            }

            if(latCursor.getPosition() == 0 && prevTime < time ){
                 if(prevLat.equals(lat) && prevLng.equals(lng) && prevApp.equals(app))
                 {
                     //Originally sent every location to database, now only sending different locations
                     Log.i(TAG, "Tried to send a duplicate location and app");
                 }
                 else{
                     //If the location or app is not the same, send it to the database!
                     try{
                         String[] params = {url,name,lat,lng,app,time+""};
                         sendData(params);
                         Log.i(TAG, "Sent new location and App");
                     }catch(Exception except){
                         Log.e(TAG, "There was an error sending the data to the database", except);
                     }
                 }
             }else{
                if(latCursor.getPosition() != 0)
                    Log.i(TAG, "The cursors were not pointed at 0");
                else
                    Log.i(TAG, "New Time is equal to previous time. Cursor at row "+latCursor.getPosition()+" of "+(latCursor.getCount()-1));
            }

            Log.i(TAG,lat);
            Log.i(TAG, lng);
            Log.i(TAG, app);
            Log.i(TAG, time + "");
            try{
                Log.i(TAG, gpsToAddress(lat,lng));
            }catch(Exception e){
                Log.e(TAG, "No Network to convert address", e);
            }

            prevLat = lat;
            prevLng = lng;
            prevTime = time;
            prevApp = app;

        } else {
            Log.i(TAG, "Cursors were null");
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

    //Prepare and send data to database
    void sendData(String [] params){
        //Set up network info
        ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        //Send data when network is available
        if(networkInfo != null && networkInfo.isConnected()) {
            if(!backlog.isEmpty()){
                for(Data object: backlog){
                    sendToDatabase backBitchin = new sendToDatabase(context);
                    backBitchin.loadData(object.dumpArray());
                    backBitchin.execute();
                }
                Log.i(TAG, "Sent data objects from the backlog");
                backlog.clear();
            }
            sendToDatabase bitchin = new sendToDatabase(context);
            bitchin.loadData(params);
            bitchin.execute();
        }else{
            //Stick the data in the backlog
            Data temp = new Data(params);
            backlog.add(temp);
            Log.i(TAG, "Added new Data object to the backlog");
        }
    }

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

        private String longToDate(String param){
            long temp = Double.valueOf(param).longValue();
            Date date = new Date(temp);
            Format format = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            return format.format(date);
        }

    }

}
