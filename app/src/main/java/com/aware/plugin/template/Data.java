package com.aware.plugin.template;

/**
 * Created by Nicky on 03-Mar-16.
 */
class Data {
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
}
