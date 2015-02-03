package com.decnorton.bitecoin;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.common.GooglePlayServicesUtil;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import butterknife.ButterKnife;
import butterknife.InjectView;
import hugo.weaving.DebugLog;


public class MainActivity extends ActionBarActivity implements View.OnClickListener {
    private static final String TAG = "MainActivity";

    /**
     * Constants
     */
    private static final int REQUEST_OAUTH = 1;

    /**
     * Helpers
     */
    private final Bus bus = BusProvider.get();

    /**
     * Views
     */
    @InjectView(R.id.main_steps_container) LinearLayout mStepsContainer;
    @InjectView(R.id.main_tracking_progress) ProgressBar mTrackingProgress;
    @InjectView(R.id.main_start_stop_container) FrameLayout mStartStopContainer;
    @InjectView(R.id.main_start_button) Button mStartButton;
    @InjectView(R.id.main_stop_button) Button mStopButton;

    private TrackerService mService;

    ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ((TrackerService.TrackerBinder) service).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }
    };
    private boolean mIsBound = false;
    private boolean mAuthInProgress = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        ButterKnife.inject(this);

        mStartButton.setOnClickListener(this);
        mStopButton.setOnClickListener(this);

        populateViews();
    }

    @Override
    protected void onResume() {
        super.onResume();

        bus.register(this);

        bindService(new Intent(this, TrackerService.class), mServiceConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    @Override
    protected void onPause() {
        super.onPause();

        bus.unregister(this);

        if (mIsBound)
            unbindService(mServiceConnection);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_OAUTH) {
            mAuthInProgress = false;

            if (resultCode == RESULT_OK) {
                mService.connectClient();
            }
        }
    }

    private void populateViews() {
        mStartButton.setVisibility(isTracking() ? View.GONE : View.VISIBLE);
        mStopButton.setVisibility(isTracking() ? View.VISIBLE : View.GONE);
        mTrackingProgress.setVisibility(isTracking() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.main_start_button:
                startTracking();
                break;

            case R.id.main_stop_button:
                stopTracking();
                break;

        }
    }

    private void startTracking() {
        if (mService != null)
            mService.startTracking();

        populateViews();
    }

    private void stopTracking() {
        if (mService != null)
            mService.stopTracking();

        populateViews();
    }

    private boolean isTracking() {
        return mService != null && mService.isTracking();
    }

    private void addSteps(int newSteps, int totalSteps) {
        TextView textView = new TextView(this);
        textView.setText(String.format("New steps: %d | Total steps: %d", newSteps, totalSteps));
        mStepsContainer.addView(textView);
    }

    @DebugLog
    @Subscribe
    public void onStepsEvent(final TrackerService.StepsEvent event) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                addSteps(event.newSteps, event.totalSteps);
            }

        });
    }

    @DebugLog
    @Subscribe
    public void onTrackingStartedEvent(TrackerService.TrackingStartedEvent event) {
    }

    @DebugLog
    @Subscribe
    public void onTrackingStoppedEvent(TrackerService.TrackingStoppedEvent event) {
    }

    @DebugLog
    @Subscribe
    public void onDataSourceFailedEvent(TrackerService.DataSourceFailedEvent event) {
        try {
            event.status.startResolutionForResult(
                    this,
                    0
            );
        } catch (IntentSender.SendIntentException e) {
            e.printStackTrace();
        }
    }

    @DebugLog
    @Subscribe
    public void onConnectionFailedEvent(TrackerService.ConnectionFailedEvent event) {
        if (!event.result.hasResolution()) {
            // Show the localized error dialog
            GooglePlayServicesUtil.getErrorDialog(
                    event.result.getErrorCode(),
                    MainActivity.this,
                    0
            ).show();

            return;
        }

        // The failure has a resolution. Resolve it.
        // Called typically when the app is not yet authorized, and an
        // authorization dialog is displayed to the user.
        if (!mAuthInProgress) {
            Log.i(TAG, "Attempting to resolve failed connection");
            mAuthInProgress = true;

            try {
                event.result.startResolutionForResult(this, REQUEST_OAUTH);
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }
        }
    }

}
