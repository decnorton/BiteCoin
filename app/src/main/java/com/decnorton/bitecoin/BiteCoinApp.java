package com.decnorton.bitecoin;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.decnorton.bitecoin.events.Bluetooth;
import com.squareup.otto.Bus;

/**
 * Created by decnorton on 03/02/15.
 */
public class BiteCoinApp extends Application {
    private static final String TAG = "BiteCoinApp";

    // Event Bus
    Bus bus = BusProvider.get();

    /**
     * Broadcast Receiver
     */
    private BluetoothBroadcastReceiver mBluetoothReceiver = new BluetoothBroadcastReceiver();
    private IntentFilter mBluetoothIntentFilter = new IntentFilter() {{
        addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
    }};

    @Override
    public void onCreate() {
        super.onCreate();

        // Register the broadcast receiver
        registerReceiver(mBluetoothReceiver, mBluetoothIntentFilter);

        // Start the background service
        startService(new Intent(this, TrackerService.class));
    }

    @Override
    public void onTerminate() {
        super.onTerminate();

        // Stop the background service
        stopService(new Intent(this, TrackerService.class));

        // Unregister the broadcast receiver
        unregisterReceiver(mBluetoothReceiver);
    }

    /**
     * Used to proxy broadcast intents into the Otto event bus
     */
    private final class BluetoothBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            switch (intent.getAction()) {
                case BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED:
                    bus.post(new Bluetooth.DeviceDisconnectRequestedEvent(device));
                    break;

                case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                    bus.post(new Bluetooth.DeviceDisconnectedEvent(device));
                    break;

                case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                    bus.post(new Bluetooth.DiscoveryStartedEvent());
                    break;

                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    bus.post(new Bluetooth.DiscoveryFinishedEvent());
                    break;
            }
        }
    }
}
