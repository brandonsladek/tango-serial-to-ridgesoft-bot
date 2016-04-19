package com.example.tangoserialapplication;

import android.app.Activity;
import android.os.Bundle;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/**
 * Created by brandonsladek on 4/12/16.
 */

public class ManualControlActivity extends Activity {

    public ManualControlActivity context = this;

//    private Button forwardButton;
//    private Button backButton;
//    private Button leftButton;
//    private Button rightButton;
//    private Button stopButton;

//    private TextView connectionTextView;
//    private TextView poseDataTextView;

    private TangoSerialConnection tangoSerialConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        TextView connectionTextView;
        TextView poseDataTextView;

        Button forwardButton;
        Button backButton;
        Button leftButton;
        Button rightButton;
        Button stopButton;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual_control);

        forwardButton = (Button) findViewById(R.id.forwardButton);
        backButton = (Button) findViewById(R.id.backButton);
        leftButton = (Button) findViewById(R.id.leftButton);
        rightButton = (Button) findViewById(R.id.rightButton);
        stopButton = (Button) findViewById(R.id.stopButton);

        tangoSerialConnection = (TangoSerialConnection) getIntent().getSerializableExtra("TangoSerialConnection");

        poseDataTextView = (TextView) findViewById(R.id.poseDataTextView);
        connectionTextView = (TextView) findViewById(R.id.connectionTextView);

        //tangoSerialConnection = TangoSerialConnection.getInstance();
        //tangoSerialConnection.usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        // Start thread for usb connection to robot
        Thread thread = new Thread(tangoSerialConnection);
        thread.start();

        if (tangoSerialConnection != null) {
            connectionTextView.setText("Connected Successfully!");

            forwardButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sendCommandToHandler('f');
                }
            });
            backButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sendCommandToHandler('b');
                }
            });
            leftButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sendCommandToHandler('l');
                }
            });
            rightButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sendCommandToHandler('r');
                }
            });
            stopButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sendCommandToHandler('s');
                }
            });
        } else {
            connectionTextView.setText("Not connected...");
        }
    }

    private void sendCommandToHandler(char commandValue) {
        Message msg = tangoSerialConnection.handler.obtainMessage();
        Bundle bundle = new Bundle();
        bundle.putChar("COMMAND_VALUE", commandValue);
        msg.setData(bundle);
        tangoSerialConnection.handler.sendMessage(msg);
    }
}
