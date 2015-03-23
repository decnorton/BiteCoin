package com.decnorton.bitecoin;

import android.support.annotation.DrawableRes;

/**
 * Created by decnorton on 17/03/15.
 */
public class FoodItem {

    public static final int CALORIES_PER_100_STEPS = 5;

    private long id;
    private String name;
    private long calories;
    @DrawableRes private int image;

    public FoodItem(long id, String name, int calories, int image) {
        this.id = id;
        this.name = name;
        this.calories = calories;
        this.image = image;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public long getCalories() {
        return calories;
    }

    public int getSteps() {
        return (int) calories;
    }

    public int getImage() {
        return image;
    }

}
