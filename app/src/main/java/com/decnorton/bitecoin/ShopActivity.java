package com.decnorton.bitecoin;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by decnorton on 17/03/15.
 */
public class ShopActivity extends ActionBarActivity implements AdapterView.OnItemClickListener {
    private static final String TAG = "ShopActivity";

    /**
     * Views
     */
    @InjectView(android.R.id.list) ListView mListView;

    ShopAdapter mAdapter;
    List<FoodItem> mFoodItems = new ArrayList<FoodItem>();

    /**
     * Services
     */
    private TrackerService mTrackerService;
    private ServiceConnection mTrackerServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mTrackerService = ((TrackerService.TrackerBinder) service).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mTrackerService = null;
        }

    };

    public static void show(Context context) {
        context.startActivity(new Intent(context, ShopActivity.class));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_shop);

        ButterKnife.inject(this);

        mAdapter = new ShopAdapter(this, mFoodItems);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(this);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        setFoodItems(createFoodList());

        bindService(new Intent(this, TrackerService.class), mTrackerServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onPause() {
        super.onPause();

        unbindService(mTrackerServiceConnection);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        FoodItem item = (FoodItem) parent.getItemAtPosition(position);

        if (item != null) {
            buyFoodItem(item);
        }
    }

    private void buyFoodItem(FoodItem item) {
        if (item.getSteps() > mTrackerService.getAvailableSteps()) {
            toast("You haven't done enough exercise to buy this yet!");
            return;
        }

        mTrackerService.spendSteps(item.getSteps());

        toast("You've bought a " + item.getName());
    }

    private List<FoodItem> createFoodList() {
        List<FoodItem> data = new ArrayList<>();

        data.add(
                new FoodItem(0, "Apple", 52, R.drawable.food_apple)
        );

        data.add(
                new FoodItem(0, "Mars Bar", 260, R.drawable.food_mars_bar)
        );

        return data;
    }

    private void setFoodItems(List<FoodItem> foodItems) {
        mFoodItems.clear();
        mFoodItems.addAll(foodItems);

        mAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
