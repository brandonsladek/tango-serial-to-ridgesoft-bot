package com.example.tangoserialapplication;

import android.os.Handler;


/**
 * Created by brandonsladek on 4/13/16.
 */

public class UsbConnectionThread implements Runnable {

    private TangoSerialConnection tangoSerialConnection;
    private Handler handler;

    public UsbConnectionThread() {}

    @Override
    public void run() {
        //tangoSerialConnection = new TangoSerialConnection();
        while (!Thread.currentThread().isInterrupted()) {

        }
    }
}
