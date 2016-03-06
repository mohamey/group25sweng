package com.aware.plugin.template;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.providers.Locations_Provider;
import com.aware.utils.Aware_Plugin;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import com.aware.providers.Screen_Provider;
import com.aware.utils.Http;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;


/**
 * Created by Nicky on 22-Feb-16.
 */
public class GpsObserver extends ContentObserver {

    Context context;
    private static final String TAG = "AWARE-PLUGIN";
    int timesOccured = 0;
    String prevLat;
    String prevLng;
    public GpsObserver(Handler handler,Context myContext) {
        super(handler);
        context=myContext;
    }

    public void onChange(boolean selfChange) {
        //Cursors used to get data from phone
        Cursor latCursor = context.getContentResolver().query(Locations_Provider.Locations_Data.CONTENT_URI,null,null,null, Locations_Provider.Locations_Data.LATITUDE);    //from tutorial: "http://www.awareframework.com/how-do-i-read-data/"
        Cursor lngCursor = context.getContentResolver().query(Locations_Provider.Locations_Data.CONTENT_URI,null,null,null, Locations_Provider.Locations_Data.LONGITUDE);   //slight change made: added 'context', otherwise getContentResolver didn't work.
        Cursor timeCursor = context.getContentResolver().query(Locations_Provider.Locations_Data.CONTENT_URI, null, null, null, Locations_Provider.Locations_Data.TIMESTAMP);  //got idea for this from "http://stackoverflow.com/questions/8017540/cannot-use-the-contentresolver".


        if(latCursor!=null && lngCursor!=null && timeCursor != null){
            latCursor.moveToFirst();
            lngCursor.moveToFirst();
            timeCursor.moveToFirst();
        }
        else {
            latCursor.moveToNext();
            lngCursor.moveToNext();
            timeCursor.moveToNext();
        }

        String lat = latCursor.getString(latCursor.getColumnIndex("double_latitude"));
        String lng = lngCursor.getString(lngCursor.getColumnIndex("double_longitude"));
        String time = timeCursor.getString(timeCursor.getColumnIndex("timestamp"));

        if(latCursor.getPosition() >= 2) {
            latCursor.moveToPrevious();
            lngCursor.moveToPrevious();
            prevLat = latCursor.getString(latCursor.getColumnIndex("double_latitude"));;
            prevLng = lngCursor.getString(lngCursor.getColumnIndex("double_longitude"));;

            latCursor.moveToNext();
            lngCursor.moveToNext();
        }
        else{
            prevLat = lat;
            prevLng = lng;
        }

        if(prevLat.equals(lat) && prevLng.equals(lng))
        {
            timesOccured++;
        }
        else{
            timesOccured=0;
        }
        if(timesOccured == 12){
            Plugin.data.add(new Data(lat, lng, time));
        }

        //Try send the data to a database
        try{
            String name = android.os.Build.MODEL;
            String[] params = {"http://mohamey.me/aware.php",name,lat,lng,time};
            sendToDatabase bitchin = new sendToDatabase(context);
            bitchin.loadData(params);
            bitchin.execute();
        }catch(Exception except){
            Log.e(TAG, "There was an error sending the data to the database", except);
        }

        Log.i(TAG,lat);
        Log.i(TAG,lng);
        Log.i(TAG,time);
        store(lat,lng,time);

    }

    public void store(String lat, String lng, String time){


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
