package com.decnorton.bitecoin.events;

import android.bluetooth.BluetoothDevice;

/**
 * Created by decnorton on 20/02/15.
 */
public class Bluetooth {

    private static abstract class BluetoothDeviceEvent {
        public final BluetoothDevice device;

        public BluetoothDeviceEvent(BluetoothDevice device) {
            this.device = device;
        }
    }

    public static class DeviceFoundEvent extends BluetoothDeviceEvent {
        public DeviceFoundEvent(BluetoothDevice device) {
            super(device);
        }
    }

    public static class DeviceConnectedEvent extends BluetoothDeviceEvent {
        public DeviceConnectedEvent(BluetoothDevice device) {
            super(device);
        }
    }

    public static class DeviceDisconnectedEvent extends BluetoothDeviceEvent {
        public DeviceDisconnectedEvent(BluetoothDevice device) {
            super(device);
        }
    }

    public static class DeviceDisconnectRequestedEvent extends BluetoothDeviceEvent {
        public DeviceDisconnectRequestedEvent(BluetoothDevice device) {
            super(device);
        }
    }

    public static class DiscoveryStartedEvent {
        public DiscoveryStartedEvent() {
        }
    }

    public static class DiscoveryFinishedEvent {
        public DiscoveryFinishedEvent() {
        }
    }

}
