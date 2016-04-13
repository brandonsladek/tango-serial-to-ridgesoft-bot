package com.example.tangoserialapplication;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.atap.tangoservice.Tango;
import com.google.atap.tangoservice.TangoAreaDescriptionMetaData;
import com.google.atap.tangoservice.TangoConfig;
import com.google.atap.tangoservice.TangoCoordinateFramePair;
import com.google.atap.tangoservice.TangoErrorException;
import com.google.atap.tangoservice.TangoEvent;
import com.google.atap.tangoservice.TangoInvalidException;
import com.google.atap.tangoservice.TangoOutOfDateException;
import com.google.atap.tangoservice.TangoPoseData;
import com.google.atap.tangoservice.TangoXyzIjData;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private Tango mTango;
    private TangoConfig mConfig;

    private TangoSerialConnection tsConn;
    private NavigationLogic navigationLogic;

    private Button manualControlButton;
    private Button autonomousControlButton;

    private Button connectButton;
    private Button startButton;

    private TextView connectionTextView;
    private TextView poseDataTextView;

    private boolean mIsRelocalized = false;
    private boolean mIsLearningMode = false;
    private boolean mIsConstantSpaceRelocalize = true;
    private TangoPoseData currentPose;

    private float[] rotationFloats;

    float x;
    float y;
    float z;
    float w;

    private final Object mSharedLock = new Object();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final MainActivity mainActivity = this;

        // Instantiate the Tango service
        mTango = new Tango(this);

        startActivityForResult(
                Tango.getRequestPermissionIntent(Tango.PERMISSIONTYPE_ADF_LOAD_SAVE),
                Tango.TANGO_INTENT_ACTIVITYCODE);

        while (!Tango.hasPermission(this, Tango.PERMISSIONTYPE_ADF_LOAD_SAVE)) {
            // do nothing until permission granted...
        }

        mIsRelocalized = false;
        mConfig = setTangoConfig(mTango, mIsLearningMode, mIsConstantSpaceRelocalize);

        // Buttons and text views!
        connectButton = (Button) findViewById(R.id.connectButton);
        manualControlButton = (Button) findViewById(R.id.manualControlButton);
        autonomousControlButton = (Button) findViewById(R.id.autonomousControlButton);
        poseDataTextView = (TextView) findViewById(R.id.poseDataTextView);
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

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // start analyzing pose updates and sending serial output
//                new Runnable() {
//                    @Override
//                    public void run() {
//                        while (true) {
//                            navigationLogic.navigate(currentPose);
//                        }
//                    }
//                };

                // Start new intent in new GUI
//                Intent navigationIntent = new Intent(MainActivity.this, NavigationActivity.class);
//                MainActivity.this.startActivity(navigationIntent);

