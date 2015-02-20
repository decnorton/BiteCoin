package com.decnorton.bitecoin;

import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.decnorton.bitecoin.events.Bluetooth;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import bolts.Continuation;
import bolts.Task;
import butterknife.ButterKnife;
import butterknife.InjectView;
import hugo.weaving.DebugLog;


public class MainActivity extends ActionBarActivity implements View.OnClickListener, BluetoothDevicesDialog.DevicePickedListener {
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

    @InjectView(R.id.main_bluetooth_device_name) TextView mBluetoothDeviceNameView;
    @InjectView(R.id.main_bluetooth_message) EditText mBluetoothMessageView;
    @InjectView(R.id.main_bluetooth_send) ImageButton mBluetoothSendView;


    // Dialogs
    private BluetoothDevicesDialog mBluetoothDevicesDialog = BluetoothDevicesDialog.getInstance(this);

    // Menu item
    private MenuItem mBluetoothConnectMenuItem;
    private MenuItem mBluetoothDisconnectMenuItem;

    /**
     * Services
     */
    private TrackerService mTrackerService;
    ServiceConnection mTrackerServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mTrackerService = ((TrackerService.TrackerBinder) service).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mTrackerService = null;
        }

    };
    private BluetoothService mBluetoothService;
    ServiceConnection mBluetoothServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBluetoothService = ((BluetoothService.BluetoothBinder) service).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBluetoothService = null;
        }

    };

    /**
     * Data
     */
    private BluetoothDevice mBluetoothDevice;
    private boolean mAuthInProgress = false;
    private boolean mIsConnecting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        ButterKnife.inject(this);

        mStartButton.setOnClickListener(this);
        mStopButton.setOnClickListener(this);
        mBluetoothSendView.setOnClickListener(this);

        showBluetoothDevicesDialog();

        populateViews();
    }

    private void showBluetoothDevicesDialog() {
        mBluetoothDevicesDialog.show(getSupportFragmentManager(), "deviceListDialog");
    }

    @Override
    protected void onResume() {
        super.onResume();

        bus.register(this);

        bindService(new Intent(this, TrackerService.class), mTrackerServiceConnection, Context.BIND_AUTO_CREATE);
        bindService(new Intent(this, BluetoothService.class), mBluetoothServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onPause() {
        super.onPause();

        unbindService(mTrackerServiceConnection);
        unbindService(mBluetoothServiceConnection);

        bus.unregister(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_OAUTH) {
            mAuthInProgress = false;

            if (resultCode == RESULT_OK) {
                mTrackerService.connectClient();
            }
        }
    }

    private void populateViews() {
        mStartButton.setVisibility(isTracking() ? View.GONE : View.VISIBLE);
        mStopButton.setVisibility(isTracking() ? View.VISIBLE : View.GONE);
        mTrackingProgress.setVisibility(isTracking() ? View.VISIBLE : View.GONE);

        // Set the device name label
        mBluetoothDeviceNameView.setText(
                mIsConnecting
                        ? "Connecting..."
                        : mBluetoothDevice == null
                        ? "Not connected"
                        : "Connected: " + mBluetoothDevice.getName()
        );

        // Set the hint on the message EditText
        mBluetoothMessageView.setHint(
                mBluetoothDevice != null
                        ? "Send message to " + mBluetoothDevice.getName()
                        : null
        );

        mBluetoothDeviceNameView.setVisibility(mBluetoothDevice != null ? View.GONE : View.VISIBLE);
        mBluetoothMessageView.setVisibility(mBluetoothDevice != null ? View.VISIBLE : View.GONE);
        mBluetoothSendView.setVisibility(mBluetoothDevice != null ? View.VISIBLE : View.GONE);

        if (mBluetoothDisconnectMenuItem != null)
            mBluetoothDisconnectMenuItem.setVisible(mBluetoothDevice != null);

        if (mBluetoothConnectMenuItem != null)
            mBluetoothConnectMenuItem.setVisible(mBluetoothDevice == null);
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

            case R.id.main_bluetooth_send:
                if (mBluetoothService != null) {
                    // Get the message
                    final String message = mBluetoothMessageView.getText().toString();

                    // Reset the field
                    mBluetoothMessageView.setText(null);

                    // Send the message
                    mBluetoothService
                            .sendMessage(mBluetoothDevice, message)
                            .continueWith(new Continuation<Boolean, Object>() {
                                @Override
                                public Object then(Task<Boolean> task) throws Exception {
                                    if (!task.isFaulted() && task.getResult()) {
                                        toast("Message sent: " + message);
                                    }

                                    return null;
                                }
                            }, Task.UI_THREAD_EXECUTOR);
                }
                break;

        }
    }

    @Override
    public void onBluetoothDevicePicked(final BluetoothDevice device) {
        Log.i(TAG, "[onBluetoothDevicePicked] " + device);

        if (mBluetoothService != null) {
            mIsConnecting = true;

            populateViews();

            // Connect to device then send a message
            mBluetoothService
                    .connectToDevice(device)
                    .continueWith(new Continuation<Boolean, Object>() {
                        @Override
                        public Object then(Task<Boolean> task) throws Exception {
                            mIsConnecting = false;

                            mBluetoothDevice = device;

                            toast("Connected to " + device.getName() + "!");

                            populateViews();

                            mBluetoothService.sendMessage(device, "BOOBIES");
                            mBluetoothService.sendMessage(device, "1");
                            return null;
                        }
                    }, Task.UI_THREAD_EXECUTOR);
        }
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        mBluetoothConnectMenuItem = menu.findItem(R.id.action_bluetooth_connect);
        mBluetoothDisconnectMenuItem = menu.findItem(R.id.action_bluetooth_disconnect);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        mBluetoothDisconnectMenuItem.setVisible(mBluetoothDevice != null);
        mBluetoothConnectMenuItem.setVisible(mBluetoothDevice == null);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.action_bluetooth_connect:
                showBluetoothDevicesDialog();
                return true;

            case R.id.action_bluetooth_disconnect:
                if (mBluetoothService != null)
                    mBluetoothService.disconnect(mBluetoothDevice);
                return true;

        }

        return super.onOptionsItemSelected(item);
    }

    private void startTracking() {
        if (mTrackerService != null)
            mTrackerService.startTracking();

        populateViews();
    }

    private void stopTracking() {
        if (mTrackerService != null)
            mTrackerService.stopTracking();

        populateViews();
    }

    private boolean isTracking() {
        return mTrackerService != null && mTrackerService.isTracking();
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

    @Subscribe
    public void onDeviceDisconnectedEvent(Bluetooth.DeviceDisconnectedEvent event) {
        if (event.device.equals(mBluetoothDevice)) {
            mBluetoothDevice = null;
            populateViews();
        }
    }

}
