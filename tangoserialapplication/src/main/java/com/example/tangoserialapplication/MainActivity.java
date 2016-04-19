package com.example.tangoserialapplication;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.atap.tangoservice.Tango;

public class MainActivity extends Activity {

    private Thread tangoSerialConnection;

    private Button manualControlButton;
    private Button autonomousControlButton;
    private Button networkControlButton;

    //private Button connectButton;
    //private TextView connectionTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final MainActivity mainActivity = this;

        startActivityForResult(
                Tango.getRequestPermissionIntent(Tango.PERMISSIONTYPE_ADF_LOAD_SAVE),
                Tango.TANGO_INTENT_ACTIVITYCODE);

        while (!Tango.hasPermission(this, Tango.PERMISSIONTYPE_ADF_LOAD_SAVE)) {
            // do nothing until permission granted...
        }

        // Buttons and text views!
        // connectButton = (Button) findViewById(R.id.connectButton);
        manualControlButton = (Button) findViewById(R.id.manualControlButton);
        autonomousControlButton = (Button) findViewById(R.id.autonomousControlButton);
        networkControlButton = (Button) findViewById(R.id.networkControlButton);

        //tangoSerialConnection = TangoSerialConnection.getInstance();

        // This is a thread
        //tangoSerialConnection = TangoSerialConnection.INSTANCE.init(getApplicationContext());

        //Bundle usbThreadBundle = new Bundle();
        //usbThreadBundle.putSerializable(tangoSerialConnection);

        //connectionTextView = (TextView) findViewById(R.id.connectionTextView);
        //connectionTextView.setText("Not connected...");

//        connectButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                tangoSerialConnection = new TangoSerialConnection(mainActivity);
//                connectionTextView.setText("Connected!");
//            }
//        });

        manualControlButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start manual control activity
                Intent manualControlIntent = new Intent(MainActivity.this, ManualControlActivity.class);
                //manualControlIntent.putExtra("TangoSerialConnection", tangoSerialConnection);
                MainActivity.this.startActivity(manualControlIntent);
            }
        });

        autonomousControlButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start autonomous control activity
                Intent autonomousControlIntent = new Intent(MainActivity.this, AutonomousControlActivity.class);
                //autonomousControlIntent.putExtra("TangoSerialConnection", tangoSerialConnection);
                MainActivity.this.startActivity(autonomousControlIntent);
            }
        });

        networkControlButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start network control activity
                Intent networkControlIntent = new Intent(MainActivity.this, NetworkControlActivity.class);
                //networkControlIntent.putExtra("TangoSerialConnection", tangoSerialConnection);
                MainActivity.this.startActivity(networkControlIntent);
            }
        });

    }

}

// Notes

// Main Activity -> Manual, Remote, Autonomous, ADFRecord

// runnables -> tts, speechrec, network, usb = all on own threads

// only want one instance of each runnable, make them static

// ClientThread extends Thread

// run {
// Looper.prepare()
// handler...
// public void handleMessage
// ct.handler.sendMessage
// public sendToTango

// create socket in Tango code
// close socket when remote activity finished
// only need to send to Tango from remote

// onClick with switch for R.id in MainActivity

// NO TANGO STUFF IN MAIN! still do Tango permissions stuff in main though...
// all activities get started from button clicks in main

// TangoCameraPreview, get camera view from R.id.cameraVIew from xml file
// onFrameAvailable next to onPoseAvailable in TangoListener
// android network permissions for camera use