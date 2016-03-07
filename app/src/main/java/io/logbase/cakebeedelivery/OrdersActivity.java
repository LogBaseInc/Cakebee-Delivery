package io.logbase.cakebeedelivery;

/**
 * Created by logbase on 20/11/15.
 */

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ListView;
import android.view.View;
import android.widget.Toast;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.firebase.client.Firebase;
import com.firebase.client.ValueEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.FirebaseError;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectWriter;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.UUID;
import android.widget.CompoundButton;
import android.content.pm.PackageInfo;

public class OrdersActivity extends ListActivity {
    List<OrderDetails> orderDetaillist;
    Context context;
    String currentDate;
    LBProcessDialog mDialog = null;
    boolean doubleBackToExitPressedOnce = false;
    String deviceID;
    String accountID;
    String accountname;
    Firebase weburlFirebaseRef = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.orders);
        context = this;

        mDialog = new LBProcessDialog(this);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        doubleBackToExitPressedOnce = false;
        ((MyApp) context.getApplicationContext()).setCurrentActivity(this);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        currentDate = sdf.format(new java.util.Date());

        TextView nolist = (TextView) findViewById(R.id.nolist);
        nolist.setVisibility(View.GONE);

        Firebase.setAndroidContext(this);

        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
        deviceID = sharedPref.getString("deviceID", null);
        accountID = sharedPref.getString("accountID", null);
        accountname = sharedPref.getString("accountname", null);

        getWebhookUrl();
        initializeSwtich();
        setVersionNumber();
    }

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            return;
        }
        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Tap again to exit", Toast.LENGTH_SHORT).show();
        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                doubleBackToExitPressedOnce = false;
            }
        }, 2000);
    }

    public void changeDeviceId(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("ChangeDevice", "true");
        startActivity(intent);
    }

    public void orderClicked(View view) {
        View parentRow = (View) view.getParent();
        ListView listView = (ListView) parentRow.getParent();
        int position = listView.getPositionForView(parentRow);
        OrderDetails orderDetail = orderDetaillist.get(position);
        showOrderDetails(orderDetail);
    }

    public void addtask(View view) {
        Intent intent = new Intent(this, TaskActivity.class);
        startActivity(intent);
    }

    private  void initializeSwtich(){
        Firebase loggedinref = new Firebase(getString(R.string.friebaseurl)+"accounts/"+accountID+"/orders/"+deviceID+"/"+currentDate+"/Loggedin");
        loggedinref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                mDialog.StopProcessDialog();
                Switch toggle = (Switch) findViewById(R.id.logintoggle);
                if (snapshot.getValue() != null && Boolean.parseBoolean(snapshot.getValue().toString()) == true) {
                    toggle.setChecked(true);
                    getOrders();
                } else {
                    toggle.setChecked(false);
                    NoOrders();
                }
                setCheckChangedListener();
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                System.out.println("The read failed: " + firebaseError.getMessage());
            }
        });
    }

    private void setCheckChangedListener() {
        Switch toggle = (Switch)findViewById(R.id.logintoggle);
        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                String currentdateandtime = sdf.format(new java.util.Date());
                String uniqueID = UUID.randomUUID().toString();
                Firebase myFirebaseRef = new Firebase(getString(R.string.friebaseurl) + "accounts/" + accountID + "/orders/" + deviceID + "/" + currentDate + "/Activity/" + uniqueID);
                myFirebaseRef.child("Date").setValue(currentdateandtime);
                myFirebaseRef.child("Login").setValue(isChecked);

                myFirebaseRef = new Firebase(getString(R.string.friebaseurl) + "accounts/" + accountID + "/orders/" + deviceID + "/" + currentDate + "/Loggedin");
                myFirebaseRef.setValue(isChecked);

                if (isChecked == true) {
                    getOrders();
                } else {
                    NoOrders();
                }
                Firebase accountdeviceref = new Firebase(getString(R.string.friebaseurl) + "accounts/" + accountID + "/devices/" + deviceID + "/activity");
                accountdeviceref.child("date").setValue(currentdateandtime);
                accountdeviceref.child("login").setValue(isChecked);
            }
        });
    }

    private void getOrders () {
        mDialog.StartProcessDialog();

        TextView title = (TextView)findViewById(R.id.title);
        title.setText((accountname.substring(0, 1).toUpperCase() + accountname.substring(1)) + " Deliveries");

        Firebase myFirebaseRef = new Firebase(getString(R.string.friebaseurl)+"accounts/"+accountID+"/orders/"+deviceID+"/"+currentDate);
        myFirebaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                mDialog.StopProcessDialog();
                ((MyApp) context.getApplicationContext()).Clear(true);
                ((MyApp) context.getApplicationContext()).Clear(false);

                orderDetaillist = new ArrayList<OrderDetails>();
                Switch toggle = (Switch) findViewById(R.id.logintoggle);
                boolean isChecked = toggle.isChecked();
                if (isChecked == true) {
                    Object orders = snapshot.getValue();
                    if (orders != null) {
                        ListView listview = (ListView) findViewById(android.R.id.list);
                        listview.setVisibility(View.VISIBLE);
                        TextView nolist = (TextView) findViewById(R.id.nolist);
                        nolist.setVisibility(View.GONE);
                        int pickedupordercount = 0;

                        String ordersString = orders.toString();

                        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
                        try {
                            ordersString = ow.writeValueAsString(orders);
                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                        }

                        ObjectMapper mapper = new ObjectMapper();
                        try {

                            JsonNode node = mapper.readTree(ordersString);
                            if (node != null) {
                                Iterator<Entry<String, JsonNode>> nodeIterator = node.fields();
                                while (nodeIterator.hasNext()) {
                                    Entry<String, JsonNode> entry = (Entry<String, JsonNode>) nodeIterator.next();
                                    if (entry.getKey() != "LoggedOn" && entry.getKey() != "Loggedat" && entry.getKey() != "Activity" && entry.getKey() != "Loggedin") {
                                        OrderDetails orderdet = mapper.convertValue(entry.getValue(), OrderDetails.class);
                                        if (IsOrderValid(orderdet)) {
                                            String[] timesplit = new String[0];
                                            boolean ispickupam = true;
                                            boolean isdeliveryam = true;

                                            if (!(orderdet.Time.contains("Mid"))) {
                                                orderdet.Time = orderdet.Time.toLowerCase();
                                                if (orderdet.Time.contains(":")) {
                                                    orderdet.Time = orderdet.Time.replaceAll(":", ".");
                                                }

                                                timesplit = orderdet.Time.split("-");
                                                if (timesplit.length >= 2) {
                                                    timesplit[0] = timesplit[0].replaceAll(" ", "");
                                                    timesplit[1] = timesplit[1].replaceAll(" ", "");

                                                    Boolean ispm = false;
                                                    isdeliveryam = !(timesplit[1].contains("pm"));
                                                    if (timesplit[0].contains("am")) {
                                                        ispm = false;
                                                        ispickupam = true;
                                                        timesplit[1] = timesplit[1].replaceAll("am", "");
                                                        timesplit[1] = timesplit[1].replaceAll("pm", "");
                                                        if (Double.parseDouble(timesplit[1]) == 12)
                                                            isdeliveryam = false;
                                                    } else if (timesplit[0].contains("pm")) {
                                                        ispm = true;
                                                        ispickupam = false;

                                                        timesplit[0] = timesplit[0].replaceAll("am", "");
                                                        timesplit[0] = timesplit[0].replaceAll("pm", "");
                                                        timesplit[1] = timesplit[1].replaceAll("am", "");
                                                        timesplit[1] = timesplit[1].replaceAll("pm", "");

                                                        if (Double.parseDouble(timesplit[1]) == 12)
                                                            isdeliveryam = false;
                                                        if (Double.parseDouble(timesplit[0]) == 12)
                                                            ispm = false;
                                                    } else {
                                                        if (timesplit[1].indexOf("pm") >= 0 && Double.parseDouble(timesplit[0]) >= 1 && Double.parseDouble(timesplit[0]) < 12) {
                                                            timesplit[1] = timesplit[1].replaceAll("am", "");
                                                            timesplit[1] = timesplit[1].replaceAll("pm", "");
                                                            if (Double.parseDouble(timesplit[1]) != 12) {
                                                                ispm = true;
                                                                ispickupam = false;
                                                            } else {
                                                                isdeliveryam = false;
                                                            }
                                                        }
                                                    }

                                                    timesplit[0] = timesplit[0].replaceAll("am", "");
                                                    timesplit[0] = timesplit[0].replaceAll("pm", "");

                                                    orderdet.TimeSort = (Double.isNaN(Double.parseDouble(timesplit[0])) ? 24 : (ispm ? (Double.parseDouble(timesplit[0]) + 12) : Double.parseDouble(timesplit[0])));
                                                } else
                                                    orderdet.TimeSort = 0.0;
                                            } else {
                                                orderdet.TimeSort = 24.0;
                                            }

                                            orderdet.Name = upperCaseFirst(orderdet.Name);
                                            orderdet.Id = entry.getKey();

                                            if (orderdet.Cancelledon != null && orderdet.Cancelledon != "") {
                                                orderdet.Status = "Cancelled";
                                                ((MyApp) context.getApplicationContext()).removeOrders(orderdet.Id, false);
                                                ((MyApp) context.getApplicationContext()).removeOrders(orderdet.Id, true);
                                            } else if (orderdet.Deliveredon != null && orderdet.Deliveredon != "") {
                                                orderdet.Status = "Delivered";
                                                ((MyApp) context.getApplicationContext()).removeOrders(orderdet.Id, false);
                                                ((MyApp) context.getApplicationContext()).removeOrders(orderdet.Id, true);
                                            } else if (orderdet.Pickedon != null && orderdet.Pickedon != "") {
                                                orderdet.Status = "Picked up";
                                                pickedupordercount = pickedupordercount + 1;

                                                ((MyApp) context.getApplicationContext()).removeOrders(orderdet.Id, true);
                                                if (timesplit.length >= 1)
                                                    ((MyApp) context.getApplicationContext()).addOrders(orderdet.Id, timesplit[1], isdeliveryam, false);
                                            } else {
                                                if (orderdet.Acceptedon != null && orderdet.Acceptedon != "")
                                                    orderdet.Status = "Yet to pick";
                                                else
                                                    orderdet.Status = "Yet to accept";

                                                if (timesplit.length >= 1)
                                                    ((MyApp) context.getApplicationContext()).addOrders(orderdet.Id, timesplit[0], ispickupam, true);

                                                if (timesplit.length >= 2)
                                                    ((MyApp) context.getApplicationContext()).addOrders(orderdet.Id, timesplit[1], isdeliveryam, false);
                                            }

                                            orderDetaillist.add(orderdet);
                                        }
                                    }
                                }

                                SharedPreferences sharedPref = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor = sharedPref.edit();

                                if (pickedupordercount > 0) {
                                    editor.putBoolean("OrderTracking", true);
                                    editor.commit();
                                    startOrderTracking();
                                } else {
                                    editor.putBoolean("OrderTracking", false);
                                    editor.commit();
                                    startDefaultTracking();
                                }

                                Collections.sort(orderDetaillist);

                                OrderlistAdapter listAdapter = new OrderlistAdapter(context, orderDetaillist);
                                setListAdapter(listAdapter);
                            }

                        } catch (IOException e) {
                            mDialog.StopProcessDialog();
                            e.printStackTrace();
                        }
                    }

                    if (orderDetaillist.size() <= 0) {
                        NoOrders();
                    }

                    mDialog.StopProcessDialog();

                    SharedPreferences sharedPref = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString("ordercount", currentDate);
                    editor.commit();
                } else {
                    NoOrders();
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                System.out.println("The read failed: " + firebaseError.getMessage());
            }
        });
    }

    private void startOrderTracking() {
        ((MyApp)this.getApplication()).startOrderTracking();
    }

    private void startDefaultTracking() {
        ((MyApp)this.getApplication()).startDefaultTracking();
    }
    private boolean IsOrderValid(OrderDetails orderdet) {
        boolean valid =false;
        if(orderdet.Name != null && orderdet.Name != "" &&
            orderdet.Amount != null &&
            orderdet.Address != null && orderdet.Address != "" &&
            orderdet.Time != null && orderdet.Time != "" &&
            orderdet.Mobile != null && orderdet.Mobile != "") {
            valid = true;
        }
        return  valid;
    }

    private void NoOrders() {
        mDialog.StopProcessDialog();
        ListView listview = (ListView)findViewById(android.R.id.list);
        listview.setVisibility(View.GONE);
        TextView nolist = (TextView) findViewById(R.id.nolist);
        nolist.setVisibility(View.VISIBLE);
    }

    private static String upperCaseFirst(String value) {
        value = value.toLowerCase();
        char[] array = value.toCharArray();
        array[0] = Character.toUpperCase(array[0]);
        return new String(array);
    }

    private void showOrderDetails(OrderDetails orderDetails) {
        String orderjson = "";
        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        try {
            orderjson = ow.writeValueAsString(orderDetails);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        Intent intent = new Intent(this, OrderDetailActivity.class);
        intent.putExtra("Order", orderjson);
        startActivity(intent);
    }

    private void getWebhookUrl() {
        if(weburlFirebaseRef == null) {
            weburlFirebaseRef = new Firebase(getString(R.string.friebaseurl) + "accounts/" + accountID + "/settings/webhook/url");
            weburlFirebaseRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    Object webhookurl = snapshot.getValue();
                    SharedPreferences sharedPref = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putBoolean("WebhookEnabled", ((webhookurl != null && webhookurl != "") ? true : false));
                    editor.putString("WebhookUrl", webhookurl != null ? webhookurl.toString() : "");
                    editor.commit();
                }

                @Override
                public void onCancelled(FirebaseError firebaseError) {
                    System.out.println("The webhookurl read failed: " + firebaseError.getMessage());
                }
            });
        }
    }

    private void setVersionNumber() {
        try {
            PackageInfo info = this.getPackageManager().getPackageInfo("io.logbase.cakebeedelivery", 0);
            Firebase myFirebaseRef = new Firebase(getString(R.string.friebaseurl)+"accounts/"+accountID+"/devices/"+deviceID+"/appversion");
            myFirebaseRef.setValue(info.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }
}