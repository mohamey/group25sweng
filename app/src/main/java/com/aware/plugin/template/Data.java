package com.aware.plugin.template;

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
    String app;

    public Data(String lat, String lng, String time){
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
        this.app = dbData[4];
        this.time = dbData[5];
    }

    public String[] dumpArray(){
        String[] result = {this.url,this.name,this.lat,this.lng, this.app, this.time};
        return result;
    }
}
