package com.example.tangoserialapplication;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.speech.tts.TextToSpeech;

import java.util.Locale;

/**
 * Created by brandonsladek on 4/13/16.
 */

public class TextToSpeechThread implements Runnable {

    private static TextToSpeechThread instance;
    private TextToSpeech tts;
    private Context context;
    public Handler handler;

    private TextToSpeechThread() {}

    public static TextToSpeechThread getInstance() {
        if (instance == null) {
            instance = new TextToSpeechThread();
        }
        return instance;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    @Override
    public void run() {
         tts = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
             @Override
             public void onInit(int status) {
                 if(status != TextToSpeech.ERROR) {
                     tts.setLanguage(Locale.US);
                 }
             }
         });

        Looper.prepare();

        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                String toSpeak = msg.getData().getString("SPEAK_TEXT");
                tts.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
            }
        };

        Looper.loop();
    }
}
