package io.logbase.cakebeedelivery;

import android.app.ProgressDialog;
import android.content.Context;


/**
 * Created by logbase on 22/12/15.
 */
public class LBProcessDialog {
    ProgressDialog mDialog = null;

    public LBProcessDialog(Context context) {
        mDialog = new ProgressDialog(context);
        mDialog.setMessage("Please wait...");
        mDialog.setCancelable(false);
    }

    public void StartProcessDialog() {
        if(mDialog != null)
            mDialog.show();
    }

    public void StopProcessDialog() {
        if(mDialog != null)
            mDialog.hide();
    }
}
