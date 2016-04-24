package com.example.tangoserialapplication;

import android.app.Activity;
import android.content.Context;
import android.hardware.usb.UsbManager;
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

    private TangoSerialConnection tangoSerialConnection;
    private Thread serialThread;
    private TangoPoseData currentPose;
    private Tango mTango;
    private TangoConfig mConfig;

    private TextToSpeechThread tts;
    Thread ttsThread;

    private boolean targetEngaged = false;

    private final Object mSharedLock = new Object();
    private NavigationLogic navigationLogic;

    private boolean mIsRelocalized = false;
    private boolean mIsLearningMode = false;
    private boolean mIsConstantSpaceRelocalize = true;

    private TextView poseDataTextView;
    private TextView adfUUIDTextView;
    private TextView directionCommandTextView;
    private TextView localizationStatusTextView;
    private TextView ourRotationTextView;
    private TextView goRotationTextView;

    Long lastUpdateTime;

    // Red X in middle of classroom floor
    private double[] targetLocation = new double[]{6.4, 3.82, 1.0};

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_autonomous_control);

        // TextViews!
        poseDataTextView = (TextView) findViewById(R.id.ac_poseDataTextView);
        adfUUIDTextView = (TextView) findViewById(R.id.ac_adfUUIDTextView);
        directionCommandTextView = (TextView) findViewById(R.id.ac_directionCommandTextView);
        ourRotationTextView = (TextView) findViewById(R.id.ac_ourRotationTextView);
        goRotationTextView = (TextView) findViewById(R.id.ac_goRotationTextView);
        localizationStatusTextView = (TextView) findViewById(R.id.ac_localizationStatusTextView);
        localizationStatusTextView.setText("Not localized!");

        // Start thread for usb serial connection
        tangoSerialConnection = TangoSerialConnection.getInstance();
        tangoSerialConnection.setUsbManager((UsbManager) this.getSystemService(Context.USB_SERVICE));
        serialThread = new Thread(tangoSerialConnection);
        serialThread.start();

        tts = TextToSpeechThread.getInstance();
        tts.setContext(getApplicationContext());
        ttsThread = new Thread(tts);
        ttsThread.start();

        navigationLogic = new NavigationLogic();

        // Instantiate the Tango service
        mTango = new Tango(this);

        mIsRelocalized = false;
        mConfig = setTangoConfig(mTango, mIsLearningMode, mIsConstantSpaceRelocalize);

        lastUpdateTime = System.currentTimeMillis();

    }

    private void sendRobotCommand(char commandValue) {
        Message msg = tangoSerialConnection.handler.obtainMessage();
        Bundle bundle = new Bundle();
        bundle.putChar("COMMAND_VALUE", commandValue);
        msg.setData(bundle);
        tangoSerialConnection.handler.sendMessage(msg);
    }

    private void sendSpeakString(String toSpeak) {
        Message msg = tts.handler.obtainMessage();
        Bundle bundle = new Bundle();
        bundle.putString("SPEAK_TEXT", toSpeak);
        msg.setData(bundle);
        tts.handler.sendMessage(msg);
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

                int index = -1;

                for (int i = 0; i < fullUUIDList.size(); i++) {
                    if (getName(fullUUIDList.get(i)).equals("april15Adf")) {
                        index = i;
                    }
                }
                config.putString(TangoConfig.KEY_STRING_AREADESCRIPTION,
                        fullUUIDList.get(index));

                final String adfName = getName(fullUUIDList.get(index));

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

                    // Process new localization
                    if (pose.baseFrame == TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION
                            && pose.targetFrame == TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE) {
                        if (pose.statusCode == TangoPoseData.POSE_VALID) {
                            mIsRelocalized = true;

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    localizationStatusTextView.setText("Localized!");
                                }
                            });
                        }
                    }
                    // Process new ADF to device pose
                    else if (pose.baseFrame == TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION
                            && pose.targetFrame == TangoPoseData.COORDINATE_FRAME_DEVICE) {

                        if (mIsRelocalized && pose.statusCode == TangoPoseData.POSE_VALID) {

                            // Throttle pose updates by a tenth of a second
                            if (System.currentTimeMillis() - lastUpdateTime > 100 && !targetEngaged) {

                                NavigationInfo navigationInfo = navigationLogic.navigationInfo(pose.translation, pose.rotation, targetLocation);
                                sendRobotCommand(navigationInfo.getCommand());

                                if (navigationInfo.getCommand() == 's') {
                                    sendSpeakString("Engaging target");
                                    targetEngaged = true;
                                }

                                updateTextViews(pose, navigationInfo);

                                lastUpdateTime = System.currentTimeMillis();
                            }
                        }
                    }
                }
            }

            @Override
            public void onFrameAvailable(int cameraId) {
                // We will use this method for dealing with the camera
            }
        });

    }

    private void updateTextViews(final TangoPoseData pose, final NavigationInfo navigationInfo) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                double xPos = pose.translation[0];
                double yPos = pose.translation[1];

                String poseString = "X position: " + round(xPos) +
                        "\nY position: " + round(yPos) +
                        "\nRotation: " + (int) getYaw(pose.rotation);

                poseDataTextView.setText(poseString);
                directionCommandTextView.setText("Command: " + navigationInfo.getCommand());
                goRotationTextView.setText("Go: " + navigationInfo.getGoRotation());
                ourRotationTextView.setText("Our: " + navigationInfo.getOurRotation());
            }
        });
    }

    private double getYaw(double[] rotation) {

        double x = rotation[0];
        double y = rotation[1];
        double z = rotation[2];
        double w = rotation[3];

        return Math.toDegrees(Math.atan2(2.0*(x*y + w*z), w*w + x*x - y*y - z*z)) + 180;
    }

    private double getPoseRotationDegrees(double[] rotation) {

        double x = rotation[0];
        double y = rotation[1];
        double z = rotation[2];
        double w = rotation[3];

        double t = y*x+z*w;
        int pole;
        double rollRadians;

        if (t > 0.499f) {
            pole = 1;
        } else if (t < -0.499f) {
            pole = -1;
        } else {
            pole = 0;
        }

        if (pole == 0) {
            rollRadians = Math.atan2(2f*(w*z + y*x), 1f - 2f * (x*x + z*z));
        } else {
            rollRadians = pole * 2f * Math.atan2(y, w);
        }

        // 0 - 360
        return Math.toDegrees(rollRadians) + 180;
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
        serialThread.interrupt();
    }

}
