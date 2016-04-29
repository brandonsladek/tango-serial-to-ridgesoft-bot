package com.example.tangoserialapplication;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

/**
 * Created by brandonsladek on 4/13/16.
 */

public class SpeechRecognitionThread implements Runnable, RecognitionListener {

    public Handler handler;

    @Override
    public void run() {
        Looper.prepare();

        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {

            }
        };

        Looper.loop();

    }

//    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
//    speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
//    speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
//    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
//    speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,
//            this.getPackageName());
//
//
//    SpeechRecognitionListener listener = new SpeechRecognitionListener();
//    speechRecognizer.setRecognitionListener(listener);

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

    }

    @Override
    public void onPartialResults(Bundle partialResults) {

    }

    @Override
    public void onEvent(int eventType, Bundle params) {

    }
}
