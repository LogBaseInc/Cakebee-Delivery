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
import android.widget.LinearLayout;
import android.widget.TextView;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.text.SimpleDateFormat;
import android.graphics.PorterDuff;
import android.content.DialogInterface;
import android.app.AlertDialog;
import android.widget.Toast;
import com.firebase.client.Firebase;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationServices;
import android.net.Uri;
import android.location.Geocoder;
import android.location.Address;
import java.util.Locale;
import java.util.List;

public class OrderDetailActivity extends Activity implements ConnectionCallbacks, OnConnectionFailedListener {
    public static Activity myActivity;

    OrderDetails orderdetail;
    Context context;
    GoogleApiClient mGoogleApiClient;
    LBProcessDialog mDialog = null;

    private String deviceID;
    private String accountID;
    private String currentDate;
    SharedPreferences sharedPref;
    boolean isStartedEnabled = false;
    boolean isDeliverEnabled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.order_detail);
        context = this;
        OrderDetailActivity.myActivity = this;
        mDialog = new LBProcessDialog(this);

        sharedPref = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
        deviceID = sharedPref.getString("deviceID", null);
        accountID = sharedPref.getString("accountID", null);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        currentDate = sdf.format(new java.util.Date());

        isStartedEnabled = sharedPref.getBoolean("startEnabled", false);
        isDeliverEnabled = sharedPref.getBoolean("deliverEnabled", true);

        Firebase.setAndroidContext(this);
        setGoogleApiClient();
        initialize();
    }

    @Override
    public void onResume() {
        super.onResume();
        ((MyApp) context.getApplicationContext()).setCurrentActivity(this);
        isStartedEnabled = sharedPref.getBoolean("startEnabled", false);
        isDeliverEnabled = sharedPref.getBoolean("deliverEnabled", true);
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
        builder.setMessage(orderdetail.Amount > 0 ? ("Confirm the delivery and the amount Rs." + orderdetail.Amount) : "Confirm the delivery")
                .setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener).show();
    }


    public void acceptOrder(View view) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        String currentdateandtime = sdf.format(new java.util.Date());

        Firebase myFirebaseRef = new Firebase(getString(R.string.friebaseurl) + "accounts/" + accountID + "/orders/" + deviceID + "/" + currentDate + "/" + orderdetail.Id + "/Acceptedon");
        myFirebaseRef.setValue(currentdateandtime);

        goback(null);
        sendActivity("ACCEPTED");
    }

    public void viewrouteclicked(View view) {
        Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + orderdetail.Address);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        startActivity(mapIntent);
    }

    public void changeDeviceId(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("ChangeDevice", "true");
        startActivity(intent);
    }

    public void cancelclicked(View view) {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        // Yes button clicked
                        Firebase myFirebaseRef = new Firebase(getString(R.string.friebaseurl) + "accounts/" + accountID + "/orders/" + deviceID + "/" + currentDate + "/" + orderdetail.Id + "/Startedon");
                        myFirebaseRef.setValue(null);

                        myFirebaseRef = new Firebase(getString(R.string.friebaseurl) + "trackurl/" + currentDate + "/" + accountID + "_" + orderdetail.Id + "/status");
                        myFirebaseRef.setValue("Prepared");

                        goback(null);
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        // No button clicked
                        // do nothing
                        break;
                }
            }
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Confrim the Cancel")
                .setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener).show();

    }

    public void goback(View view) {
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

    private void pickupConfirmed() {
        mDialog.StartProcessDialog();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        String currentdateandtime = sdf.format(new java.util.Date());
        Firebase myFirebaseRef = new Firebase(getString(R.string.friebaseurl) + "accounts/" + accountID + "/orders/" + deviceID + "/" + currentDate + "/" + orderdetail.Id + "/Pickedon");
        myFirebaseRef.setValue(currentdateandtime);

        sendActivity("PICKEDUP");

        getlocation(deviceID, currentDate, "Pickedat");

        startTracking(deviceID);
        goback(null);
    }

    private void DeliverdConfirmed() {
        mDialog.StartProcessDialog();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        String currentdateandtime = sdf.format(new java.util.Date());

        Firebase myFirebaseRef = new Firebase(getString(R.string.friebaseurl) + "accounts/" + accountID + "/orders/" + deviceID + "/" + currentDate + "/" + orderdetail.Id + "/Deliveredon");
        myFirebaseRef.setValue(currentdateandtime);

        sendActivity("DELIVERED");

        getlocation(deviceID, currentDate, "Deliveredat");
        goback(null);
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
        if(Utility.checkLocationPermission()) {
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
                            if (i == 1)
                                strReturnedAddress.append(", ");
                            if (i <= 1)
                                strReturnedAddress.append(returnedAddress.getAddressLine(i));
                            else
                                break;
                        }
                        String address = strReturnedAddress.toString();
                        if (attext == "Pickedat") {
                            Firebase myFirebaseRef = new Firebase(getString(R.string.friebaseurl) + "accounts/" + accountID + "/orders/" + deviceID + "/" + currentDate + "/" + orderdetail.Id + "/Startlocation");
                            myFirebaseRef.setValue(address);
                        } else if (attext == "Deliveredat") {
                            Firebase myFirebaseRef = new Firebase(getString(R.string.friebaseurl) + "accounts/" + accountID + "/orders/" + deviceID + "/" + currentDate + "/" + orderdetail.Id + "/Endlocation");
                            myFirebaseRef.setValue(address);

                            if (orderdetail.Pickedat != null && orderdetail.Pickedat != "") {
                                float[] results = new float[5];
                                String[] pickedarray = orderdetail.Pickedat.split(" ");
                                Location.distanceBetween(Double.parseDouble(pickedarray[0]), Double.parseDouble(pickedarray[1]), mLastLocation.getLatitude(), mLastLocation.getLongitude(), results);
                                if (results != null && results.length > 0) {
                                    float distance = (results[0] / 1000);

                                    Firebase distref = new Firebase(getString(R.string.friebaseurl) + "accounts/" + accountID + "/orders/" + deviceID + "/" + currentDate + "/" + orderdetail.Id + "/Distance");
                                    distref.setValue(distance);
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    System.out.print("Error:" + e.getLocalizedMessage());
                    e.printStackTrace();
                }

                Firebase myFirebaseRef1 = new Firebase(getString(R.string.friebaseurl) + "accounts/" + accountID + "/orders/" + deviceID + "/" + currentDate + "/" + orderdetail.Id + "/" + attext);
                myFirebaseRef1.setValue(mLastLocation.getLatitude() + " " + mLastLocation.getLongitude());
            } else if (attext.contains("route")) {
                showToast("Current location not identified");
            }
        }
        mDialog.StopProcessDialog();
    }

    private void showToast(String message) {
        mDialog.StopProcessDialog();
        Toast toast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
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

        Button acceptbtn = (Button) findViewById(R.id.acceptbtn);
        acceptbtn.getBackground().setColorFilter(0xFF00b5ad, PorterDuff.Mode.MULTIPLY);

        Button pickupbtn = (Button) findViewById(R.id.pickupbtn);
        pickupbtn.getBackground().setColorFilter(0xFF00b5ad, PorterDuff.Mode.MULTIPLY);

        Button deliveredbtn = (Button) findViewById(R.id.deliveredbtn);
        deliveredbtn.getBackground().setColorFilter(0xFF00b5ad, PorterDuff.Mode.MULTIPLY);

        String lastupdate = sharedPref.getString("lastupdate", null);
        TextView textView = (TextView) findViewById(R.id.clock);
        textView.setText(lastupdate);

        setOrderDetails();
    }

    private void setOrderDetails() {
        TextView title = (TextView) findViewById(R.id.title);
        TextView timelabel = (TextView) findViewById(R.id.timelabel);
        TextView namelabel = (TextView) findViewById(R.id.namelabel);
        TextView addresslabel = (TextView) findViewById(R.id.addresslabel);
        TextView phonelabel = (TextView) findViewById(R.id.phonelabel);
        TextView amountlabel = (TextView) findViewById(R.id.amountlabel);
        TextView itemdetailslabel = (TextView) findViewById(R.id.itemdetailslabel);

        title.setText("Order #" + orderdetail.Id);
        timelabel.setText(orderdetail.Time);
        namelabel.setText(orderdetail.Name);
        addresslabel.setText(orderdetail.Address);
        phonelabel.setText(orderdetail.Mobile);
        amountlabel.setText("Rs." + orderdetail.Amount);

        boolean isItemPresent = false;
        if (orderdetail.Items != null) {
            String itemdetails = "";
            for (int i = 0; i < orderdetail.Items.size(); i++) {
                ItemDetails item = orderdetail.Items.get(i);
                if (item != null) {
                    if (item.Name != "") {
                        itemdetails = itemdetails + item.Name;
                    }
                    if (item.Description != "") {
                        if (itemdetails != "")
                            itemdetails = itemdetails + " - ";

                        itemdetails = itemdetails + item.Description;
                    }

                    itemdetails = itemdetails + "\n";
                }
            }
            isItemPresent = isItemPresent || itemdetails != "";
            itemdetailslabel.setText(itemdetails);
        }

        LinearLayout itemslayout = (LinearLayout) findViewById(R.id.itemslayout);
        if (isItemPresent) {
            itemslayout.setVisibility(View.VISIBLE);
        } else {
            itemslayout.setVisibility(View.GONE);
        }

        TextView notesdetaillabel = (TextView) findViewById(R.id.notesdetaillabel);
        LinearLayout noteslayout = (LinearLayout) findViewById(R.id.noteslayout);

        if (orderdetail.Notes != null && orderdetail.Notes != "") {
            notesdetaillabel.setText(orderdetail.Notes);
            noteslayout.setVisibility(View.VISIBLE);
        } else {
            noteslayout.setVisibility(View.GONE);
        }

        Button acceptbtn = (Button) findViewById(R.id.acceptbtn);
        Button pickupbtn = (Button) findViewById(R.id.pickupbtn);
        Button deliveredbtn = (Button) findViewById(R.id.deliveredbtn);
        Button viewroutebtn = (Button) findViewById(R.id.viewroutegtn);
        Button cancelbtn = (Button) findViewById(R.id.cancelbtn);

        acceptbtn.setVisibility(View.GONE);
        pickupbtn.setVisibility(View.GONE);
        deliveredbtn.setVisibility(View.GONE);
        cancelbtn.setVisibility(View.GONE);
        viewroutebtn.setVisibility(View.VISIBLE);

        if(orderdetail.Status != null) {
            if (orderdetail.Status.equals("Yet to accept")) {
                acceptbtn.setVisibility(View.VISIBLE);
            } else if (orderdetail.Status.equals("Yet to pick")) {
                pickupbtn.setVisibility(View.VISIBLE);
            } else if (orderdetail.Status.equals("Picked up") ) {
                if (isDeliverEnabled == true && isStartedEnabled == false) {
                    deliveredbtn.setVisibility(View.VISIBLE);
                } else if (isDeliverEnabled == true && orderdetail.Startedon != null && orderdetail.Startedon != "") {
                    deliveredbtn.setVisibility(View.VISIBLE);
                    cancelbtn.setVisibility(View.VISIBLE);
                }
            }
            else if(orderdetail.Status.equals("Yet to deliver") && isDeliverEnabled == true) {
                deliveredbtn.setVisibility(View.VISIBLE);
            }
        }
    }

    private void startTracking(String deviceID) {
        Button pickupbtn = (Button) findViewById(R.id.pickupbtn);
        pickupbtn.setVisibility(View.GONE);

        Button deliveredbtn = (Button) findViewById(R.id.deliveredbtn);
        deliveredbtn.setVisibility(View.VISIBLE);
    }

    private void sendActivity(String activity) {
        Utility.sendActivity(accountID, deviceID, orderdetail.Id, activity, sharedPref);
    }
}
