package com.decnorton.bitecoin;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by decnorton on 20/02/15.
 */
public class BluetoothDeviceAdapter extends ArrayAdapter<BluetoothDevice> {
    private static final String TAG = "BluetoothDeviceAdapter";

    private LayoutInflater mInflater;

    public BluetoothDeviceAdapter(Context context, List<BluetoothDevice> objects) {
        super(context, R.layout.list_item_bluetooth_device, objects);

        mInflater = LayoutInflater.from(context);
    }

    @Override
    public View getView(int position, View v, ViewGroup parent) {
        Holder holder;

        if (v == null) {
            v = mInflater.inflate(R.layout.list_item_bluetooth_device, parent, false);

            holder = new Holder(v);
        } else {
            holder = (Holder) v.getTag();
        }

        BluetoothDevice device = getItem(position);

        holder.name.setText(device.getName());

        return v;
    }

    static class Holder {
        @InjectView(R.id.bluetooth_device_name) TextView name;

        private Holder(View view) {
            ButterKnife.inject(this, view);
            view.setTag(this);
        }
    }
}
