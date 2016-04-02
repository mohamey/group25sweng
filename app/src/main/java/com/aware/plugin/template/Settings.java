package com.aware.plugin.template;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;

import com.aware.Aware;

public class Settings extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    //Plugin settings in XML @xml/preferences
    public static final String STATUS_PLUGIN_LCP = "status_plugin_lcp";

    //Plugin settings UI elements
    private static CheckBoxPreference status;

    String preferredTime="21";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        this.preferredTime = prefs.getString("status_time_pref", "21");
        Log.i("AWARE-PLUGIN", "Updated preferred time to: "+preferredTime);
        prefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        status = (CheckBoxPreference) findPreference(STATUS_PLUGIN_LCP);
        if( Aware.getSetting(this, STATUS_PLUGIN_LCP).length() == 0 ) {
            Aware.setSetting(this, STATUS_PLUGIN_LCP, true); //by default, the setting is true on install
        }
        status.setChecked(Aware.getSetting(getApplicationContext(), STATUS_PLUGIN_LCP).equals("true"));
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference setting = findPreference(key);

        if( setting.getKey().equals(STATUS_PLUGIN_LCP) ) {
            boolean is_active = sharedPreferences.getBoolean(key, false);
            Aware.setSetting(this, key, is_active);
            if( is_active ) {
                Aware.startPlugin(getApplicationContext(), "com.aware.plugin.template");
            } else {
                Aware.stopPlugin(getApplicationContext(), "com.aware.plugin.template");
            }
            status.setChecked(is_active);
        }else if(setting.getKey().equals("status_time_pref")){
            String newTime = sharedPreferences.getString(key, "21");
            this.preferredTime = newTime;
            Log.i("AWARE-PLUGIN", "Preferred time updated to: "+preferredTime);
        }

    }

    public String getPreferredTime(){
        return this.preferredTime;
    }
}
