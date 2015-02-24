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

    private static final int NUM_PIXELS = 16;
    private static final int STEPS_PER_PIXEL = 10;

    private static final Executor sSingleThreadExecutor = Executors.newSingleThreadExecutor();

    // Well known SPP UUID
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // Insert your bluetooth devices MAC address
    private BluetoothDevice mBluetoothDevice;
    private BluetoothSocket mBluetoothSocket;

    /**
     * Helpers
     */
    private final Bus bus = BusProvider.get();

    private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

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
                mBluetoothDevice = device;

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
                    }

                    return false;
                }

                mBluetoothSocket = socket;

                Log.i(TAG, "[connectToDevice] Connected!");

                bus.post(new Bluetooth.DeviceConnectedEvent(device));

                return true;
            }
        });
    }

    private boolean isConnected() {
        if (mBluetoothSocket == null) {
            Log.e(TAG, "[isConnected] mBluetoothSocket is null");
            return false;
        }

        return mBluetoothSocket.isConnected();
    }

    @DebugLog
    public boolean disconnect() {
        try {
            if (mBluetoothSocket != null)
                mBluetoothSocket.close();

            bus.post(new Bluetooth.DeviceDisconnectedEvent(mBluetoothDevice));

            mBluetoothSocket = null;
            mBluetoothDevice = null;

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
    public Task<Boolean> sendMessage(final String message) {
        return Task.call(new Callable<Boolean>() {

            @Override
            public Boolean call() throws Exception {
                if (!isConnected()) {
                    Log.e(TAG, "[sendMessage] Not connected. Message: " + message);
                    return false;
                }

                byte[] bytes = message.getBytes();

                try {
                    mBluetoothSocket.getOutputStream().write(bytes);
                    return true;
                } catch (IOException e) {
                    Log.e(TAG, "[sendMessage] Couldn't send data: " + e.getMessage(), e);
                    return false;
                }
            }

        }, sSingleThreadExecutor);
    }

    @DebugLog
    public Task<Boolean> sendPixelMessage(final int totalSteps) {
        String message = "pixel ";

        int pixel = Math.min(totalSteps / STEPS_PER_PIXEL, NUM_PIXELS);

        message += pixel;

        Log.i(TAG, "[sendPixelMessage] Steps: " + totalSteps);
        Log.i(TAG, "[sendPixelMessage] Pixel: " + pixel);

        return sendMessage(message);
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
        if (event.device.equals(mBluetoothDevice)) {
            mBluetoothSocket = null;
            mBluetoothDevice = null;
        }
    }

    @Subscribe
    public void onDeviceDisconnectRequestedEvent(Bluetooth.DeviceDisconnectRequestedEvent event) {
        if (event.device.equals(mBluetoothDevice))
            disconnect();
    }

    @Subscribe
    public void onStepsEvent(TrackerService.StepsEvent event) {
        Log.i(TAG, "[onStepsEvent] Total steps: " + event.totalSteps);
        sendPixelMessage(event.totalSteps);
    }

}
