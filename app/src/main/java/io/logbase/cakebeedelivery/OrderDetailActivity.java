package io.logbase.cakebeedelivery;

/**
 * Created by logbase on 23/11/15.
 */

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.text.SimpleDateFormat;
import android.graphics.PorterDuff;
import android.widget.Toast;
import android.content.DialogInterface;
import android.app.AlertDialog;
import java.util.Calendar;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationServices;
import io.logbase.stickandroidsdk.StickMobile;

public class OrderDetailActivity extends Activity implements ConnectionCallbacks, OnConnectionFailedListener {
    public static Activity myActivity;

    OrderDetails orderdetail;
    Context context;
    GoogleApiClient mGoogleApiClient;

    //private StickMobile stick;
    //private Integer frequency = 10;
    private boolean ispickedup = false;
    private String deviceID;
    private String accountID;
    private String currentDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.order_detail);
        context = this;
        OrderDetailActivity.myActivity = this;

        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
        deviceID = sharedPref.getString("deviceID", null);
        accountID = sharedPref.getString("accountID", null);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        currentDate = sdf.format(new java.util.Date());

        Firebase.setAndroidContext(this);
        setGoogleApiClient();
        initialize();
        //TrackingSettings();
        checkIsPickedup();
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

    public void pickupclicked(View view) {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        // Yes button clicked
                        pickupConfirmed();
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        // No button clicked
                        // do nothing
                        break;
                }
            }
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Confirm the pickup")
                .setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener).show();
    }

    public void pickupConfirmed() {
        ispickedup = true;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        String currentdateandtime = sdf.format(new java.util.Date());

        Firebase myFirebaseRef = new Firebase(getString(R.string.friebaseurl)+"accounts/"+accountID+"/orders/"+deviceID+"/"+currentDate+"/"+orderdetail.Id+"/Pickedon");
        myFirebaseRef.setValue(currentdateandtime);

        getlocation(deviceID, currentDate, "Pickedat");

        startTracking(deviceID);
    }

    public void delivered(View view) {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        // Yes button clicked
                        DeliverdConfirmed();
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        // No button clicked
                        // do nothing
                        break;
                }
            }
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(orderdetail.Amount >0 ? ("Confirm the delivery and the amount Rs."+ orderdetail.Amount)  : "Confirm the delivery")
                .setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener).show();

    }

    public void DeliverdConfirmed() {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        String currentdateandtime = sdf.format(new java.util.Date());

        Firebase myFirebaseRef = new Firebase(getString(R.string.friebaseurl)+"accounts/"+accountID+"/orders/"+deviceID+"/"+currentDate+"/"+orderdetail.Id+"/Deliveredon");
        myFirebaseRef.setValue(currentdateandtime);

        getlocation(deviceID, currentDate, "Deliveredat");

        stopTracking();

        ispickedup = false;
        cancelclicked(null);
    }

    public void cancelclicked(View view) {
        Intent intent = new Intent(this, OrdersActivity.class);
        startActivity(intent);
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

    private void getlocation(String deviceID, String currentDate, String attext) {
        Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        if (mLastLocation != null) {
            //ShowToast("getlocation: " + mLastLocation);

            Firebase myFirebaseRef = new Firebase(getString(R.string.friebaseurl)+"accounts/"+accountID+"/orders/"+deviceID+"/"+currentDate+"/"+orderdetail.Id+"/"+attext);
            myFirebaseRef.setValue(mLastLocation.getLatitude() +" " +mLastLocation.getLongitude());

        }
        else {
            //ShowToast("getlocation null");
        }
    }

    private void ShowToast(String message) {
        Toast toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
        toast.show();
    }

    private void initialize() {
        Bundle b = getIntent().getExtras();
        String orderjson = b.getString("Order");

        ObjectMapper mapper = new ObjectMapper();
        JsonNode actualObj = null;
        try {
            actualObj = mapper.readTree(orderjson);
        } catch (IOException e) {
            e.printStackTrace();
        }

        orderdetail = mapper.convertValue(actualObj, OrderDetails.class);

        TextView idlabel = (TextView)findViewById(R.id.idlabel);
        TextView timelabel = (TextView)findViewById(R.id.timelabel);
        TextView namelabel = (TextView)findViewById(R.id.namelabel);
        TextView addresslabel = (TextView)findViewById(R.id.addresslabel);
        TextView phonelabel = (TextView)findViewById(R.id.phonelabel);
        TextView amountlabel = (TextView)findViewById(R.id.amountlabel);
        TextView itemdetailslabel = (TextView)findViewById(R.id.itemdetailslabel);

        idlabel.setText(orderdetail.Id);
        timelabel.setText(orderdetail.Time);
        namelabel.setText(orderdetail.Name);
        addresslabel.setText(orderdetail.Address);
        phonelabel.setText(orderdetail.Mobile);
        amountlabel.setText("Rs." + orderdetail.Amount);

        if(orderdetail.Items != null) {
            String itemdetails = "";
            for (int i = 0; i < orderdetail.Items.size(); i++) {
                ItemDetails item = orderdetail.Items.get(i);
                if (item != null) {
                    itemdetails = itemdetails + item.Name + " - " + item.Description + "\n\n";
                }
            }
            itemdetailslabel.setText(itemdetails);
        }

        Button pickupbtn = (Button)findViewById(R.id.pickupbtn);
        pickupbtn.getBackground().setColorFilter(0xFF00b5ad, PorterDuff.Mode.MULTIPLY);

        Button deliveredbtn = (Button)findViewById(R.id.deliveredbtn);
        deliveredbtn.setVisibility(View.GONE);
        deliveredbtn.getBackground().setColorFilter(0xFF00b5ad, PorterDuff.Mode.MULTIPLY);

        RelativeLayout lastupdatelayout = (RelativeLayout)findViewById(R.id.lastupdatelayout);
        lastupdatelayout.setVisibility(View.GONE);

    }

    @Override
    public void onBackPressed() {
        if(ispickedup == false)
            super.onBackPressed();
        //dont call **super**, if u want disable back button in current screen.
    }

    /*private void TrackingSettings() {
        IntentFilter mStatusIntentFilter = new IntentFilter("STICK_MOBILE_BROADCAST");
        // Instantiates a new DownloadStateReceiver
        StatusReceiver mStatusReceiver = new StatusReceiver();
        // Registers the DownloadStateReceiver and its intent filters
        LocalBroadcastManager.getInstance(this).registerReceiver(
                mStatusReceiver,
                mStatusIntentFilter);
    }*/

    private void startTracking(String deviceID) {
        Button pickupbtn = (Button)findViewById(R.id.pickupbtn);
        pickupbtn.setVisibility(View.GONE);
        Button cancelbtn = (Button)findViewById(R.id.cancelbtn);
        cancelbtn.setVisibility(View.GONE);

        Button deliveredbtn = (Button)findViewById(R.id.deliveredbtn);
        deliveredbtn.setVisibility(View.VISIBLE);
        RelativeLayout lastupdatelayout = (RelativeLayout)findViewById(R.id.lastupdatelayout);
        lastupdatelayout.setVisibility(View.VISIBLE);

        ((MyApp)this.getApplication()).startOrderTracking();

        /*if(stick == null) {
            stick = new StickMobile(this, deviceID, frequency, null);
        }

        //Using SDK starts
        boolean stickStarted = false;
        if (stick != null && stick.isRunning() == false) {
            stickStarted = stick.start();
        }

        if (!stickStarted)
            ShowToast("Unable to start if blank device ID, no Network or GPS");*/
    }

    private void stopTracking() {
        //Stop order tracking and start default tracking
        ((MyApp) this.getApplication()).startDefaultTracking();

        /*//Using SDK ends
        if (stick != null)
            stick.stop();*/
    }

    private void checkIsPickedup(){
        Firebase myFirebaseRef = new Firebase(getString(R.string.friebaseurl)+"accounts/"+accountID+"/orders/"+deviceID+"/"+currentDate+"/"+orderdetail.Id);
        myFirebaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                OrderDetails order = snapshot.getValue(OrderDetails.class);
                if(order.Pickedat != null && order.Pickedat != "") {
                    startTracking(deviceID);
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                System.out.println("The read failed: " + firebaseError.getMessage());
            }
        });
    }

    /*public class StatusReceiver extends BroadcastReceiver {
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
                TextView clock = (TextView) findViewById(R.id.clock);
                Calendar cal = Calendar.getInstance();
                String date = android.text.format.DateFormat.format("MMM dd, yyyy hh:mm:ss a", cal.getTime()).toString();
                clock.setText(date);
            }
        }
    }*/

}