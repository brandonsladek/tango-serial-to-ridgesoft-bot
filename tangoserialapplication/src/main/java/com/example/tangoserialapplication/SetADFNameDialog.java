package com.example.tangoserialapplication;

// This file is from the Google Tango Area Learning Tutorial

import com.google.atap.tangoservice.TangoAreaDescriptionMetaData;

import android.app.Activity;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

/**
 * Queries the user for an ADF name, optionally showing the ADF UUID.
 */
public class SetADFNameDialog extends DialogFragment implements OnClickListener {

    EditText mNameEditText;
    TextView mUUIDTextView;
    CallbackListener mCallbackListener;

    interface CallbackListener {
        public void onAdfNameOk(String name, String uuid);
        public void onAdfNameCancelled();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mCallbackListener = (CallbackListener)activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflator, ViewGroup container,
                             Bundle savedInstanceState) {
        View dialogView = inflator.inflate(R.layout.set_name_dialog, null);
        getDialog().setTitle("Title");
        mNameEditText = (EditText) dialogView.findViewById(R.id.name);
        mUUIDTextView = (TextView) dialogView.findViewById(R.id.uuidDisplay);
        dialogView.findViewById(R.id.Ok).setOnClickListener(this);
        dialogView.findViewById(R.id.cancel).setOnClickListener(this);
        setCancelable(false);
        String name = this.getArguments().getString(TangoAreaDescriptionMetaData.KEY_NAME);
        String id = this.getArguments().getString(TangoAreaDescriptionMetaData.KEY_UUID);
        if (name != null) {
            mNameEditText.setText(name);
        }
        if (id != null) {
            mUUIDTextView.setText(id);
        }
        return dialogView;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.Ok:
                mCallbackListener.onAdfNameOk(
                        mNameEditText.getText().toString(),
                        mUUIDTextView.getText().toString());
                dismiss();
                break;
            case R.id.cancel:
                mCallbackListener.onAdfNameCancelled();
                dismiss();
                break;
        }
    }
}
