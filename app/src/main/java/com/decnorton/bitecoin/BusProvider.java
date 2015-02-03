package com.decnorton.bitecoin;

import com.squareup.otto.Bus;
import com.squareup.otto.ThreadEnforcer;

/**
 * Created by decnorton on 03/02/15.
 */
public class BusProvider {

    private static final Bus instance = new Bus(ThreadEnforcer.ANY);

    public static Bus get() {
        return instance;
    }

}
