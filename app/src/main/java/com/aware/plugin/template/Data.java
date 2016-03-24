package com.aware.plugin.template;

import android.location.Address;
import android.location.Geocoder;

import java.util.List;

/**
 * Created by Nicky on 03-Mar-16.
 */
class Data {
    String url;
    String name;
    String lat;
    String lng;
    String time;
    String context;

    Data(String lat, String lng, String time ){
        this.lat = lat;
        this.lng = lng;
        this.time = time;
        this.context = "";
    }

    public Data(String[] dbData){
        this.url = dbData[0];
        this.name = dbData[1];
        this.lat = dbData[2];
        this.lng = dbData[3];
        this.time = dbData[4];
    }

    public String[] dumpArray(){
        String[] result = {this.url,this.name,this.lat,this.lng,this.time};
        return result;
    }
}
