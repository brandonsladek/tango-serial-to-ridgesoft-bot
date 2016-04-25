package com.example.tangoserialapplication;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

/**
 * Created by brandonsladek on 4/13/16.
 */

public class SpeechRecognitionThread implements Runnable {

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

}
