package com.aware.plugin.template;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.providers.Locations_Provider;
import com.aware.utils.Aware_Plugin;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Handler;
import android.util.Log;

import com.aware.providers.Screen_Provider;



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

        Log.i(TAG,lat);
        Log.i(TAG,lng);
        Log.i(TAG,time);
        store(lat,lng,time);

    }

    public void store(String lat, String lng, String time){


    }

}
