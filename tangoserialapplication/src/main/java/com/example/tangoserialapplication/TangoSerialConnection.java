package com.example.tangoserialapplication;

import android.app.Activity;
import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

/**
 * Created by brandonsladek on 3/28/16.
 */

public class TangoSerialConnection implements Runnable, Serializable {

    private UsbManager usbManager = null;
    private UsbDeviceConnection connection = null;
    private UsbSerialDriver driver = null;
    private UsbSerialPort port = null;
    private boolean isConnected = false;
    public Handler handler;
    private String TANGO_SERIAL_CONNECTION_THREAD = "TANGO_SERIAL_CONNECTION_THREAD";

    public TangoSerialConnection(Activity activity) {
        usbManager = (UsbManager) activity.getSystemService(Context.USB_SERVICE);
    }

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
                        commandRobot(CommandValues.MOVE_FORWARD);
                        break;
                    case CommandValues.MOVE_REVERSE:
                        commandRobot(CommandValues.MOVE_REVERSE);
                        break;
                    case CommandValues.MOVE_LEFT:
                        commandRobot(CommandValues.MOVE_LEFT);
                        break;
                    case CommandValues.MOVE_RIGHT:
                        commandRobot(CommandValues.MOVE_RIGHT);
                        break;
                    case CommandValues.MOVE_STOP:
                        commandRobot(CommandValues.MOVE_STOP);
                        break;
                    default:
                        Log.w(TANGO_SERIAL_CONNECTION_THREAD, "Unexpected value sent to TangoSerialConnection handler!");
                }
            }
        };
        Looper.loop();
    }

}
