package com.example.tangoserialapplication;

import android.app.Activity;
import android.content.Context;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/**
 * Created by brandonsladek on 4/12/16.
 */

public class ManualControlActivity extends Activity {

    private TangoSerialConnection tangoSerialConnection;

    private TextToSpeechThread tts;
    Thread ttsThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual_control);

        TextView connectionTextView;
        TextView poseDataTextView;

        Button forwardButton = (Button) findViewById(R.id.forwardButton);
        Button backButton = (Button) findViewById(R.id.backButton);
        Button leftButton = (Button) findViewById(R.id.leftButton);
        Button rightButton = (Button) findViewById(R.id.rightButton);
        Button stopButton = (Button) findViewById(R.id.stopButton);

        poseDataTextView = (TextView) findViewById(R.id.poseDataTextView);
        connectionTextView = (TextView) findViewById(R.id.connectionTextView);

        tangoSerialConnection = TangoSerialConnection.getInstance();
        tangoSerialConnection.setUsbManager((UsbManager) this.getSystemService(Context.USB_SERVICE));
        Thread thread = new Thread(tangoSerialConnection);
        thread.start();

        tts = TextToSpeechThread.getInstance();
        tts.setContext(getApplicationContext());
        ttsThread = new Thread(tts);
        ttsThread.start();


        if (tangoSerialConnection != null) {
            connectionTextView.setText("Connected Successfully!");

            forwardButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sendRobotCommand(CommandValues.MOVE_FORWARD);
                    sendSpeakString("Moving forward");
                }
            });
            backButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sendRobotCommand(CommandValues.MOVE_REVERSE);
                    sendSpeakString("Moving backwards");
                }
            });
            leftButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sendRobotCommand(CommandValues.MOVE_LEFT);
                    sendSpeakString("Moving left");
                }
            });
            rightButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sendRobotCommand(CommandValues.MOVE_RIGHT);
                    sendSpeakString("Moving right");
                }
            });
            stopButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sendRobotCommand(CommandValues.MOVE_STOP);
                    sendSpeakString("Stopping");
                }
            });
        } else {
            connectionTextView.setText("Not connected...");
        }
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
