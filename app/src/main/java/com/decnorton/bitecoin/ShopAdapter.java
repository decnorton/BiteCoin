package com.decnorton.bitecoin;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by decnorton on 17/03/15.
 */
public class ShopAdapter extends ArrayAdapter<FoodItem> {
    private static final String TAG = "FoodItemAdapter";

    private LayoutInflater mInflater;

    public ShopAdapter(Context context, List<FoodItem> objects) {
        super(context, R.layout.list_item_shop, objects);

        mInflater = LayoutInflater.from(context);
    }

    @Override
    public View getView(int position, View v, ViewGroup parent) {
        Holder holder;

        if (v == null) {
            v = mInflater.inflate(R.layout.list_item_shop, parent, false);
            holder = new Holder(v);
        } else {
            holder = (Holder) v.getTag();
        }

        FoodItem item = getItem(position);

        holder.name.setText(item.getName());
        holder.cost.setText(String.valueOf(item.getSteps()));
        holder.image.setImageResource(item.getImage());

        return v;
    }

    static class Holder {
        @InjectView(R.id.food_image) public ImageView image;
        @InjectView(R.id.food_name) public TextView name;
        @InjectView(R.id.food_cost) public TextView cost;

        public Holder(View view) {
            ButterKnife.inject(this, view);
            view.setTag(this);
        }
    }
}
