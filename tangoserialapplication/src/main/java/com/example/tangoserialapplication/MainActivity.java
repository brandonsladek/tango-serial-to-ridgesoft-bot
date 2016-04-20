package com.example.tangoserialapplication;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.atap.tangoservice.Tango;

public class MainActivity extends Activity {

    private TangoSerialConnection tangoSerialConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Button manualControlButton;
        Button autonomousControlButton;
        Button networkControlButton;
        Button recordAdfButton;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startActivityForResult(
                Tango.getRequestPermissionIntent(Tango.PERMISSIONTYPE_ADF_LOAD_SAVE),
                Tango.TANGO_INTENT_ACTIVITYCODE);

        while (!Tango.hasPermission(this, Tango.PERMISSIONTYPE_ADF_LOAD_SAVE)) {
            // do nothing until permission granted...
        }

        // Buttons and text views!
        manualControlButton = (Button) findViewById(R.id.manualControlButton);
        autonomousControlButton = (Button) findViewById(R.id.autonomousControlButton);
        networkControlButton = (Button) findViewById(R.id.networkControlButton);
        recordAdfButton = (Button) findViewById(R.id.recordAdfButton);

        manualControlButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start manual control activity
                Intent manualControlIntent = new Intent(MainActivity.this, ManualControlActivity.class);
                MainActivity.this.startActivity(manualControlIntent);
            }
        });

        autonomousControlButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start autonomous control activity
                Intent autonomousControlIntent = new Intent(MainActivity.this, AutonomousControlActivity.class);
                MainActivity.this.startActivity(autonomousControlIntent);
            }
        });

        networkControlButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start network control activity
                Intent networkControlIntent = new Intent(MainActivity.this, NetworkControlActivity.class);
                MainActivity.this.startActivity(networkControlIntent);
            }
        });

        recordAdfButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start ADF record activity
                Intent recordAdfIntent = new Intent(MainActivity.this, ADFRecordActivity.class);
                MainActivity.this.startActivity(recordAdfIntent);
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