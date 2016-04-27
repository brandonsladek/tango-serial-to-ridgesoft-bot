package com.example.tangoserialapplication;

import android.app.Activity;
import android.content.Context;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
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

import java.io.IOException;
import java.net.ServerSocket;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;

/** Brandon Sladek and John Waters */

public class NetworkControlActivity extends Activity implements SaveAdfTask.SaveAdfListener {

    private ServerSocket serverSocket;
    private TextView mostRecentMessageTextView;
    private TextView localizationStatusTextView;
    private TextView adfUUIDTextView;
    private TangoSerialConnection tangoSerialConnection;
    private TextToSpeechThread tts;
    private NavigationLogic navigationLogic;
    private NetworkControlActivity context = this;
    private int SERVERPORT = 5010;
    private String landmark1Name;
    private String landmark2Name;
    private double[] landmarkOneLocation;
    private double[] landmarkTwoLocation;
    private Tango mTango;
    private TangoConfig mConfig;
    private boolean mIsRelocalized = false;
    private boolean mIsLearningMode = false;
    private boolean mIsConstantSpaceRelocalize = true;
    private TangoPoseData currentPose;
    private final Object mSharedLock = new Object();
    private TextView poseDataTextView;
    private TextView currentCommandTextView;
    private UsbManager usbManager;
    private String TAG = NetworkControlActivity.class.getSimpleName();
    private SaveAdfTask saveAdfTask;
    private NavigationInfo navigationInfo;

    private boolean adfLoadSpecific = false;
    private String adfToLoadName = "";

    private SafePath safePath;

    private boolean goToLandmarkOne = false;
    private boolean goToLandmarkTwo = false;
    private boolean goingToStartOfSafePath = false;
    private boolean driftCorrectionMode = false;

    private boolean recordingSafePath = false;
    private boolean safePathRecorded = false;
    ArrayList<SafePoint> safePoints = new ArrayList<>();

