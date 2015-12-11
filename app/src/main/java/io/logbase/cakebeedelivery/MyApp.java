package io.logbase.cakebeedelivery;

/**
 * Created by logbase on 30/11/15.
 */
import android.app.Application;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.support.v4.content.LocalBroadcastManager;
import android.content.BroadcastReceiver;
import io.logbase.stickandroidsdk.StickMobile;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;
import java.util.Calendar;
import android.widget.TextView;

public class MyApp extends Application {

    private StickMobile stick;
    private Integer frequency;

    private void startTracking(Integer frequency) {
        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
        String deviceID = sharedPref.getString("deviceID", null);
        boolean stickStarted;

        if(stick == null) {
            IntentFilter mStatusIntentFilter = new IntentFilter("STICK_MOBILE_BROADCAST");
            // Instantiates a new DownloadStateReceiver
            StatusReceiver mStatusReceiver = new StatusReceiver();
            // Registers the DownloadStateReceiver and its intent filters
            LocalBroadcastManager.getInstance(this).registerReceiver(
                    mStatusReceiver,
                    mStatusIntentFilter);

            stick = new StickMobile(this, deviceID, frequency, null);
            stickStarted = stick.start();
        }
        else if(stick.isRunning() && this.frequency != frequency) {
            stick.stop();

            stick = new StickMobile(this, deviceID, frequency, null);
            stickStarted = stick.start();
        }
        else {
            stickStarted = stick.start();
        }

        if (!stickStarted)
            ShowToast("Unable to start if blank device ID, no Network or GPS");

        this.frequency = frequency;
    }

    public void stopTracking() {
        if(stick != null) {
            stick.stop();
        }
    }

    public void startDefaultTracking(){
        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
        String trackDefault = sharedPref.getString("TrackDefault", null);

        if(trackDefault.contains("true")) {
            Integer trackDefaultFreq = sharedPref.getInt("TrackDefaultFreq", 0);
            startTracking(trackDefaultFreq);
        }
        else{
            stopTracking();
        }
    }

    public void startOrderTracking(){
        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);

        Integer trackOrderFreq = sharedPref.getInt("TrackOrderFreq", 0);
        startTracking(trackOrderFreq);
    }

    private void ShowToast(String message) {
        Toast toast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        toast.show();
    }

    public class StatusReceiver extends BroadcastReceiver {
        private static final String LOG_TAG = "StickStatusReceiver";
        // Called when the BroadcastReceiver gets an Intent it's registered to receive
        public void onReceive(Context context, Intent intent) {
            String status = intent.getExtras().getString("SERVICE_STATUS");
            Log.i(LOG_TAG, "Received status: " + status);
            if((status != null)&&(status.equals("STOP"))) {
                ShowToast("Unable to run service, check if GPS and Network is connected");
            }
            else {
                //Received data that was sent to server
                if(OrderDetailActivity.myActivity != null) {
                    Calendar cal = Calendar.getInstance();
                    String date = android.text.format.DateFormat.format("MMM dd, yyyy hh:mm:ss a", cal.getTime()).toString();
                    TextView clock = (TextView) OrderDetailActivity.myActivity.findViewById(R.id.clock);
                    clock.setText(date);
                }
            }
        }
    }
}
