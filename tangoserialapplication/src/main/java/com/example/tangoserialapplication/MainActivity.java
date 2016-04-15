package com.example.tangoserialapplication;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

import com.google.atap.tangoservice.Tango;

public class MainActivity extends Activity {

    private TangoSerialConnection tsConn;

    private Button manualControlButton;
    private Button autonomousControlButton;
    private Button networkControlButton;
    private Button connectButton;
    private Button startButton;

    private TextView connectionTextView;

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
        connectButton = (Button) findViewById(R.id.connectButton);
        manualControlButton = (Button) findViewById(R.id.manualControlButton);
        autonomousControlButton = (Button) findViewById(R.id.autonomousControlButton);
        networkControlButton = (Button) findViewById(R.id.networkControlButton);

        connectionTextView = (TextView) findViewById(R.id.connectionTextView);
        connectionTextView.setText("Not connected...");

        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tsConn = new TangoSerialConnection(mainActivity);
                connectionTextView.setText("Connection successful!");

                //navigationLogic = new NavigationLogic(tsConn, mainActivity);
            }
        });

        manualControlButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start manual control activity
                Intent manualControlIntent = new Intent(MainActivity.this, ManualControlActivity.class);
                manualControlIntent.putExtra("TangoSerialConnection", tsConn);
                MainActivity.this.startActivity(manualControlIntent);
            }
        });

        autonomousControlButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start autonomous control activity
                Intent autonomousControlIntent = new Intent(MainActivity.this, AutonomousControlActivity.class);
                autonomousControlIntent.putExtra("TangoSerialConnection", tsConn);
                MainActivity.this.startActivity(autonomousControlIntent);
            }
        });

        networkControlButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start network control activity
                Intent networkControlIntent = new Intent(MainActivity.this, NetworkControlActivity.class);
                //networkControlIntent.putExtra("TangoSerialConnection", tsConn);
                MainActivity.this.startActivity(networkControlIntent);
            }
        });

//        startButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                // start analyzing pose updates and sending serial output
////                new Runnable() {
////                    @Override
////                    public void run() {
////                        while (true) {
////                            navigationLogic.navigate(currentPose);
////                        }
////                    }
////                };
//
//                // Start new intent in new GUI
////                Intent navigationIntent = new Intent(MainActivity.this, NavigationActivity.class);
////                MainActivity.this.startActivity(navigationIntent);
//
////                while(true) {
////                    TangoPoseData pose = getCurrentPose();
////                    navigationLogic.navigate(pose);
////                }
//            }
//        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
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