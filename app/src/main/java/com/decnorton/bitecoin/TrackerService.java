package com.decnorton.bitecoin;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.Value;
import com.google.android.gms.fitness.request.DataSourcesRequest;
import com.google.android.gms.fitness.request.OnDataPointListener;
import com.google.android.gms.fitness.request.SensorRequest;
import com.google.android.gms.fitness.result.DataSourcesResult;
import com.squareup.otto.Bus;
import com.squareup.otto.Produce;

import java.util.concurrent.TimeUnit;

import hugo.weaving.DebugLog;

/**
 * Created by decnorton on 03/02/15.
 */
public class TrackerService extends Service {
    private static final String TAG = "TrackerService";

    /**
     * Constants
     */
    private static final int REQUEST_OAUTH = 1;

    /**
     * Helpers
     */
    private final Bus bus = BusProvider.get();

    /**
     * Binder
     */
    private final TrackerBinder mBinder = new TrackerBinder();

    /**
     * Track whether an authorization activity is stacking over the current activity, i.e. when
     * a known auth error is being resolved, such as showing the account chooser or presenting a
     * consent dialog. This avoids common duplications as might happen on screen rotations, etc.
     */
    private static final String AUTH_PENDING = "auth_state_pending";
    private boolean authInProgress = false;

    private GoogleApiClient mClient = null;

    private boolean mIsTracking = false;
    private Integer mInitialSteps = null;
    private int mLatestNewSteps = 0;
    private int mTotalSteps = 0;

