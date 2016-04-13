package com.example.tangoserialapplication;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/**
 * Created by brandonsladek on 4/12/16.
 */
public class ManualControlActivity extends Activity {

    private Button forwardButton;
    private Button backButton;
    private Button leftButton;
    private Button rightButton;
    private Button stopButton;

    private TextView connectionTextView;
    private TextView poseDataTextView;

    private TangoSerialConnection tsConn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mc_activity);

        forwardButton = (Button) findViewById(R.id.forwardButton);
        backButton = (Button) findViewById(R.id.backButton);
        leftButton = (Button) findViewById(R.id.leftButton);
        rightButton = (Button) findViewById(R.id.rightButton);
        stopButton = (Button) findViewById(R.id.stopButton);

        tsConn = (TangoSerialConnection) getIntent().getSerializableExtra("TangoSerialConnection");

        poseDataTextView = (TextView) findViewById(R.id.poseDataTextView);
        connectionTextView = (TextView) findViewById(R.id.connectionTextView);

        if (tsConn != null) {
            connectionTextView.setText("Connected Successfully!");

            forwardButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    tsConn.handleMessage('f');
                }
            });
            backButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    tsConn.handleMessage('b');
                }
            });
            leftButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    tsConn.handleMessage('l');
                }
            });
            rightButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    tsConn.handleMessage('r');
                }
            });
            stopButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    tsConn.handleMessage('s');
                }
            });
        } else {
            connectionTextView.setText("Not connected...");
        }
    }
}
