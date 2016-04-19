package com.example.tangoserialapplication;

import android.app.Activity;
import android.os.Bundle;
import android.os.Message;
import android.view.View;
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

/** Brandon Sladek and John Waters */

public class AutonomousControlActivity extends Activity {

    private Button stopButton;

    private TangoSerialConnection tangoSerialConnection;
    private NavigationLogic navigationLogic;
    private Tango mTango;
    private TangoConfig mConfig;
    private boolean mIsRelocalized = false;
    private boolean mIsLearningMode = false;
    private boolean mIsConstantSpaceRelocalize = true;
    private TangoPoseData currentPose;
    private TangoPoseData[] mPoses;
    private final Object mSharedLock = new Object();
    private TextView poseDataTextView;
    private TextView connectionTextView;
    private TextView adfUUIDTextView;

    private double[] targetLocation = new double[]{-3.0, 4.0, 0.0};

    private float[] rotationFloats;
    float x;
    float y;
    float z;
    float w;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_autonomous_control);

        stopButton = (Button) findViewById(R.id.ac_stopButton);
        poseDataTextView = (TextView) findViewById(R.id.ac_poseDataTextView);
        connectionTextView = (TextView) findViewById(R.id.ac_connectionTextView);
        adfUUIDTextView = (TextView) findViewById(R.id.ac_adfUUIDTextView);

        // Start thread for usb serial connection
        tangoSerialConnection = new TangoSerialConnection(this);
        Thread thread = new Thread(tangoSerialConnection);
        thread.start();

        navigationLogic = new NavigationLogic();

        // Instantiate the Tango service
        mTango = new Tango(this);

        mIsRelocalized = false;
        mConfig = setTangoConfig(mTango, mIsLearningMode, mIsConstantSpaceRelocalize);

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onDestroy();
            }
        });

//        char currentCommand;
//
//        Thread navigationLogicThread = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                NavigationLogic navigationLogic = new NavigationLogic();
//
//                while(!Thread.currentThread().isInterrupted()) {
//                    char command = navigationLogic.navigate(currentPose.translation, currentPose.rotation, targetLocation);
//                    sendCommandToHandler(command);
//                }
//            }
//        });
//        navigationLogicThread.start();

//        while(true) {
//            char command = navigationLogic.navigate(currentPose.translation, currentPose.rotation, targetLocation);
//            sendCommandToHandler(command);
//        }

    }

    private void sendCommandToHandler(char commandValue) {
        Message msg = tangoSerialConnection.handler.obtainMessage();
        Bundle bundle = new Bundle();
        bundle.putChar("COMMAND_VALUE", commandValue);
        msg.setData(bundle);
        tangoSerialConnection.handler.sendMessage(msg);
    }

    // All of the methods below this point are from the Google Project Tango area learning tutorials
    // Some of the methods are modified.

    /**
     * Sets up the tango configuration object. Make sure mTango object is initialized before
     * making this call.
     */
    private TangoConfig setTangoConfig(Tango tango, boolean isLearningMode, boolean isLoadAdf) {

        TangoConfig config = tango.getConfig(TangoConfig.CONFIG_TYPE_CURRENT);

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
                config.putString(TangoConfig.KEY_STRING_AREADESCRIPTION,
                        fullUUIDList.get(fullUUIDList.size() - 1));

                final String adfName = getName(fullUUIDList.get(fullUUIDList.size()-1));

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adfUUIDTextView.setText("ADF Using: " + adfName);
                    }
                });
            }
        }
        return config;
    }

    // From Google Project Tango website tutorial
    public String getName(String uuid) {

        TangoAreaDescriptionMetaData metadata;
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
     * Initializes pose data we keep track of.
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
                // Will have to use XyzIj data for A level
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

//                    if (pose.baseFrame == TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION
//                            && pose.targetFrame == TangoPoseData.COORDINATE_FRAME_DEVICE) {
//                        mPoses[0] = pose;
//
//                    } else if (pose.baseFrame == TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE
//                            && pose.targetFrame == TangoPoseData.COORDINATE_FRAME_DEVICE) {
//                        mPoses[1] = pose;
//
//                    } else if (pose.baseFrame == TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION
//                            && pose.targetFrame == TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE) {
//                        mPoses[2] = pose;
//
//                        if (pose.statusCode == TangoPoseData.POSE_VALID) {
//                            mIsRelocalized = true;
//                        } else {
//                            mIsRelocalized = false;
//                        }
//                    }

                    currentPose = pose;
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (currentPose != null) {

                            double xPos = currentPose.translation[0];
                            double yPos = currentPose.translation[1];

                            String poseString = "X position: " +  round(xPos) +
                                    "\nY position: " + round(yPos) +
                                    "\nRotation: " + (int) getPoseRotationDegrees(currentPose.getRotationAsFloats());

                            poseDataTextView.setText(poseString);
                        }
                    }
                });

            }

            @Override
            public void onFrameAvailable(int cameraId) {
                // We will use this method for dealing with the camera
            }
        });
    }


    public float getPoseRotationDegrees(float[] rotationFloats) {

        float x = rotationFloats[0];
        float y = rotationFloats[1];
        float z = rotationFloats[2];
        float w = rotationFloats[3];

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
        initializePoseData();

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

}
