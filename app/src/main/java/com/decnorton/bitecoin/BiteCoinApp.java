package com.decnorton.bitecoin;

import android.app.Application;
import android.content.Intent;

/**
 * Created by decnorton on 03/02/15.
 */
public class BiteCoinApp extends Application {
    private static final String TAG = "BiteCoinApp";

    @Override
    public void onCreate() {
        super.onCreate();

        // Start the background service
        startService(new Intent(this, TrackerService.class));
    }

    @Override
    public void onTerminate() {
        super.onTerminate();

        // Stop the background service
        stopService(new Intent(this, TrackerService.class));
    }
}
