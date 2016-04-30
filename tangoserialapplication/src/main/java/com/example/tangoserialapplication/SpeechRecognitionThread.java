package com.example.tangoserialapplication;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import java.util.ArrayList;

/**
 * Created by brandonsladek on 4/13/16.
 */

public class SpeechRecognitionThread implements Runnable, RecognitionListener {

    public Handler handler;
    private Context context;
    private SpeechRecognizer speech = null;
    private Intent recognizerIntent;
    private AutonomousControlActivity autonomousControlActivity;

    private static SpeechRecognitionThread instance = null;

    public static SpeechRecognitionThread getInstance() {
        if (instance == null) {
            return new SpeechRecognitionThread();
        }
        return instance;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public void setAutonomousControlActivity(AutonomousControlActivity autonomousControlActivity) {
        this.autonomousControlActivity = autonomousControlActivity;
    }

    @Override
    public void run() {

//        Looper.prepare();
//
//        speech = SpeechRecognizer.createSpeechRecognizer(autonomousControlActivity.getApplicationContext());
//        speech.setRecognitionListener(this);
//        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
//        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,
//                "en");
//        recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,
//                context.getPackageName());
//        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
//                RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
//        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
//
//        handler = new Handler() {
//            @Override
//            public void handleMessage(Message msg) {
//                switch(msg.getData().getString("SPEECH_REC_COMMAND")) {
//                    case "start":
//                        speech.startListening(recognizerIntent);
//                        break;
//                    case "stop":
//                        speech.stopListening();
//                    default:
//                        break;
//                }
//            }
//        };
//
//        Looper.loop();
    }

    @Override
    public void onReadyForSpeech(Bundle params) {
        // ignore
    }

    @Override
    public void onBeginningOfSpeech() {
        // ignore
    }

    @Override
    public void onRmsChanged(float rmsdB) {
        // ignore
    }

    @Override
    public void onBufferReceived(byte[] buffer) {
        // ignore
    }

    @Override
    public void onEndOfSpeech() {
        // ignore
    }

    @Override
    public void onError(int error) {
        // ignore
    }

    @Override
    public void onResults(Bundle results) {
        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

        String text = "";
        for (String result : matches)
            text += result + "\n";

        // what to do with it here?

        switch(text) {
            case "go to landmark one":
                autonomousControlActivity.speechLandmarkName = "one";
                break;
            case "go to landmark two":
                break;
            default:
                break;
        }


    }

    @Override
    public void onPartialResults(Bundle partialResults) {
        // ignore
    }

    @Override
    public void onEvent(int eventType, Bundle params) {
        // ignore
    }
}
