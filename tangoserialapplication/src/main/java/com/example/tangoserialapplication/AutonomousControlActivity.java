package com.example.tangoserialapplication;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentCallbacks;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Message;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
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

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

/** Brandon Sladek and John Waters */

public class AutonomousControlActivity extends Activity implements View.OnClickListener, RecognitionListener {

    private TangoSerialConnection tangoSerialConnection;
    private Thread serialThread;
    private TangoPoseData currentPose;
    private Tango mTango;
    private TangoConfig mConfig;

    private TextToSpeechThread tts;
    Thread ttsThread;

    private final Object mSharedLock = new Object();
    private NavigationLogic navigationLogic;

    private boolean mIsRelocalized = false;
    private boolean mIsLearningMode = false;
    private boolean mIsConstantSpaceRelocalize = true;
    private String adfToLoadName;

    private TextView poseDataTextView;
    private TextView adfUUIDTextView;
    private TextView directionCommandTextView;
    private TextView localizationStatusTextView;
    private TextView ourRotationTextView;
    private TextView goRotationTextView;
    private TextView landmarkOneTextView;
    private Button saveLandmarkOneButton;
    private Button goToLandmarkOneButton;
    private TextView landmarkTwoTextView;
    private Button saveLandmarkTwoButton;
    private Button goToLandmarkTwoButton;
    private Button speakCommandButton;
    private TextView spokenCommandTextView;
    private Button recordSafePathButton;
    private Button stopSafePathButton;
    private Button followSafePathButton;
    private Button viewSafePathButton;
    private Button deleteSafePathButton;

    private boolean driftCorrectionMode = false;
    private boolean equalizingRotationsMode = false;
    private boolean goToLandmarkByName = false;

    private TargetLocation currentTargetLandmark;

    public String speechLandmarkName;

    private boolean recordingSafePath = false;
    private boolean safePathRecorded = false;
    ArrayList<SafePoint> safePoints = new ArrayList<>();

    private SafePath safePath;
    private HashMap<String, TargetLocation> landmarks = new HashMap<>();

    private SpeechRecognizer speech = null;
    private Intent recognizerIntent;

    private NavigationInfo navigationInfo;
    private CourseInfo courseInfo;

    Long lastUpdateTime;