    Handler updateConversationHandler;
    Thread serverThread = null;
    Thread ttsThread;
    Long lastUpdateTime;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_network_control);

        mostRecentMessageTextView = (TextView) findViewById(R.id.nc_mostRecentMessageTextView);
        poseDataTextView = (TextView) findViewById(R.id.nc_poseDataTextView);
        currentCommandTextView = (TextView) findViewById(R.id.nc_currentCommandTextView);
        localizationStatusTextView = (TextView) findViewById(R.id.nc_localizationStatusTextView);
        localizationStatusTextView.setText("Not localized");
        adfUUIDTextView = (TextView) findViewById(R.id.nc_adfUUIDTextView);

        updateConversationHandler = new Handler();

        navigationLogic = new NavigationLogic();

        // Instantiate the Tango service
        mTango = new Tango(this);

        mIsRelocalized = false;
        mConfig = setTangoConfig(mTango, mIsLearningMode, mIsConstantSpaceRelocalize);

        this.serverThread = new Thread(new ServerThread());
        this.serverThread.start();

        tts = TextToSpeechThread.getInstance();
        tts.setContext(getApplicationContext());
        ttsThread = new Thread(tts);
        ttsThread.start();

        usbManager = (UsbManager) this.getSystemService(Context.USB_SERVICE);

        lastUpdateTime = System.currentTimeMillis();

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

            if (adfLoadSpecific) {
                // Load the ADF specified
                if (fullUUIDList.size() > 0) {

                    int index = -1;

                    for (int i = 0; i < fullUUIDList.size(); i++) {
                        if (getName(fullUUIDList.get(i)).equals(adfToLoadName)) {
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
            } else {
                // Load the most recent ADF
                if (fullUUIDList.size() > 0) {
                    config.putString(TangoConfig.KEY_STRING_AREADESCRIPTION,
                            fullUUIDList.get(fullUUIDList.size() - 1));
                }
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
                    if (pose.baseFrame == TangoPoseData.COORDINATE_FRAME_AREA_DESCRIPTION
                            && pose.targetFrame == TangoPoseData.COORDINATE_FRAME_DEVICE) {

                        if (mIsRelocalized && pose.statusCode == TangoPoseData.POSE_VALID) {

                            currentPose = pose;
                            updateTextViews(pose, null);

                            // Add current location to list of safe points
                            if (recordingSafePath) {
                                safePoints.add(new SafePoint(pose.translation));
                            }

                            if (driftCorrectionMode) {
                                double[] closestSafePathPoint = safePath.getClosestSafePathPoint(pose.translation);
                                double distanceFromSafePath = navigationLogic.getDistance(pose.translation, closestSafePathPoint);

                                if (distanceFromSafePath < 0.075) {
                                    driftCorrectionMode = false;
                                    return;
                                }

                                // Throttle pose updates by a tenth of a second
                                if (System.currentTimeMillis() - lastUpdateTime > 100) {

                                    navigationInfo = navigationLogic.navigationInfo(pose.translation, pose.rotation, closestSafePathPoint);
                                    sendRobotCommand(navigationInfo.getCommand());

                                    lastUpdateTime = System.currentTimeMillis();
                                }
                            }

                            else if (goToLandmarkOne) {
                                // Throttle pose updates by a tenth of a second
                                if (System.currentTimeMillis() - lastUpdateTime > 100) {

//                                    // Add current location to list of safe points
//                                    if (recordingSafePath) {
//                                        safePoints.add(new SafePoint(pose.translation));
//                                    }

                                    if (safePathRecorded) {
                                        double[] closestSafePathPoint = safePath.getClosestSafePathPoint(pose.translation);
                                        double distanceFromSafePath = navigationLogic.getDistance(pose.translation, closestSafePathPoint);

                                        if (distanceFromSafePath > 0.075) {
                                            sendSpeakString("Entering drift correction mode");
                                            driftCorrectionMode = true;
                                            return;
                                        }
                                    }

                                    navigationInfo = navigationLogic.navigationInfo(pose.translation, pose.rotation, landmarkOneLocation);
                                    sendRobotCommand(navigationInfo.getCommand());

                                    if (navigationInfo.getCommand() == 's') {
                                        goToLandmarkOne = false;
                                        sendSpeakString("Engaging target one");
                                    }

                                    updateTextViews(pose, navigationInfo);
                                    lastUpdateTime = System.currentTimeMillis();
                                }
                            }

                            else if (goToLandmarkTwo) {
                                // Throttle pose updates by a tenth of a second
                                if (System.currentTimeMillis() - lastUpdateTime > 100) {

//                                    // Add current location to list of safe points
//                                    if (recordingSafePath) {
//                                        safePoints.add(new SafePoint(pose.translation));
//                                    }

                                    if (safePathRecorded) {
                                        double[] closestSafePathPoint = safePath.getClosestSafePathPoint(pose.translation);
                                        double distanceFromSafePath = navigationLogic.getDistance(pose.translation, closestSafePathPoint);

                                        if (distanceFromSafePath > 0.075) {
                                            sendSpeakString("Entering drift correction mode");
                                            driftCorrectionMode = true;
                                            return;
                                        }
                                    }

                                    navigationInfo = navigationLogic.navigationInfo(pose.translation, pose.rotation, landmarkTwoLocation);
                                    sendRobotCommand(navigationInfo.getCommand());

                                    if (navigationInfo.getCommand() == 's') {
                                        goToLandmarkTwo = false;
                                        sendSpeakString("Engaging target two");
                                    }

                                    updateTextViews(pose, navigationInfo);
                                    lastUpdateTime = System.currentTimeMillis();
                                }
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
                        "\nRotation: " + (int) getOurRotation(pose.rotation);

                poseDataTextView.setText(poseString);
                //currentCommandTextView.setText("Command: " + navigationInfo.getCommand());
                //goRotationTextView.setText("Go: " + navigationInfo.getGoRotation());
                //ourRotationTextView.setText("Our: " + navigationInfo.getOurRotation());
            }
        });
    }

    private double getOurRotation(double[] rotation) {

        double x = rotation[0];
        double y = rotation[1];
        double z = rotation[2];
        double w = rotation[3];

        return Math.toDegrees(Math.atan2(2.0*(x*y + w*z), w*w + x*x - y*y - z*z)) + 180;
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

    /**
     * Handles failed save from mSaveAdfTask.
     */
    public void onSaveAdfFailed(String adfName) {
        String toastMessage = String.format(
                getResources().getString(R.string.save_adf_failed_toast_format),
                adfName);
        Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show();
        saveAdfTask = null;
    }

    /**
     * Handles successful save from mSaveAdfTask.
     */
    @Override
    public void onSaveAdfSuccess(String adfName, String adfUuid) {
        String toastMessage = String.format(
                getResources().getString(R.string.save_adf_success_toast_format),
                adfName, adfUuid);
        Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show();
        saveAdfTask = null;
        finish();
    }

    class ServerThread implements Runnable {

        public void run() {
            Socket socket = null;

            try {
                serverSocket = new ServerSocket(SERVERPORT);
            } catch (IOException e) {
                e.printStackTrace();
            }

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // close this socket before closing serverSocket
                    socket = serverSocket.accept();

                    CommunicationThread commThread = new CommunicationThread(socket);
                    new Thread(commThread).start();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    class CommunicationThread implements Runnable {

        private Socket clientSocket;
        private BufferedReader input;

        public CommunicationThread(Socket clientSocket) {
            this.clientSocket = clientSocket;

            try {
                this.input = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    String read = input.readLine();

                    if (read.equals("c")) {
                        tangoSerialConnection = TangoSerialConnection.getInstance();
                        tangoSerialConnection.setUsbManager(usbManager);
                        Thread thread = new Thread(tangoSerialConnection);
                        thread.start();
                        updateConversationHandler.post(new updateUIThread("Connected!"));

                    }

                    else if (read.equals("recordADF")) {
                        onPause();
                        // Create new tango config with learning mode on this time
                        mConfig = setTangoConfig(mTango, true, false);
                        onResume();

                    }

                    else if (read.contains("adfSave")) {
                        if (mConfig.getBoolean(TangoConfig.KEY_BOOLEAN_LEARNINGMODE)) {
                            String data = read;
                            String adfName = data.split(" ")[1];
                            saveAdf(adfName);
                            //saveAdfTask = new SaveAdfTask(context, context, mTango, adfName);
                            //saveAdfTask.execute();
                        }

                    }

                    else if (read.contains("adfLoad")) {
                        adfLoadSpecific = true;
                        String data = read;
                        adfToLoadName = data.split(" ")[1];
                        onPause();
                        mConfig = setTangoConfig(mTango, false, true);
                        onResume();
                    }

                    else if (read.equals("startSafePathRecord")) {
                        recordingSafePath = true;
                        sendSpeakString("Recording safe path");
                    }

                    else if (read.equals("stopSafePathRecord")) {
                        recordingSafePath = false;
                        safePathRecorded = true;
                        sendSpeakString("Done recording safe path");
                        safePath = new SafePath(safePoints);
                    }

                    else if (read.contains("save")) {
                        String landmarkName = read;
                        String commands[] = landmarkName.split(" ");

                        if (landmark1Name == null) {
                            landmark1Name = commands[1];
                            landmarkOneLocation = currentPose.translation;
                            sendSpeakString("Target 1 recorded");
                        } else {
                            landmark2Name = commands[1];
                            landmarkTwoLocation = currentPose.translation;
                            sendSpeakString("Target 2 recorded");
                        }

                    } else if (read.equals("goto1")) {
                        goToLandmarkOne = true;
                        goToLandmarkTwo = false;

                    } else if (read.equals("goto2")) {
                        goToLandmarkTwo = true;
                        goToLandmarkOne = false;

                    } else {
                        if (tangoSerialConnection != null) {
                            sendRobotCommand(read.charAt(0));
                            speakDirection(read.charAt(0));
                        }
                    }
                    updateConversationHandler.post(new updateUIThread(read));

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private void saveAdf(final String name) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String adfUuid = null;
                try {
                    // Save the ADF.
                    adfUuid = mTango.saveAreaDescription();

                    // Read the ADF Metadata, set the desired name, and save it back.
                    TangoAreaDescriptionMetaData metadata = mTango.loadAreaDescriptionMetaData(adfUuid);
                    metadata.set(TangoAreaDescriptionMetaData.KEY_NAME, name.getBytes());
                    mTango.saveAreaDescriptionMetadata(adfUuid, metadata);

                } catch (TangoErrorException e) {
                    Log.w(TAG, "TangoErrorException saving adf!");
                } catch (TangoInvalidException e) {
                    Log.w(TAG, "TangoInvalidException saving adf!");
                }
            }
        }).start();
    }

    private String getLandmark(int num) {
        if (num == 1) {
            return landmark1Name;
        } else if(num == 2) {
            return landmark2Name;
        }
        return "";
    }

    class updateUIThread implements Runnable {
        private String msg;

        public updateUIThread(String str) {
            this.msg = str;
        }

        @Override
        public void run() {
            mostRecentMessageTextView.setText(mostRecentMessageTextView.getText().toString()+"Client Says: "+ msg + "\n");
        }
    }

    private void goToLocation(final double[] target) {

        new Thread(new Runnable() {
            @Override
            public void run() {

                Long lastTime = System.currentTimeMillis();
                sendSpeakString("Starting");

                NavigationInfo navigationInfo = navigationLogic.navigationInfo(currentPose.translation, currentPose.rotation, target);
                char command = navigationInfo.getCommand();

                char previousCommand = 'z';
                boolean timeToStop = false;

                while (!timeToStop) {

                    if (System.currentTimeMillis() - lastTime > 100) {

                        if (command == CommandValues.MOVE_STOP) {
                            timeToStop = true;
                        }

                        lastTime = System.currentTimeMillis();
                        final char comm = command;

                        if (command != previousCommand) {
                            sendRobotCommand(command);

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    currentCommandTextView.setText("Current command: " + comm);
                                }
                            });
                        }
                        previousCommand = command;
                        NavigationInfo newNavInfo = navigationLogic.navigationInfo(currentPose.translation, currentPose.rotation, target);
                        command = newNavInfo.getCommand();
                    }
                }
                sendSpeakString("Engaging target");
            }
        }).start();
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

    private void speakDirection(char command) {

        switch (command) {
            case CommandValues.MOVE_FORWARD:
                sendSpeakString("Moving forward");
                break;
            case CommandValues.MOVE_REVERSE:
                sendSpeakString("Moving in reverse");
                break;
            case CommandValues.MOVE_LEFT:
                sendSpeakString("Moving left");
                break;
            case CommandValues.MOVE_RIGHT:
                sendSpeakString("Moving right");
                break;
            case CommandValues.MOVE_STOP:
                sendSpeakString("Stopping");
                break;
            default:
                sendSpeakString("Unknown direction command");
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Stop threads
        serverThread.interrupt();
        ttsThread.interrupt();
    }


    @Override
    protected void onStop() {
        super.onStop();
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