//                while(true) {
//                    TangoPoseData pose = getCurrentPose();
//                    navigationLogic.navigate(pose);
//                }
            }
        });

    }

    // All of the methods below this point are from the Google Project Tango area learning tutorials
    // Some of the methods are modified.

    /**
     * Sets up the tango configuration object. Make sure mTango object is initialized before
     * making this call.
     */
    private TangoConfig setTangoConfig(Tango tango, boolean isLearningMode, boolean isLoadAdf) {

        TangoConfig config = new TangoConfig();
        config = tango.getConfig(TangoConfig.CONFIG_TYPE_CURRENT);

        // Check if learning mode
        if (isLearningMode) {
            // Set learning mode to config.
            config.putBoolean(TangoConfig.KEY_BOOLEAN_LEARNINGMODE, true);

        }

        // Check for Load ADF/Constant Space relocalization mode
        if (isLoadAdf) {
            ArrayList<String> fullUUIDList = new ArrayList<String>();

            // Returns a list of ADFs with their UUIDs
            fullUUIDList = tango.listAreaDescriptions();

            // Load the ADF named bestAdf
            if (fullUUIDList.size() > 0) {
//                for (int i = 0; i < fullUUIDList.size(); i++) {
//                    String uuid = fullUUIDList.get(i);
//                    String name = getName(uuid);
//                    if (name != null) {
//                        if (name.equals("bestAdf")) {
//                            config.putString(TangoConfig.KEY_STRING_AREADESCRIPTION, uuid);
//                        }
//                    }
//                }
                config.putString(TangoConfig.KEY_STRING_AREADESCRIPTION,
                        fullUUIDList.get(fullUUIDList.size()-1));
            }
        }
        return config;
    }

    // From Google Project Tango website tutorial
    public String getName(String uuid) {

        TangoAreaDescriptionMetaData metadata = new TangoAreaDescriptionMetaData();
        metadata = mTango.loadAreaDescriptionMetaData(uuid);
        byte[] nameBytes = metadata.get(TangoAreaDescriptionMetaData.KEY_NAME);
        if (nameBytes != null) {
            String name = new String(nameBytes);
            return name;
        } else {
            return null;
        }
    }

    /**
     * Initializes pose data we keep track of. To be done
     */
    private void initializePoseData() {
        currentPose = new TangoPoseData();
    }

    public TangoPoseData getCurrentPose() {
        return currentPose;
    }

    /**
     * Set up the callback listeners for the Tango service, then begin using the Motion
     * Tracking API. This is called in response to the user clicking the 'Start' Button.
     */
    private void setUpTangoListeners() {

        // Set Tango Listeners for Poses Device wrt Start of Service, Device wrt
        // ADF and Start of Service wrt ADF
        ArrayList<TangoCoordinateFramePair> framePairs = new ArrayList<TangoCoordinateFramePair>();
        framePairs.add(new TangoCoordinateFramePair(
                TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE,
                TangoPoseData.COORDINATE_FRAME_DEVICE));
        framePairs.add(new TangoCoordinateFramePair(
                TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION,
                TangoPoseData.COORDINATE_FRAME_DEVICE));
        framePairs.add(new TangoCoordinateFramePair(
                TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION,
                TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE));

        mTango.connectListener(framePairs, new Tango.OnTangoUpdateListener() {
            @Override
            public void onXyzIjAvailable(TangoXyzIjData xyzij) {
                // Not using XyzIj data for this sample
            }

            // Listen to Tango Events
            @Override
            public void onTangoEvent(final TangoEvent event) {

            }

            @Override
            public void onPoseAvailable(TangoPoseData pose) {
                // Make sure to have atomic access to Tango Data so that
                // UI loop doesn't interfere while Pose call back is updating
                // the data.
                synchronized (mSharedLock) {
                    currentPose = pose;
                    rotationFloats = currentPose.getRotationAsFloats();
                    x = rotationFloats[0];
                    y = rotationFloats[1];
                    z = rotationFloats[2];
                    w = rotationFloats[3];
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (currentPose != null) {

                            double xPos = currentPose.translation[0];
                            double yPos = currentPose.translation[1];
                            //double rotation = currentPose.rotation[1];

                            String poseString = "X position: " +  round(xPos) +
                                    "\nY position: " + round(yPos) +
                                    "\nRotation: " + (int) getPoseRotationDegrees();

                            poseDataTextView.setText(poseString);
                        }
                    }
                });

                if (navigationLogic != null) {
                    navigationLogic.navigate(currentPose);
                }

            }

            @Override
            public void onFrameAvailable(int cameraId) {
                // We are not using onFrameAvailable for this application.
            }
        });
    }

    public float getPoseRotationDegrees() {

        float t = y*x+z*w;
        int pole;
        float rollRadians;

        if (t > 0.499f) {
            pole = 1;
        } else if (t < -0.499f) {
            pole = -1;
        } else {
            pole = 0;
        }

        if (pole == 0) {
            rollRadians = (float) Math.atan2(2f*(w*z + y*x), 1f - 2f * (x*x + z*z));
        } else {
            rollRadians = pole * 2f * (float) Math.atan2(y, w);
        }

        // 0 - 360
        return (float) Math.toDegrees(rollRadians) + 180;
    }

    private double round(double number) {
        double newNumber = number * 100;
        int newInt = (int) newNumber;
        return newInt / 100.0;
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Reset pose data and start counting from resume.
        //initializePoseData();

        // Clear the relocalization state: we don't know where the device has been since our app was paused.
        mIsRelocalized = false;

        // Re-attach listeners.
        try {
            setUpTangoListeners();
        } catch (TangoErrorException e) {
            Toast.makeText(getApplicationContext(), "TangoErrorException!", Toast.LENGTH_SHORT).show();
        } catch (SecurityException e) {
            Toast.makeText(getApplicationContext(), "SecurityException!", Toast.LENGTH_SHORT).show();
        }

        // Connect to the tango service (start receiving pose updates).
        try {
            mTango.connect(mConfig);
        } catch (TangoOutOfDateException e) {
            Toast.makeText(getApplicationContext(), "TangoOutOfDateException!", Toast.LENGTH_SHORT).show();
        } catch (TangoErrorException e) {
            Toast.makeText(getApplicationContext(), "TangoErrorException!", Toast.LENGTH_SHORT).show();
        } catch (TangoInvalidException e) {
            Toast.makeText(getApplicationContext(), "TangoInvalidException!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            mTango.disconnect();
        } catch (TangoErrorException e) {
            Toast.makeText(getApplicationContext(), "TangoErrorException!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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