    // Red X in middle of classroom floor
    private double[] targetLocation = new double[]{6.4, 3.82, 1.0};
    private double[] landmarkOneLocation = new double[3];
    private double[] landmarkTwoLocation = new double[3];

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_autonomous_control);

        // Buttons and TextViews!
        poseDataTextView = (TextView) findViewById(R.id.ac_poseDataTextView);
        adfUUIDTextView = (TextView) findViewById(R.id.ac_adfUUIDTextView);
        directionCommandTextView = (TextView) findViewById(R.id.ac_directionCommandTextView);
        ourRotationTextView = (TextView) findViewById(R.id.ac_ourRotationTextView);
        goRotationTextView = (TextView) findViewById(R.id.ac_goRotationTextView);
        localizationStatusTextView = (TextView) findViewById(R.id.ac_localizationStatusTextView);
        localizationStatusTextView.setText("Not localized!");
        landmarkOneTextView = (TextView) findViewById(R.id.ac_landmarkOneTextView);
        saveLandmarkOneButton = (Button) findViewById(R.id.ac_saveLandmarkOneButton);
        goToLandmarkOneButton = (Button) findViewById(R.id.ac_goToLandmarkOneButton);
        speakCommandButton = (Button) findViewById(R.id.ac_speakCommandButton);
        spokenCommandTextView = (TextView) findViewById(R.id.ac_spokenCommandTextView);
        landmarkTwoTextView = (TextView) findViewById(R.id.ac_landmarkTwoTextView);
        saveLandmarkTwoButton = (Button) findViewById(R.id.ac_saveLandmarkTwoButton);
        goToLandmarkTwoButton = (Button) findViewById(R.id.ac_goToLandmarkTwoButton);
        recordSafePathButton = (Button) findViewById(R.id.ac_recordSafePathButton);
        stopSafePathButton = (Button) findViewById(R.id.ac_stopSafePathButton);
        followSafePathButton = (Button) findViewById(R.id.ac_followSafePathButton);
        viewSafePathButton = (Button) findViewById(R.id.ac_viewSafePathButton);
        deleteSafePathButton = (Button) findViewById(R.id.ac_deleteSafePathButton);

        saveLandmarkOneButton.setOnClickListener(this);
        saveLandmarkTwoButton.setOnClickListener(this);
        goToLandmarkOneButton.setOnClickListener(this);
        goToLandmarkTwoButton.setOnClickListener(this);
        speakCommandButton.setOnClickListener(this);
        recordSafePathButton.setOnClickListener(this);
        stopSafePathButton.setOnClickListener(this);
        followSafePathButton.setOnClickListener(this);
        viewSafePathButton.setOnClickListener(this);
        deleteSafePathButton.setOnClickListener(this);

        // Start thread for usb serial connection
        tangoSerialConnection = TangoSerialConnection.getInstance();
        tangoSerialConnection.setUsbManager((UsbManager) this.getSystemService(Context.USB_SERVICE));
        serialThread = new Thread(tangoSerialConnection);
        serialThread.start();

        // Start thread for text to speech
        tts = TextToSpeechThread.getInstance();
        tts.setContext(getApplicationContext());
        ttsThread = new Thread(tts);
        ttsThread.start();

        ComponentName caller = getCallingActivity();

        if (caller != null) {
            if (caller.getClass().getSimpleName().equals("NetworkControlActivity")) {
                courseInfo = (CourseInfo) getIntent().getSerializableExtra("COURSE_INFO");
                adfToLoadName = getIntent().getStringExtra("ADF_TO_LOAD");
            }
        }

        if (courseInfo != null) {
            safePath = courseInfo.getSafePath();
            landmarks = courseInfo.getTargetLocationsByName();
        }

        speech = SpeechRecognizer.createSpeechRecognizer(this);
        speech.setRecognitionListener(this);
        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,
                "en");
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,
                this.getPackageName());
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);


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

    /**
     * Sets up the tango configuration object. Make sure mTango object is initialized before
     * making this call.
     */
    private TangoConfig setTangoConfig(Tango tango, boolean isLearningMode, boolean isLoadAdf) {

        TangoConfig config = tango.getConfig(TangoConfig.CONFIG_TYPE_CURRENT);
        config.putBoolean(TangoConfig.KEY_BOOLEAN_DEPTH, true);

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
            public void onXyzIjAvailable(TangoXyzIjData xyzIj) {

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
                            sendSpeakString("Localized");

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

                            //checkForCurrentTarget();

                            currentPose = pose;
                            double[] ourLocation = roundLocation(pose.translation);
                            updateTextViews(pose, null);

                            if (driftCorrectionMode) {
                                driftCorrectionMode(pose, ourLocation);
                            }

                            else if (equalizingRotationsMode) {
                                equalizeRotations(pose, currentTargetLandmark);
                            }

                            else if (goToLandmarkByName) {
                                goToTargetLandmark(pose, currentTargetLandmark);
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

    private void goToTargetLandmark(TangoPoseData pose, TargetLocation targetLandmark) {

        double[] ourLocation = roundLocation(pose.translation);

        // Throttle pose updates by a tenth of a second
        if (System.currentTimeMillis() - lastUpdateTime > 100) {

            if (safePathRecorded) {
                double[] closestSafePathPoint = roundLocation(safePath.getClosestSafePathPoint(ourLocation));
                double distanceFromSafePath = navigationLogic.getDistance(ourLocation, closestSafePathPoint);

                if (distanceFromSafePath > 0.075) {
                    sendSpeakString("Entering drift correction mode");
                    driftCorrectionMode = true;
                    return;
                }
            }

            navigationInfo = navigationLogic.navigationInfo(ourLocation, pose.rotation, roundLocation(targetLandmark.getTargetLocation()));
            sendRobotCommand(navigationInfo.getCommand());

            if (navigationInfo.getCommand() == 's') {
                goToLandmarkByName = false;
                equalizingRotationsMode = true;
            }

            updateTextViews(pose, navigationInfo);
            lastUpdateTime = System.currentTimeMillis();
        }
    }

    private void driftCorrectionMode(TangoPoseData pose, double[] ourLocation) {
        double[] closestSafePathPoint = roundLocation(safePath.getClosestSafePathPoint(ourLocation));
        double distanceFromSafePath = navigationLogic.getDistance(ourLocation, closestSafePathPoint);

        if (distanceFromSafePath < 0.075) {
            driftCorrectionMode = false;
            return;
        }

        // Throttle pose updates by a tenth of a second
        if (System.currentTimeMillis() - lastUpdateTime > 100) {

            navigationInfo = navigationLogic.navigationInfo(ourLocation, pose.rotation, closestSafePathPoint);
            sendRobotCommand(navigationInfo.getCommand());

            lastUpdateTime = System.currentTimeMillis();
        }
    }

    private void equalizeRotations(TangoPoseData pose, TargetLocation targetLocation) {
        if (System.currentTimeMillis() - lastUpdateTime > 100) {

            char command = navigationLogic.equalizeRotations((int) getOurRotation(pose.rotation), targetLocation.getRotation());
            sendRobotCommand(command);

            if (command == 's') {
                equalizingRotationsMode = false;
                sendSpeakString("Engaging target");
            }

            lastUpdateTime = System.currentTimeMillis();
        }
    }

    private double[] roundLocation(double[] originalLocation) {
        return new double[]{round(originalLocation[0]), round(originalLocation[1]), round(originalLocation[2])};
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

                if (navigationInfo != null) {
                    directionCommandTextView.setText("Command: " + navigationInfo.getCommand());
                    goRotationTextView.setText("Go: " + navigationInfo.getGoRotation());
                    ourRotationTextView.setText("Our: " + navigationInfo.getOurRotation());
                }
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
            Toast.makeText(getApplicationContext(), "TangoErrorException with setting up listeners!", Toast.LENGTH_SHORT).show();
        } catch (SecurityException e) {
            Toast.makeText(getApplicationContext(), "SecurityException!", Toast.LENGTH_SHORT).show();
        }

        // Connect to the tango service (start receiving pose updates).
        try {
            mTango.connect(mConfig);
        } catch (TangoOutOfDateException e) {
            Toast.makeText(getApplicationContext(), "TangoOutOfDateException!", Toast.LENGTH_SHORT).show();
        } catch (TangoErrorException e) {
            Toast.makeText(getApplicationContext(), "TangoErrorException connecting Tango!", Toast.LENGTH_SHORT).show();
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

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ac_saveLandmarkOneButton:

                if (mIsRelocalized) {
                    final TargetLocation landmarkOne = new TargetLocation(roundLocation(currentPose.translation), (int) getOurRotation(currentPose.rotation));
                    landmarks.put("one", landmarkOne);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            landmarkOneTextView.setText(landmarkOne.getTargetLocationAsString());
                        }
                    });
                    sendSpeakString("Target one recorded");
                }
                break;

            case R.id.ac_saveLandmarkTwoButton:

                if (mIsRelocalized) {
                    final TargetLocation landmarkTwo = new TargetLocation(roundLocation(currentPose.translation), (int) getOurRotation(currentPose.rotation));
                    landmarks.put("two", landmarkTwo);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            landmarkTwoTextView.setText(landmarkTwo.getTargetLocationAsString());
                        }
                    });
                    sendSpeakString("Target two recorded");
                }
                break;

            case R.id.ac_goToLandmarkOneButton:
                goToLandmarkByName = true;
                currentTargetLandmark = landmarks.get("one");
                break;

            case R.id.ac_goToLandmarkTwoButton:
                goToLandmarkByName = true;
                currentTargetLandmark = landmarks.get("two");
                break;

            case R.id.ac_speakCommandButton:
                speech.startListening(recognizerIntent);
                break;

            case R.id.ac_recordSafePathButton:
                recordingSafePath = true;
                sendSpeakString("Recording safe path");
                break;

            case R.id.ac_stopSafePathButton:
                recordingSafePath = false;
                safePathRecorded = true;
                sendSpeakString("Done recording safe path");
                safePath = new SafePath(safePoints);
                break;

            case R.id.ac_followSafePathButton:
                sendSpeakString("Going to start of safe path");
                break;

            case R.id.ac_viewSafePathButton:
                Toast.makeText(getApplicationContext(), safePath.getSafePathString(), Toast.LENGTH_LONG).show();
                break;

            case R.id.ac_deleteSafePathButton:
                safePoints = new ArrayList<>();
                safePathRecorded = false;
                sendSpeakString("Safe path deleted");
                break;

            default:
                break;
        }
    }

    @Override
    public void onReadyForSpeech(Bundle params) {

    }

    @Override
    public void onBeginningOfSpeech() {

    }

    @Override
    public void onRmsChanged(float rmsdB) {

    }

    @Override
    public void onBufferReceived(byte[] buffer) {

    }

    @Override
    public void onEndOfSpeech() {

    }

    @Override
    public void onError(int error) {

    }

    @Override
    public void onResults(Bundle results) {
        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

        final String spokenText = matches.get(0);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                spokenCommandTextView.setText(spokenText);
            }
        });

        switch(spokenText) {
            case "go to first landmark":
                goToLandmarkByName = true;
                currentTargetLandmark = landmarks.get("one");
                break;
            case "go to second landmark":
                goToLandmarkByName = true;
                currentTargetLandmark = landmarks.get("two");
                break;
            default:
                sendSpeakString("I have no idea what you are saying");
                break;
        }
    }

    @Override
    public void onPartialResults(Bundle partialResults) {

    }

    @Override
    public void onEvent(int eventType, Bundle params) {

    }
}
