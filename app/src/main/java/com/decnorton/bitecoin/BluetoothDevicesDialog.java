package com.decnorton.bitecoin;

import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.decnorton.bitecoin.events.Bluetooth;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import bolts.Continuation;
import bolts.Task;
import butterknife.ButterKnife;
import butterknife.InjectView;
import hugo.weaving.DebugLog;

/**
 * Created by decnorton on 20/02/15.
 */
public class BluetoothDevicesDialog extends DialogFragment implements AdapterView.OnItemClickListener {
    private static final String TAG = "DeviceListDialog";

    /**
     * Helpers
     */
    private Bus bus = BusProvider.get();
    private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    /**
     * Views
     */
    @InjectView(android.R.id.list) ListView mListView;
    private BluetoothDeviceAdapter mAdapter;
    private List<BluetoothDevice> mBluetoothDevices = new UniqueList<>();

    /**
     * Listener
     */
    private DevicePickedListener mListener;


    public static BluetoothDevicesDialog getInstance(DevicePickedListener listener) {
        BluetoothDevicesDialog instance = new BluetoothDevicesDialog();
        instance.setDevicePickedListener(listener);
        return instance;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAdapter = new BluetoothDeviceAdapter(getActivity(), mBluetoothDevices);

        getBondedBluetoothDevices();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        View view = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_bluetooth_devices, null);

        builder.setTitle(getString(R.string.dialog_bluetooth_devices_title));
        builder.setView(view);

        AlertDialog dialog = builder.create();

        ButterKnife.inject(this, view);

        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(this);

        return dialog;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();

        bus.register(this);

        mBluetoothAdapter.startDiscovery();
    }

    @Override
    public void onPause() {
        super.onPause();

        bus.unregister(this);

        mBluetoothAdapter.cancelDiscovery();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        BluetoothDevice device = (BluetoothDevice) parent.getItemAtPosition(position);

        if (mListener != null)
            mListener.onBluetoothDevicePicked(device);

        dismiss();
    }

    @DebugLog
    private Task<Set<BluetoothDevice>> getBondedBluetoothDevices() {
        return Task.callInBackground(new Callable<Set<BluetoothDevice>>() {
            @Override
            public Set<BluetoothDevice> call() throws Exception {
                return mBluetoothAdapter.getBondedDevices();
            }
        }).continueWithTask(new Continuation<Set<BluetoothDevice>, Task<Set<BluetoothDevice>>>() {

            @Override
            public Task<Set<BluetoothDevice>> then(Task<Set<BluetoothDevice>> task) throws Exception {
                if (task.isFaulted()) {
                    Exception e = task.getError();

                    Log.e(
                            TAG,
                            "[getBondedBluetoothDevices] Couldn't get bonded bluetooth devices.",
                            e
                    );

                    return Task.forError(task.getError());
                }

                addDevices(task.getResult());

                return task;
            }

        });
    }

    @DebugLog
    private void addDevices(Collection<BluetoothDevice> devices) {
        if (devices == null)
            return;

        mBluetoothDevices.addAll(devices);
        mAdapter.notifyDataSetChanged();
    }

    @DebugLog
    private void addDevice(BluetoothDevice device) {
        if (device == null)
            return;

        mBluetoothDevices.add(device);
        mAdapter.notifyDataSetChanged();
    }

    public void setDevicePickedListener(DevicePickedListener listener) {
        mListener = listener;
    }

    public interface DevicePickedListener {
        public void onBluetoothDevicePicked(BluetoothDevice device);
    }

    @Subscribe
    public void onDeviceFoundEvent(Bluetooth.DeviceFoundEvent event) {
        addDevice(event.device);
    }

    @DebugLog
    @Subscribe
    public void onDiscoveryStartedEvent(Bluetooth.DiscoveryStartedEvent event) {

    }

    @DebugLog
    @Subscribe
    public void onDiscoveryFinishedEvent(Bluetooth.DiscoveryFinishedEvent event) {

    }

}
