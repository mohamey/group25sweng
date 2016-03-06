package com.aware.plugin.template;

import com.aware.providers.Locations_Provider;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

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
    int timesOccured = 0;
    String prevLat, prevLng, name = Build.MODEL, url = "http://mohamey.me/aware.php";
    ArrayList<Data> backlog;

    //Declare the cursors
    Cursor latCursor, lngCursor, timeCursor;
    public GpsObserver(Handler handler,Context myContext) {
        super(handler);
        context=myContext;

        //When network is unavailable, log locations here
        backlog = new ArrayList<Data>();

        //Cursors used to get data from phone
        latCursor = context.getContentResolver().query(Locations_Provider.Locations_Data.CONTENT_URI,null,null,null, Locations_Provider.Locations_Data.LATITUDE);    //from tutorial: "http://www.awareframework.com/how-do-i-read-data/"
        lngCursor = context.getContentResolver().query(Locations_Provider.Locations_Data.CONTENT_URI, null, null, null, Locations_Provider.Locations_Data.LONGITUDE);   //slight change made: added 'context', otherwise getContentResolver didn't work.
        timeCursor = context.getContentResolver().query(Locations_Provider.Locations_Data.CONTENT_URI, null, null, null, Locations_Provider.Locations_Data.TIMESTAMP);  //got idea for this from "http://stackoverflow.com/questions/8017540/cannot-use-the-contentresolver".

    }

    public void onChange(boolean selfChange) {

         if(latCursor!=null && lngCursor!=null && timeCursor != null){
            latCursor.moveToFirst();
            lngCursor.moveToFirst();
            timeCursor.moveToFirst();

             String lat = latCursor.getString(latCursor.getColumnIndex("double_latitude"));
             String lng = lngCursor.getString(lngCursor.getColumnIndex("double_longitude"));
             Double time = timeCursor.getDouble(timeCursor.getColumnIndex("timestamp"));

             if(latCursor.getPosition() == 0) {
                 try{
                     String[] params = {url,name,lat,lng,time+""};
                     sendData(params);
                 }catch(Exception except) {
                     Log.e(TAG, "There was an error logging the initial location value", except);
                 }

             }else if(latCursor.getPosition() > 0){
                 if(prevLat.equals(lat) && prevLng.equals(lng))
                 {
                     timesOccured++;
                 }
                 else{
                     timesOccured=0;
                     //If the location is not the same, send it to the database!
                     try{
                         String[] params = {url,name,lat,lng,time+""};
                         sendData(params);
                     }catch(Exception except){
                         Log.e(TAG, "There was an error sending the data to the database", except);
                     }
                 }
                 if(timesOccured == 12) {
                     Plugin.data.add(new Data(lat, lng, time+""));
                 }

                 Log.i(TAG,lat);
                 Log.i(TAG,lng);
                 Log.i(TAG,time+"");
                 store(lat,lng,time+"");
             }

             prevLat = lat;
             prevLng = lng;

             //Advance the cursors
             latCursor.moveToNext();
             lngCursor.moveToNext();
             timeCursor.moveToNext();
        }
        else {
            Log.i(TAG, "Cursors were null");
        }
    }

    public void store(String lat, String lng, String time){


    }

    //Prepare and send data to database
    void sendData(String[] params){
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
                postData.put("Time",longToDate(params[4]));

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
