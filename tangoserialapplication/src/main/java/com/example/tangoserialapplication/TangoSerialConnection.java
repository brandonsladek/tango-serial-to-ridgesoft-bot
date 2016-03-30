package com.example.tangoserialapplication;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;
import java.util.List;

/**
 * Created by brandonsladek on 3/28/16.
 */
public class TangoSerialConnection {

    private UsbManager usbManager = null;
    private UsbDeviceConnection connection = null;
    private UsbSerialDriver driver = null;
    private UsbSerialPort port = null;

    public TangoSerialConnection(MainActivity mainActivity) {
        usbManager = (UsbManager) mainActivity.getSystemService(Context.USB_SERVICE);

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

}
