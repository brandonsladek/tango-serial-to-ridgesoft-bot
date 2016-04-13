package com.example.tangoserialapplication;

import android.app.Activity;
import android.os.Bundle;
import android.widget.RadioButton;

/**
 * Created by brandonsladek on 4/12/16.
 */
public class AutonomousControlActivity extends Activity {

    private RadioButton stopRadioButton;
    private TangoSerialConnection tsConn;
    private NavigationLogic navigationLogic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ac_activity);

        stopRadioButton = (RadioButton) findViewById(R.id.ac_stopButton);

        tsConn = (TangoSerialConnection) getIntent().getSerializableExtra("TangoSerialConnection");

        if (tsConn != null) {
            navigationLogic = new NavigationLogic(tsConn);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        while (!stopRadioButton.isChecked()) {
            // navigate... how do I get pose updates if the main activity is paused?!
        }
    }

}