    OnDataPointListener mStepListener = new OnDataPointListener() {
        @Override
        public void onDataPoint(DataPoint dataPoint) {
            if (!mIsTracking)
                return;

            for (Field field : dataPoint.getDataType().getFields()) {
                Value val = dataPoint.getValue(field);

                Log.i(TAG, "Detected DataPoint field: " + field.getName());
                Log.i(TAG, "Detected DataPoint value: " + val);

                if (mInitialSteps == null) {
                    mInitialSteps = val.asInt();
                    mTotalSteps = 0;
                }

                int totalSteps = val.asInt() - mInitialSteps;
                if (mLatestNewSteps == 0) {
                    mLatestNewSteps = totalSteps;
                } else {
                    mLatestNewSteps = totalSteps - mLatestNewSteps;
                }

                mTotalSteps = totalSteps;

                Log.i(TAG, "[onDataPoint] New: " + mLatestNewSteps);
                Log.i(TAG, "[onDataPoint] Total: " + mTotalSteps);

                bus.post(new StepsEvent(mLatestNewSteps, mTotalSteps));

                // Send event
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        Log.i(TAG, "[onCreate]");

        bus.register(this);

        buildFitnessClient();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "[onStartCommand]");

        // Connect to the Fitness API
        connectClient();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.i(TAG, "[onDestroy]");

        disconnectClient();

        bus.unregister(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void startTracking() {
        Log.i(TAG, "[startTracking]");

        bus.post(new TrackingStartedEvent());

        mIsTracking = true;
    }

    public void stopTracking() {
        Log.i(TAG, "[stopTracking]");

        bus.post(new TrackingStoppedEvent());

        mIsTracking = false;
    }

    public boolean isTracking() {
        return mIsTracking;
    }

    public int getTotalSteps() {
        return mTotalSteps;
    }

    public void resetSteps() {
        mInitialSteps = null;
    }

    @DebugLog
    @Produce
    public StepsEvent produceStepsEvent() {
        return new StepsEvent(mLatestNewSteps, mTotalSteps);
    }

    /**
     * Build a {@link GoogleApiClient} that will authenticate the user and allow the application
     * to connect to Fitness APIs. The scopes included should match the scopes your app needs
     * (see documentation for details). Authentication will occasionally fail intentionally,
     * and in those cases, there will be a known resolution, which the OnConnectionFailedListener()
     * can address. Examples of this include the user never having signed in before, or having
     * multiple accounts on the device and needing to specify which account to use, etc.
     */
    private void buildFitnessClient() {
        // Create the Google API Client
        mClient = new GoogleApiClient.Builder(this)
                .addApi(Fitness.API)
                .addScope(new Scope(Scopes.FITNESS_LOCATION_READ))
                .addConnectionCallbacks(
                        new GoogleApiClient.ConnectionCallbacks() {

                            @Override
                            public void onConnected(Bundle bundle) {
                                Log.i(TAG, "Connected!!!");

                                // Now you can make calls to the Fitness APIs.
                                // Put application specific code here.

                                listSensors();
                            }

                            @Override
                            public void onConnectionSuspended(int i) {
                                // If your connection to the sensor gets lost at some point,
                                // you'll be able to determine the reason and react to it here.
                                if (i == GoogleApiClient.ConnectionCallbacks.CAUSE_NETWORK_LOST) {
                                    Log.i(TAG, "Connection lost.  Cause: Network Lost.");
                                } else if (i == GoogleApiClient.ConnectionCallbacks.CAUSE_SERVICE_DISCONNECTED) {
                                    Log.i(TAG, "Connection lost.  Reason: Service Disconnected");
                                }
                            }
                        }
                )
                .addOnConnectionFailedListener(
                        new GoogleApiClient.OnConnectionFailedListener() {
                            // Called whenever the API client fails to connect.
                            @Override
                            public void onConnectionFailed(ConnectionResult result) {
                                Log.i(TAG, "Connection failed. Cause: " + result.toString());

                                bus.post(new ConnectionFailedEvent(result));
                            }
                        }
                )
                .build();
    }

    private void listSensors() {
        Fitness.SensorsApi.findDataSources(
                mClient,
                new DataSourcesRequest.Builder()
                        // At least one datatype must be specified.
                        .setDataTypes(DataType.TYPE_STEP_COUNT_CUMULATIVE)
                                // Can specify whether data type is raw or derived.
                        .setDataSourceTypes(DataSource.TYPE_RAW)
                        .build()
        )
                .setResultCallback(new ResultCallback<DataSourcesResult>() {
                    @Override
                    public void onResult(DataSourcesResult dataSourcesResult) {
                        Log.i(TAG, "[listSensors:onResult] Result: " + dataSourcesResult.getStatus().toString());

                        if (dataSourcesResult.getStatus().hasResolution()) {
                            bus.post(new DataSourceFailedEvent(dataSourcesResult.getStatus()));
                            return;
                        }

                        for (DataSource dataSource : dataSourcesResult.getDataSources()) {
                            Log.i(TAG, "Data source found: " + dataSource.toString());
                            Log.i(TAG, "Data Source type: " + dataSource.getDataType().getName());
                            addStepListener(dataSource);
                        }
                    }
                });
    }

    private void addStepListener(DataSource dataSource) {
        Fitness.SensorsApi.add(
                mClient,
                new SensorRequest.Builder()
                        .setDataType(dataSource.getDataType())
                        .setDataSource(dataSource)
                        .setSamplingRate(10, TimeUnit.SECONDS)
                                // Can specify whether data type is raw or derived.
                        .build(),
                mStepListener
        )
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        if (status.isSuccess()) {
                            Log.i(TAG, "Listener registered!");
                        } else {
                            Log.i(TAG, "Listener not registered.");
                        }
                    }
                });
    }

    @DebugLog
    public void connectClient() {
        // Make sure the app is not already connected or attempting to connect
        if (!mClient.isConnecting() && !mClient.isConnected()) {
            mClient.connect();
        }
    }

    public void disconnectClient() {
        if (mClient.isConnected()) {
            mClient.disconnect();
        }
    }

    public class TrackerBinder extends Binder {

        public TrackerService getService() {
            return TrackerService.this;
        }

    }

    public static class StepsEvent {
        public final int newSteps;
        public final int totalSteps;

        public StepsEvent(int newSteps, int totalSteps) {
            this.newSteps = newSteps;
            this.totalSteps = totalSteps;
        }

        @Override
        public String toString() {
            return String.format("{ newSteps: %d, totalSteps: %d }", newSteps, totalSteps);
        }
    }

    public static class TrackingStartedEvent {}

    public static class TrackingStoppedEvent {}

    public static class ConnectionFailedEvent {
        public final ConnectionResult result;

        public ConnectionFailedEvent(ConnectionResult result) {
            this.result = result;
        }
    }

    public class DataSourceFailedEvent {
        public final Status status;

        public DataSourceFailedEvent(Status status) {
            this.status = status;
        }
    }
}
