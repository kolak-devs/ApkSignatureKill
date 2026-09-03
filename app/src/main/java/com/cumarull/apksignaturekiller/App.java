package com.cumarull.apksignaturekiller;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import cat.ereza.customactivityoncrash.config.CaocConfig;

import androidx.preference.PreferenceManager;

import com.mcal.common.data.ReactivePreferences;

public class App extends Application {
    @SuppressLint("StaticFieldLeak")
    public static Context context;
    public static SharedPreferences preferences;

    public static Context getContext() {
        if (context == null) {
            context = new App();
        }
        return context;
    }

    public static SharedPreferences getPreferences() {
        return preferences;
    }

    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        ReactivePreferences.init(getApplicationContext());
        CaocConfig.Builder.create()
        .enabled(true) //default: true
        .showErrorDetails(true) //default: true
        .showRestartButton(true) //default: true
        .logErrorOnRestart(true) //default: true
        .trackActivities(true) //default: false
        .apply();
        
    }
}