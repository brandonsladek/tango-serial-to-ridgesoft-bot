package com.example.tangoserialapplication;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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

public class NetworkControlActivity extends Activity {

    private ServerSocket serverSocket;
    private TextView mostRecentMessageTextView;
    private TangoSerialConnection tangoSerialConnection;
    private TextToSpeechThread tts;
    private NavigationLogic navigationLogic;
    private NetworkControlActivity context = this;
    private int SERVERPORT = 5010;
    private String landmark1Name;
    private String landmark2Name;
    private double[] landmark1;
    private double[] landmark2;
    private Tango mTango;
    private TangoConfig mConfig;
    private boolean mIsRelocalized = false;
    private boolean mIsLearningMode = false;
    private boolean mIsConstantSpaceRelocalize = true;
    private TangoPoseData currentPose;
    private final Object mSharedLock = new Object();
    private TextView poseDataTextView;
    private TextView currentCommandTextView;
    Handler updateConversationHandler;
    Thread serverThread = null;
    Thread ttsThread;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_network_control);

        mostRecentMessageTextView = (TextView) findViewById(R.id.nc_mostRecentMessageTextView);
        poseDataTextView = (TextView) findViewById(R.id.nc_poseDataTextView);
        currentCommandTextView = (TextView) findViewById(R.id.nc_currentCommandTextView);

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

            // Load the most recent ADF
            if (fullUUIDList.size() > 0) {
                config.putString(TangoConfig.KEY_STRING_AREADESCRIPTION,
                        fullUUIDList.get(fullUUIDList.size()-1));
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
                                    "\nRotation: " + (int) getPoseRotationDegrees(currentPose.rotation);

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
                        tangoSerialConnection = new TangoSerialConnection(context);
                        Thread thread = new Thread(tangoSerialConnection);
                        thread.start();
                        updateConversationHandler.post(new updateUIThread("Connected!"));

                    } else if (read.equals("recordADF")) {
                        //need to do something to start recording the ADF here.....

                    } else if (read.contains("save")) {
                        String landmarkName = read;
                        String commands[] = landmarkName.split(" ");

                        if (landmark1Name == null) {
                            landmark1Name = commands[1];
                            landmark1 = currentPose.translation;
                            sendSpeakString("Target 1 recorded");
                        } else {
                            landmark2Name = commands[1];
                            landmark2 = currentPose.translation;
                            sendSpeakString("Target 2 recorded");
                        }

                    } else if (read.equals("goto1")) {
                        goToLocation(landmark1);

                    } else if (read.equals("goto2")) {
                        goToLocation(landmark2);

                    } else {
                        if (tangoSerialConnection != null) {
                            sendRobotCommand(read.charAt(0));
                        }

                    }
                    updateConversationHandler.post(new updateUIThread(read));

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public String getLandmark(int num) {
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

                sendSpeakString("Starting");

                Long lastTime = System.currentTimeMillis();
                char previousCommand = 'z';
                char command = navigationLogic.navigate(currentPose.translation, currentPose.rotation, target);
                boolean timeToStop = false;

                while (!timeToStop) {

                    if (System.currentTimeMillis() - lastTime > 100) {

                        if (command == 's') {
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
                        command = navigationLogic.navigate(currentPose.translation, currentPose.rotation, target);
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

}

