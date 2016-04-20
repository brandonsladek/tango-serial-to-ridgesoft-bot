package com.example.tangoserialapplication;

// This file is from the Google Tango Area Learning Tutorial

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ProgressBar;

/**
 * Displays progress bar and text information while saving an adf.
 */
public class SaveAdfDialog extends AlertDialog {
    private static final String TAG = SaveAdfDialog.class.getSimpleName();

    private ProgressBar mProgressBar;

    public SaveAdfDialog(Context context) {
        super(context);
    }

    public void setProgress(int progress) {
        if (mProgressBar != null) {
            mProgressBar.setVisibility(View.VISIBLE);
            mProgressBar.setProgress(progress);
        }
    }

    public void setProgressBarVisibility(int visibility) {
        if (mProgressBar != null) {
            mProgressBar.setVisibility(visibility);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.save_adf_dialog);

        setCancelable(false);

        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
        if (mProgressBar == null) {
            Log.e(TAG, "Unable to find view progress_bar.");
        }
    }
}
