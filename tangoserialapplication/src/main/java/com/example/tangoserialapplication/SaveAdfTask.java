package com.example.tangoserialapplication;

// This file is from the Google Tango Area Learning Tutorial

import com.google.atap.tangoservice.Tango;
import com.google.atap.tangoservice.TangoAreaDescriptionMetaData;
import com.google.atap.tangoservice.TangoErrorException;
import com.google.atap.tangoservice.TangoInvalidException;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

/**
 * Saves the ADF on a background thread and shows a progress dialog while
 * saving.
 */
public class SaveAdfTask extends AsyncTask<Void, Integer, String> {

    public interface SaveAdfListener {
        void onSaveAdfFailed(String adfName);
        void onSaveAdfSuccess(String adfName, String adfUuid);
    }

    Context mContext;
    SaveAdfListener mCallbackListener;
    SaveAdfDialog mProgressDialog;
    Tango mTango;
    String mAdfName;

    SaveAdfTask(Context context, SaveAdfListener callbackListener, Tango tango, String adfName) {
        mContext = context;
        mCallbackListener = callbackListener;
        mTango = tango;
        mAdfName = adfName;
        mProgressDialog = new SaveAdfDialog(context);
    }

    /**
     * Call this method to marshall progress updates to the UI thread.
     */
    public void publishProgress(int progress) {
        super.publishProgress(progress);
    }


    /**
     * Sets up the progress dialog.
     */
    @Override
    protected void onPreExecute() {
        if (mProgressDialog != null) {
            mProgressDialog.show();
        }
    }

    /**
     * Performs long-running save in the background.
     */
    @Override
    protected String doInBackground(Void... params) {
        String adfUuid = null;
        try {
            // Save the ADF.
            adfUuid = mTango.saveAreaDescription();

            // Read the ADF Metadata, set the desired name, and save it back.
            TangoAreaDescriptionMetaData metadata = mTango.loadAreaDescriptionMetaData(adfUuid);
            metadata.set(TangoAreaDescriptionMetaData.KEY_NAME, mAdfName.getBytes());
            mTango.saveAreaDescriptionMetadata(adfUuid, metadata);

        } catch (TangoErrorException e) {
            adfUuid = null; // There's currently no additional information in the exception.
        } catch (TangoInvalidException e) {
            adfUuid = null; // There's currently no additional information in the exception.
        }
        return adfUuid;
    }

    /**
     * Responds to progress updates events by updating the UI.
     */
    @Override
    protected void onProgressUpdate(Integer... progress) {
        if (mProgressDialog != null) {
            mProgressDialog.setProgress(progress[0]);
        }
    }

    /**
     * Dismisses the progress dialog and call the activity.
     */
    @Override
    protected void onPostExecute(String adfUuid) {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
        if (mCallbackListener != null) {
            if (adfUuid == null) {
                mCallbackListener.onSaveAdfFailed(mAdfName);
            } else {
                mCallbackListener.onSaveAdfSuccess(mAdfName, adfUuid);
            }
        }
    }
}