package io.logbase.cakebeedelivery;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.location.Location;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationServices;

import java.text.SimpleDateFormat;
import java.util.HashSet;

/**
 * Created by logbase on 30/11/15.
 */

public class LoginActivity extends Activity implements ConnectionCallbacks, OnConnectionFailedListener {

    Context context;
    String deviceID;
    String accountID;
    String currentDate;
    GoogleApiClient mGoogleApiClient;
    LBProcessDialog mDialog = null;
    MyApp myapp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);
        context = this;
        mDialog = new LBProcessDialog(this);
        myapp = ((MyApp)context.getApplicationContext());
    }

    @Override
    public void onResume() {
        super.onResume();
        myapp.setCurrentActivity(this);

        Firebase.setAndroidContext(this);

        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);

        deviceID = sharedPref.getString("deviceID", null);
        accountID = sharedPref.getString("accountID", null);

        String accountname = sharedPref.getString("accountname", null);
        TextView title = (TextView)findViewById(R.id.title);
        title.setText((accountname.substring(0, 1).toUpperCase() + accountname.substring(1)) + " Deliveries");

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        currentDate = sdf.format(new java.util.Date());

        Button loginButton = (Button)findViewById(R.id.loginButton);
        loginButton.getBackground().setColorFilter(0xFF00b5ad, PorterDuff.Mode.MULTIPLY);
        loginButton.setVisibility(View.GONE);

        isLoggedIn();
    }

        @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        return super.onOptionsItemSelected(item);
    }

    public void login(View view) {
        mDialog.StartProcessDialog();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        String currentdateandtime = sdf.format(new java.util.Date());

        myapp.AddLoginLog(currentdateandtime);

        Firebase myFirebaseRef = new Firebase(getString(R.string.friebaseurl)+"accounts/"+accountID+"/orders/"+deviceID+"/"+currentDate+"/LoggedOn");
        myFirebaseRef.setValue(currentdateandtime);

        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("loggedinDate", currentDate);
        editor.putStringSet("OrderIds", null);
        editor.commit();

        startDefaultTracking();

        getlocation();
    }

    public void stopTracking() {
        //((MyApp)context.getApplicationContext()).stopTracking();
    }

    public void startDefaultTracking() {
        myapp.startDefaultTracking();
    }

    private void GoToOrders() {
        mDialog.StopProcessDialog();

        Intent intent = new Intent(this, OrdersActivity.class);
        startActivity(intent);
    }

    private void isLoggedIn() {
        mDialog.StartProcessDialog();

        Firebase myFirebaseRef =  new Firebase(getString(R.string.friebaseurl)+"accounts/"+accountID+"/orders/"+deviceID+"/"+currentDate+"/LoggedOn");
        myFirebaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                mDialog.StopProcessDialog();

                Object obj = snapshot.getValue();
                if (obj != null && obj != "") {
                    SharedPreferences sharedPref = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString("loggedinDate", currentDate);
                    editor.commit();

                    startDefaultTracking();

                    GoToOrders();
                } else {
                    Button loginButton = (Button) findViewById(R.id.loginButton);
                    loginButton.setVisibility(View.VISIBLE);
                    setGoogleApiClient();

                    stopTracking();
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                System.out.println("The read failed: " + firebaseError.getMessage());
            }
        });
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


    private void setGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    private void getlocation() {
        Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        if (mLastLocation != null) {
            Firebase myFirebaseRef =  new Firebase(getString(R.string.friebaseurl)+"accounts/"+accountID+"/orders/"+deviceID+"/"+currentDate+"/Loggedat");
            myFirebaseRef.setValue(mLastLocation.getLatitude() +" " +mLastLocation.getLongitude());
        }

        GoToOrders();
    }
}
