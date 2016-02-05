package io.logbase.cakebeedelivery;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;

/**
 * Created by abishek on 10/07/15.
 */
public class TrackerIntentService extends IntentService implements
        ConnectionCallbacks, OnConnectionFailedListener, LocationListener {

    private static final String LOG_TAG = "StickMobileService";
    private int frequency;
    private GoogleApiClient mGoogleApiClient;
    private GoogleApiClient activityGoogleApiClient = null;
    private BroadcastReceiver receiver;
    private boolean runService = true;
    private Location lastLocation = null;
    private long lastLocationTime = 0;
    private static final boolean FIXED_FREQ_WRITE = true;
    private PowerManager.WakeLock wakeLock = null;
    private String deviceID;
    private String currentActivity = null;
    private int confidence = 0;
    private static final String API_HOST="http://stick-write-dev.logbase.io/api/locations/m1/";

    public TrackerIntentService() {
        super("TrackerIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.i(LOG_TAG, "Trip tracker service started.");

        /*
        //GPS and Network
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        ConnectivityManager cm = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean networkReady = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

        //Check is GPS and Network are available
        if( lm.isProviderEnabled(LocationManager.GPS_PROVIDER) && networkReady){
        */
            //Acquire wake lock:
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "StickWakelock");
            wakeLock.acquire();

            SharedPreferences sharedPref = this.getSharedPreferences("StickMobile",
                    Context.MODE_PRIVATE);

            deviceID = intent.getStringExtra("deviceID");
            frequency = intent.getIntExtra("frequency", 2);
            frequency = frequency * 1000;
            Log.i(LOG_TAG, "Device and Frequency: " + deviceID + "|" + frequency);

            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
            //GPS listener will be registered on connection
            mGoogleApiClient.connect();

            setActivityRecoginitionService();

            while(runService) {
                //Log.i(LOG_TAG, "Running service loop...");
                //Sleep for a frequency
                try {
                    Thread.sleep(frequency);
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Interrupted while sleeping : " + e);
                }

                //if fixedFrequencyWrite, write to file.
                if(FIXED_FREQ_WRITE) {
                    if(lastLocation != null) {

                        double lat = lastLocation.getLatitude();
                        double lon = lastLocation.getLongitude();
                        long ts = lastLocationTime;
                        double speed = lastLocation.getSpeed();
                        double accuracy = lastLocation.getAccuracy();
                        Log.i(LOG_TAG, "Received location: " + lastLocation.getLatitude() + "|" + lastLocation.getLongitude());
                        try {
                            JSONObject locationJson = new JSONObject();
                            locationJson.put("latitude", lat);
                            locationJson.put("longitude", lon);
                            locationJson.put("timestamp", ts);
                            locationJson.put("speed", speed);
                            locationJson.put("accuracy", accuracy);
                            String data = locationJson.toString();
                            String urlString = API_HOST + deviceID + "?data=" + data;
                            Log.i(LOG_TAG, "URL generated: " + urlString);
                            int responseCode = sendToServer(urlString);
                            if(responseCode == 200) {
                                //Broadcast to activity new state
                                Intent localIntent = new Intent("STICK_MOBILE_BROADCAST")
                                        .putExtra("SERVICE_STATUS", data);
                                // Broadcasts the Intent to receivers in this app.
                                LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
                            }
                            lastLocation = null;
                        } catch(Exception e) {
                            Log.e(LOG_TAG, "Error forming location JSON and posting to Server");
                        }
                    }
                }

                //Read flag again
                String toggleState = sharedPref.getString("stickState", null);
                if ((toggleState != null) && (toggleState.equals("OFF"))) {
                    Log.i(LOG_TAG, "Stopping service, toggleState changed.");
                    runService = false;
                }

                //Read location and network availability
                /*
                if( !lm.isProviderEnabled(LocationManager.GPS_PROVIDER) || !(activeNetwork != null && activeNetwork.isConnectedOrConnecting())) {
                    Log.i(LOG_TAG, "Stopping service, GPS or network disabled");
                    runService = false;
                }
                */

            }
            //After loop: Unregister listeners, recording complete
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
            if(activityGoogleApiClient != null)
                activityGoogleApiClient.disconnect();
            Log.i(LOG_TAG, "Disconnecting Google API, stopping sensor tracker service.");
            //Release wakelock
            wakeLock.release();
        /*
        } else {
            Log.i(LOG_TAG, "GPS or Sensors unavailable.");
            //Broadcast to activity that the service stopped due to state issue
            Intent localIntent = new Intent("STICK_MOBILE_BROADCAST")
                    .putExtra("SERVICE_STATUS", "STOP");
            // Broadcasts the Intent to receivers in this app.
            LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
        }
        */
        Log.i(LOG_TAG, "Tracker service stopped.");
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(LOG_TAG, "Google API connected");
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(frequency);
        mLocationRequest.setFastestInterval(frequency);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,
                mLocationRequest, this);
        runService = true;
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // The connection has been interrupted.
        // Disable any UI components that depend on Google APIs
        // until onConnected() is called.
        Log.i(LOG_TAG, "Google API connection suspended");
        runService = false;
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // This callback is important for handling errors that
        // may occur while attempting to connect with Google.
        //
        // More about this in the next section.
        Log.i(LOG_TAG, "Google API connection failed");
        runService = false;
    }

    @Override
    public void onLocationChanged(Location location) {
        double lat = location.getLatitude();
        double lon = location.getLongitude();
        //long ts = location.getTime();
        double speed = location.getSpeed();
        double accuracy = location.getAccuracy();
        long ts = (new Date()).getTime();
        if(!FIXED_FREQ_WRITE) {
            try {
                JSONObject locationJson = new JSONObject();
                locationJson.put("latitude", lat);
                locationJson.put("longitude", lon);
                locationJson.put("timestamp", ts);
                locationJson.put("speed", getSpeedBasedonActivity(speed));
                locationJson.put("accuracy", accuracy);
                String data = locationJson.toString();
                String urlString = API_HOST + deviceID + "?data=" + data;
                Log.i(LOG_TAG, "URL generated: " + urlString);
                sendToServer(urlString);
                lastLocation = null;
            } catch(Exception e) {
                Log.e(LOG_TAG, "Error forming location JSON and posting to Server");
            }
        } else {
            lastLocation = location;
            lastLocationTime = new Date().getTime();
        }
    }

    private void setActivityRecoginitionService() {
        final Context context = this;
        activityGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(ActivityRecognition.API)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle bundle) {
                        Intent i = new Intent(context, ActivityRecognitionIntentService.class);
                        PendingIntent mActivityRecongPendingIntent = PendingIntent.getService(context, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
                        ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(activityGoogleApiClient, 0, mActivityRecongPendingIntent);
                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult connectionResult) {
                    }
                })
                .build();

        activityGoogleApiClient.connect();

        //Broadcast receiver
        receiver  = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //Add current time
                currentActivity = intent.getStringExtra("activity");
                confidence = intent.getExtras().getInt("confidence");
            }
        };

        //Filter the Intent and register broadcast receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction("ImActive");
        registerReceiver(receiver, filter);
    }

    private double getSpeedBasedonActivity(double locspeed) {
        if(locspeed <= 0) {
            if(currentActivity.toLowerCase().contains("still") == false &&
               currentActivity.toLowerCase().contains("tilting") == false) {
                return 1;
            }
        }
        return locspeed;
    }

    private int sendToServer(String urlString){
        int responseCode = 0;
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000 /* milliseconds */);
            conn.setConnectTimeout(15000 /* milliseconds */);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            // Starts the query
            conn.connect();
            responseCode = conn.getResponseCode();
            Log.d(LOG_TAG, "The response is: " + responseCode);
        } catch(Exception e) {
            Log.e(LOG_TAG, "Error while sending data to server: " + e);
        }
        return responseCode;
    }

}
