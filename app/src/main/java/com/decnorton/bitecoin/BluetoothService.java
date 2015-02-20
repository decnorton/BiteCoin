package com.decnorton.bitecoin;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.decnorton.bitecoin.events.Bluetooth;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import bolts.Task;
import hugo.weaving.DebugLog;

/**
 * Created by decnorton on 20/02/15.
 */
public class BluetoothService extends Service {
    private static final String TAG = "BluetoothService";

    /**
     * Constants
     */
    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_ENABLE_BLUTEOOTH = 3;

    private static final Executor sSingleThreadExecutor = Executors.newSingleThreadExecutor();

    // Well known SPP UUID
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // Insert your bluetooth devices MAC address
    private BluetoothDevice mDevice;

    /**
     * Helpers
     */
    private final Bus bus = BusProvider.get();

    private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private Map<BluetoothDevice, BluetoothSocket> mBluetoothSockets = new HashMap<>();

    /**
     * Binder
     */
    private final BluetoothBinder mBinder = new BluetoothBinder();

    @Override
    public void onCreate() {
        super.onCreate();

        bus.register(this);

        checkBluetoothState();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        bus.unregister(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public Task<Boolean> connectToDevice(final BluetoothDevice device) {
        return Task.callInBackground(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                mDevice = device;

                if (device == null)
                    return false;

                if (!checkBluetoothState())
                    return false;

                BluetoothSocket socket;

                // Two things are needed to make a connection:
                //   A MAC address, which we got above.
                //   A Service ID or UUID.  In this case we are using the
                //     UUID for SPP.
                try {
                    socket = device.createRfcommSocketToServiceRecord(MY_UUID);
                } catch (IOException e) {
                    Log.e(TAG, "[connectToDevice] Couldn't create socket: " + e.getMessage(), e);
                    return false;
                }

                // Discovery is resource intensive.  Make sure it isn't going on
                // when you attempt to connect and pass your message.
                mBluetoothAdapter.cancelDiscovery();

                // Establish the connection.  This will block until it connects.
                try {
                    socket.connect();
                } catch (IOException e) {
                    Log.e(TAG, "[connectToDevice] Couldn't connect to device: " + e.getMessage(), e);

                    try {
                        socket.close();
                    } catch (IOException e2) {
                        Log.e(TAG, "[connectToDevice] Couldn't close socket during connection failure: " + e2.getMessage(), e);
                        return false;
                    }
                }

                mBluetoothSockets.put(device, socket);

                Log.i(TAG, "[connectToDevice] Connected!");

                bus.post(new Bluetooth.DeviceConnectedEvent(device));

                return true;
            }
        });
    }

    private boolean isConnected(BluetoothDevice device) {
        return mBluetoothSockets.containsKey(device) && mBluetoothSockets.get(device).isConnected();
    }

    @DebugLog
    public boolean disconnect(BluetoothDevice device) {
        if (!mBluetoothSockets.containsKey(device)) {
            Log.e(TAG, "[disconnect] Device isn't in mBluetoothSockets");
            return true;
        }

        try {
            mBluetoothSockets.get(device).close();
            mBluetoothSockets.remove(device);
            bus.post(new Bluetooth.DeviceDisconnectedEvent(device));
            Log.i(TAG, "[disconnect] Disconnected!");
            return true;
        } catch (IOException e) {
            Log.e(TAG, "[disconnect] Couldn't disconnect: " + e.getMessage(), e);
            return false;
        }
    }

    private boolean checkBluetoothState() {
        // Emulator doesn't support Bluetooth and will return null
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "[checkBluetoothState] Bluetooth not supported.");
            return false;
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Log.e(TAG, "[checkBluetoothState] Bluetooth isn't enabled.");
            return false;
        }

        Log.i(TAG, "[checkBluetoothState] Bluetooth is enabled.");

        return true;

    }

    @DebugLog
    public Task<Boolean> sendData(final BluetoothDevice device, final String message) {
        return Task.call(new Callable<Boolean>() {

            @Override
            public Boolean call() throws Exception {
                if (!isConnected(device)) {
                    Log.e(TAG, "[sendData] Not connected. Message: " + message);
                    return false;
                }

                byte[] bytes = message.getBytes();

                try {
                    mBluetoothSockets.get(device).getOutputStream().write(bytes);
                    return true;
                } catch (IOException e) {
                    Log.e(TAG, "[sendData] Couldn't send data: " + e.getMessage(), e);
                    return false;
                }
            }

        }, sSingleThreadExecutor);
    }


    public class BluetoothBinder extends Binder {

        public BluetoothService getService() {
            return BluetoothService.this;
        }

    }

    /**
     * Events
     */

    @Subscribe
    public void onDeviceConnectedEvent(Bluetooth.DeviceConnectedEvent event) {

    }

    @Subscribe
    public void onDeviceDisconnectedEvent(Bluetooth.DeviceDisconnectedEvent event) {
    }

    @Subscribe
    public void onDeviceDisconnectRequestedEvent(Bluetooth.DeviceDisconnectRequestedEvent event) {
        disconnect(event.device);
    }
}
