package io.logbase.stickandroidsdk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

/**
 * Created by abishek on 10/07/15.
 */
public class StickMobile {

    private Context context;
    private boolean isRunning;
    private String deviceID;
    private int frequency;

    public StickMobile(Context context, String deviceID, int frequency, String token){
        if(context == null)
            throw new IllegalArgumentException("Context passed is null");
        if ( (deviceID == null) || (deviceID.length() == 0) )
            throw new IllegalArgumentException("Device ID is invalid");
        if ( (frequency < 1) || (frequency > 60))
            throw new IllegalArgumentException("Frequency should be between 1 second to 60 seconds");
        this.context = context;
        this.deviceID = deviceID;
        this.frequency = frequency;
        SharedPreferences sharedPref = context.getSharedPreferences("StickMobile", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("stickState","OFF");
        editor.commit();
        this.isRunning = false;
    }

    public String pingTest() {
        return "Hello";
    }

    public boolean start() {
        if(!isRunning) {

            //Check GPS and Network
            LocationManager lm = (LocationManager) context.getSystemService(context.LOCATION_SERVICE);
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            boolean networkReady = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
            //If GPS and Network are available then start
            if( lm.isProviderEnabled(LocationManager.GPS_PROVIDER) && networkReady) {
                SharedPreferences sharedPref = context.getSharedPreferences("StickMobile", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.remove("stickState");
                editor.commit();
                Intent trackerIntent = new Intent(context, TrackerIntentService.class);
                trackerIntent.putExtra("deviceID", deviceID);
                trackerIntent.putExtra("frequency", frequency);
                context.startService(trackerIntent);
                isRunning = true;
                return true;
            } else
                return false;
        } else
            return false;
    }

    public boolean stop() {
            SharedPreferences sharedPref = context.getSharedPreferences("StickMobile", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("stickState","OFF");
            editor.commit();
            isRunning = false;
            return true;
    }

    /*
    private class StatusReceiver extends BroadcastReceiver {
        private static final String LOG_TAG = "StickStatusReceiver";
        // Called when the BroadcastReceiver gets an Intent it's registered to receive
        public void onReceive(Context context, Intent intent) {
            String status = intent.getExtras().getString("SERVICE_STATUS");
            Log.i(LOG_TAG, "Received status: " + status);
            if((status != null)&&(status.equals("STOP"))) {
                stop();
            }
        }
    }
    */

    public boolean isRunning() {
        return isRunning;
    }

}
