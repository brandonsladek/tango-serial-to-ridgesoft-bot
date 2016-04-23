package com.example.tangoserialapplication;

import android.app.Activity;
import android.content.Context;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/**
 * Created by brandonsladek on 4/12/16.
 */

public class ManualControlActivity extends Activity {

    private TangoSerialConnection tangoSerialConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual_control);

        TextView connectionTextView;
        TextView poseDataTextView;

        Button forwardButton = (Button) findViewById(R.id.forwardButton);
        Button backButton = (Button) findViewById(R.id.backButton);
        Button leftButton = (Button) findViewById(R.id.leftButton);
        Button rightButton = (Button) findViewById(R.id.rightButton);
        Button stopButton = (Button) findViewById(R.id.stopButton);

        poseDataTextView = (TextView) findViewById(R.id.poseDataTextView);
        connectionTextView = (TextView) findViewById(R.id.connectionTextView);

        tangoSerialConnection = TangoSerialConnection.getInstance();
        tangoSerialConnection.setUsbManager((UsbManager) this.getSystemService(Context.USB_SERVICE));
        Thread thread = new Thread(tangoSerialConnection);
        thread.start();

        if (tangoSerialConnection != null) {
            connectionTextView.setText("Connected Successfully!");

            forwardButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sendRobotCommand('f');
                }
            });
            backButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sendRobotCommand('b');
                }
            });
            leftButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sendRobotCommand('l');
                }
            });
            rightButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sendRobotCommand('r');
                }
            });
            stopButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sendRobotCommand('s');
                }
            });
        } else {
            connectionTextView.setText("Not connected...");
        }
    }

    private void sendRobotCommand(char commandValue) {
        Message msg = tangoSerialConnection.handler.obtainMessage();
        Bundle bundle = new Bundle();
        bundle.putChar("COMMAND_VALUE", commandValue);
        msg.setData(bundle);
        tangoSerialConnection.handler.sendMessage(msg);
    }
}
