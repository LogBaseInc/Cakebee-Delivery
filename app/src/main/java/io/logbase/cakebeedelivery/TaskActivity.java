package io.logbase.cakebeedelivery;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.firebase.client.Firebase;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationServices;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by logbase on 06/02/16.
 */
public class TaskActivity extends Activity implements ConnectionCallbacks, OnConnectionFailedListener {
    boolean taskstopped = false;
    String starttime = "";
    String startedat = "";
    String startlocation = "";
    String endtime = "";
    String endedat = "";
    String endlocation = "";
    float distance = 0;
    MyApp myapp = null;
    GoogleApiClient mGoogleApiClient;
    String deviceID;
    String accountID;
    String currentDate;
    SharedPreferences sharedPref;
    LBProcessDialog mDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.task);
        Firebase.setAndroidContext(this);
        myapp = ((MyApp) this.getApplicationContext());
        mDialog = new LBProcessDialog(this);

        sharedPref = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
        deviceID = sharedPref.getString("deviceID", null);
        accountID = sharedPref.getString("accountID", null);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        currentDate = sdf.format(new java.util.Date());

        Button starttrackingbtn = (Button)findViewById(R.id.starttrackingbtn);
        starttrackingbtn.getBackground().setColorFilter(0xFF00b5ad, PorterDuff.Mode.MULTIPLY);

        Button stoptrackingbtn = (Button)findViewById(R.id.stoptrackingbtn);
        stoptrackingbtn.getBackground().setColorFilter(0xFF00b5ad, PorterDuff.Mode.MULTIPLY);

        setGoogleApiClient();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    public void cancel(View view) {
        onBackPressed();
    }

    public void startTask(View view) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        starttime = sdf.format(new java.util.Date());
        getlocation("start");

        Button starttrackingbtn = (Button)findViewById(R.id.starttrackingbtn);
        starttrackingbtn.setVisibility(View.GONE);
        Button cancelbtn = (Button)findViewById(R.id.cancelbtn);
        cancelbtn.setVisibility(View.GONE);

        Button stoptrackingbtn = (Button)findViewById(R.id.stoptrackingbtn);
        stoptrackingbtn.setVisibility(View.VISIBLE);

        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean("TaskRunning", true);
        editor.commit();

        myapp.startOrderTracking();
    }

    public void stopTask(View view) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        endtime = sdf.format(new java.util.Date());
        getlocation("end");
    }

    private void setGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    private void getlocation(String attext) {
        Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            Geocoder geocoder;
            List<Address> addresses;
            geocoder = new Geocoder(this, Locale.getDefault());

            try {
                addresses = geocoder.getFromLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude(), 1);
                if (addresses != null) {
                    Address returnedAddress = addresses.get(0);
                    StringBuilder strReturnedAddress = new StringBuilder("");

                    for (int i = 0; i < returnedAddress.getMaxAddressLineIndex(); i++) {
                        if(i == 1)
                            strReturnedAddress.append(", ");
                        if(i <= 1)
                            strReturnedAddress.append(returnedAddress.getAddressLine(i));
                        else
                            break;
                    }
                    if(attext == "start") {
                        startedat = mLastLocation.getLatitude() + " " + mLastLocation.getLongitude();
                        startlocation = strReturnedAddress.toString();
                    }
                    else if(attext == "end") {
                        endedat = mLastLocation.getLatitude() + " " + mLastLocation.getLongitude();
                        endlocation = strReturnedAddress.toString();

                        if(startedat != null && startedat != "") {
                            float[] results = new float[5];
                            String[] pickedarray = startedat.split(" ");
                            Location.distanceBetween(Double.parseDouble(pickedarray[0]), Double.parseDouble(pickedarray[1]), mLastLocation.getLatitude(), mLastLocation.getLongitude(), results);
                            if(results != null && results.length > 0) {
                                distance = (results[0]/1000);
                            }
                        }
                        taskstopped();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else if(attext == "end"){
            taskstopped();
        }

        mDialog.StopProcessDialog();
    }

    private void taskstopped() {
        EditText notetext = (EditText)findViewById(R.id.notetext);
        String notes = notetext.getText().toString();

        Map<String, String> task = new HashMap<String, String>();
        task.put("starttime", starttime);
        task.put("startedat", startedat);
        task.put("startlocation", startlocation);
        task.put("endtime", endtime);
        task.put("endedat", endedat);
        task.put("endlocation", endlocation);
        task.put("distance", Float.toString(distance));
        task.put("notes", (notes != null ? notes : ""));

        Firebase myFirebaseRef = new Firebase(getString(R.string.friebaseurl)+"accounts/"+accountID+"/tasks/"+deviceID+"/"+currentDate+"/"+ (UUID.randomUUID().toString()));
        myFirebaseRef.setValue(task);

        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean("TaskRunning", false);
        editor.commit();

        myapp.startDefaultTracking();

        taskstopped = true;
        onBackPressed();
    }
    @Override
    public void onConnected(Bundle connectionHint) {
        //ShowToast("onConnected: ");
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // The connection has been interrupted.
        // Disable any UI components that depend on Google APIs
        // until onConnected() is called.
        //ShowToast("onConnectionSuspended: ");
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // This callback is important for handling errors that
        // may occur while attempting to connect with Google.
        //
        // More about this in the next section.
        //ShowToast("onConnectionFailed: ");
    }

    @Override
    public void onBackPressed() {
        Button starttrackingbtn = (Button)findViewById(R.id.starttrackingbtn);
        if(starttrackingbtn.getVisibility() == View.VISIBLE || taskstopped == true)
            super.onBackPressed();
    }
}
