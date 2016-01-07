package io.logbase.cakebeedelivery;

/**
 * Created by logbase on 30/11/15.
 */
import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;
import android.content.BroadcastReceiver;
import io.logbase.stickandroidsdk.StickMobile;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import android.widget.TextView;
import android.app.Activity;
import java.util.Map;
import java.util.HashMap;
import android.support.v7.app.NotificationCompat;

public class MyApp extends Application {

    private StickMobile stick;
    private Integer frequency;
    private Map<String, String>  yettodeliverorders= new HashMap<String, String>();
    private Map<String, String>  yettopickuporders= new HashMap<String, String>();

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

    public void addOrders(String ordernumber, String deliverytime, boolean isam, boolean yettopick) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
        String currentDate = sdf.format(new java.util.Date());

        deliverytime = deliverytime.replace(".", ":");
        if(!deliverytime.contains(":")) {
            deliverytime = deliverytime + ":00";
        }

        String time = currentDate + " " + deliverytime + " " + (isam ? "AM" : "PM");
        if(yettopick) {
            yettopickuporders.put(ordernumber, time);
        }
        else {
            yettodeliverorders.put(ordernumber, time);
        }
    }

    public void removeOrders(String ordernumber, boolean ispickedup) {
        if(ispickedup) {
            yettopickuporders.remove(ordernumber);
        }
        else {
            yettodeliverorders.remove(ordernumber);
        }
    }

    public Map<String, String> getOrders(boolean yettopick) {
        if(yettopick)
            return yettopickuporders;
        else
            return yettodeliverorders;
    }

    public void Clear(boolean yettopick) {
        if(yettopick) {
            yettopickuporders = new HashMap<String, String>();
        }
        else {
            yettodeliverorders = new HashMap<String, String>();
        }
    }

    public void Notify(String message){
        try {
            // define sound URI, the sound to be played when there's a notification
            SharedPreferences sharedPref = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
            String savedalerttype = sharedPref.getString("alerttype", null);
            int ringtonetype = RingtoneManager.TYPE_RINGTONE;

            if(savedalerttype != null && savedalerttype.contains("Notification")) {
                ringtonetype = RingtoneManager.TYPE_NOTIFICATION;
            }

            Intent notificationIntent = new Intent(this, OrdersActivity.class);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            PendingIntent intent = PendingIntent.getActivity(this, 0,
                    notificationIntent, 0);

            Uri soundUri = RingtoneManager.getDefaultUri(ringtonetype);
            NotificationManager notificationManager = (NotificationManager) this
                    .getSystemService(Context.NOTIFICATION_SERVICE);

            NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
            bigTextStyle.setBigContentTitle("Delivery");
            bigTextStyle.bigText(message);

            Notification notification = new Notification.Builder(this)
                    .setContentTitle("Delivery")
                    .setContentIntent(intent)
                    .setAutoCancel(true)
                    .setContentText(message)
                    .setStyle(new Notification.BigTextStyle().bigText(message))
                    .setSmallIcon(R.drawable.icon)
                    .setSound(soundUri)
                    .setFullScreenIntent(intent, true)
                    .setVibrate(new long[] {1000, 1000, 1000, 1000, 1000})
                    .getNotification();

            notification.flags |= Notification.FLAG_AUTO_CANCEL;
            notificationManager.notify(0, notification);
        }
        catch (Exception e) {
            ShowToast(e.getMessage());
        }
    }

    private void ShowToast(String message) {
        Toast toast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        toast.show();
    }

    private Activity mCurrentActivity = null;
    public Activity getCurrentActivity(){
        return mCurrentActivity;
    }
    public void setCurrentActivity(Activity mCurrentActivity){
        this.mCurrentActivity = mCurrentActivity;
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
