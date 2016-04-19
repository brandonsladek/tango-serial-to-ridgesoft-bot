package com.example.tangoserialapplication;

import android.app.Activity;
import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.google.atap.tangoservice.Tango;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

/**
 * Created by brandonsladek on 3/28/16.
 */

public enum TangoSerialConnection implements Runnable, Serializable {

    INSTANCE;

    public UsbManager usbManager = null;
    private UsbDeviceConnection connection = null;
    private UsbSerialDriver driver = null;
    private UsbSerialPort port = null;
    private boolean isConnected = false;
    public static Handler handler;
    private String TANGO_SERIAL_CONNECTION_THREAD;
    private Context applicationContext;

    private static TangoSerialConnection tangoSerialConnection;

    private TangoSerialConnection() {}

    public static TangoSerialConnection getInstance() {
        return INSTANCE;
    }

    public void init(final Context context) {
        applicationContext = context.getApplicationContext();
        usbManager = (UsbManager) applicationContext.getSystemService(applicationContext.USB_SERVICE);
    }

    public Context getApplicationContext() {
        if (null == applicationContext) {
            throw new IllegalStateException("have you called init(context)?");
        }

        return applicationContext;
    }

//    public TangoSerialConnection(Activity activity) {
//        usbManager = (UsbManager) activity.getSystemService(Context.USB_SERVICE);
//    }

    private void connectUsb() {
        List<UsbSerialDriver> availableDrivers =  UsbSerialProber.getDefaultProber().findAllDrivers(usbManager);

        // Set driver to Tango
        for (UsbSerialDriver usd : availableDrivers) {
            UsbDevice udv = usd.getDevice();
            if (udv.getVendorId()==1659 || udv.getProductId()==8963){
                driver = usd;
                break;
            }
        }

        connection = usbManager.openDevice(driver.getDevice());
        port = driver.getPorts().get(0);

        if (connection == null) return;

        try {
            port.open(connection);
            port.setParameters(115200, UsbSerialPort.DATABITS_8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);

        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public void handleMessage(char c) {
        try {
            port.write(new byte[]{(byte) c}, 1000);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void commandRobot(char c) {
        try {
            port.write(new byte[]{(byte) c}, 1000);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {

        Looper.prepare();

        if (!isConnected) {
            connectUsb();
            isConnected = true;
        }

        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch(msg.getData().getChar("COMMAND_VALUE")) {
                    case CommandValues.MOVE_FORWARD:
                        commandRobot('f');
                        break;
                    case CommandValues.MOVE_REVERSE:
                        commandRobot('b');
                        break;
                    case CommandValues.MOVE_LEFT:
                        commandRobot('l');
                        break;
                    case CommandValues.MOVE_RIGHT:
                        commandRobot('r');
                        break;
                    case CommandValues.MOVE_STOP:
                        commandRobot('s');
                        break;
                    default:
                        Log.w(TANGO_SERIAL_CONNECTION_THREAD, "Unexpected value sent to TangoSerialConnection handler!");
                }
            }
        };

        Looper.loop();

    }

}
