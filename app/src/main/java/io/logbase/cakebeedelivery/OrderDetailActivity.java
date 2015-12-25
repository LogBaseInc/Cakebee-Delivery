package io.logbase.cakebeedelivery;

/**
 * Created by logbase on 23/11/15.
 */

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
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
import android.content.DialogInterface;
import android.app.AlertDialog;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationServices;

public class OrderDetailActivity extends Activity implements ConnectionCallbacks, OnConnectionFailedListener {
    public static Activity myActivity;

    OrderDetails orderdetail;
    Context context;
    GoogleApiClient mGoogleApiClient;
    LBProcessDialog mDialog = null;

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
        mDialog = new LBProcessDialog(this);

        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
        deviceID = sharedPref.getString("deviceID", null);
        accountID = sharedPref.getString("accountID", null);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        currentDate = sdf.format(new java.util.Date());

        Firebase.setAndroidContext(this);
        setGoogleApiClient();
        initialize();
        checkIsPickedup();
    }

    @Override
    public void onResume() {
        super.onResume();
        ((MyApp) context.getApplicationContext()).setCurrentActivity(this);
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
        mDialog.StartProcessDialog();

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
        mDialog.StartProcessDialog();

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

    public void viewrouteclicked(View view){
        Intent intent = new Intent(this, RouteActivity.class);
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
            Firebase myFirebaseRef = new Firebase(getString(R.string.friebaseurl)+"accounts/"+accountID+"/orders/"+deviceID+"/"+currentDate+"/"+orderdetail.Id+"/"+attext);
            myFirebaseRef.setValue(mLastLocation.getLatitude() +" " +mLastLocation.getLongitude());

        }

        mDialog.StopProcessDialog();
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

        Button pickupbtn = (Button)findViewById(R.id.pickupbtn);
        pickupbtn.getBackground().setColorFilter(0xFF00b5ad, PorterDuff.Mode.MULTIPLY);

        Button deliveredbtn = (Button)findViewById(R.id.deliveredbtn);
        deliveredbtn.setVisibility(View.GONE);
        deliveredbtn.getBackground().setColorFilter(0xFF00b5ad, PorterDuff.Mode.MULTIPLY);

        getOrderDetails();

    }

    private void setOrderDetails() {
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
        Button deliveredbtn = (Button)findViewById(R.id.deliveredbtn);

        if(orderdetail.Status == "Picked up") {
            pickupbtn.setVisibility(View.GONE);
            deliveredbtn.setVisibility(View.VISIBLE);
        }
        else if(orderdetail.Status == "Delivered") {
            pickupbtn.setVisibility(View.GONE);
            deliveredbtn.setVisibility(View.GONE);
        }
        else {
            pickupbtn.setVisibility(View.VISIBLE);
            deliveredbtn.setVisibility(View.GONE);
        }
    }

    @Override
    public void onBackPressed() {
        //if(ispickedup == false)
        super.onBackPressed();
        //dont call **super**, if u want disable back button in current screen.
    }

    private void getOrderDetails() {
        Firebase myFirebaseRef =  new Firebase(getString(R.string.friebaseurl)+"accounts/"+accountID+"/orders/"+deviceID+"/"+currentDate+"/"+orderdetail.Id);
        myFirebaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                orderdetail = snapshot.getValue(OrderDetails.class);
                orderdetail.Id = snapshot.getKey();
                if(orderdetail.Deliveredon != null && orderdetail.Deliveredon != "") {
                    orderdetail.Status = "Delivered";
                }
                else if(orderdetail.Pickedon != null && orderdetail.Pickedon != "") {
                    orderdetail.Status = "Picked up";
                }
                else {
                    orderdetail.Status = "Yet to pick";
                }

                setOrderDetails();
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                System.out.println("The read failed: " + firebaseError.getMessage());
            }
        });
    }

    private void startTracking(String deviceID) {
        Button pickupbtn = (Button)findViewById(R.id.pickupbtn);
        pickupbtn.setVisibility(View.GONE);

        Button deliveredbtn = (Button)findViewById(R.id.deliveredbtn);
        deliveredbtn.setVisibility(View.VISIBLE);

        //RelativeLayout lastupdatelayout = (RelativeLayout)findViewById(R.id.lastupdatelayout);
        //lastupdatelayout.setVisibility(View.VISIBLE);

        //((MyApp)this.getApplication()).startOrderTracking();
    }

    private void stopTracking() {
        //Stop order tracking and start default tracking
        //((MyApp) this.getApplication()).startDefaultTracking();
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

}
