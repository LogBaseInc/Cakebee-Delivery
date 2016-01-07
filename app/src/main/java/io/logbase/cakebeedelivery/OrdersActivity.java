package io.logbase.cakebeedelivery;

/**
 * Created by logbase on 20/11/15.
 */

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ListView;
import android.view.View;
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
import android.app.Activity;
import java.util.Collections;

public class OrdersActivity extends ListActivity {
    List<OrderDetails> orderDetaillist;
    Context context;
    String currentDate;
    LBProcessDialog mDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.orders);
        context = this;

        mDialog = new LBProcessDialog(this);
    }

    private boolean CheckLoggedIn() {
        boolean isloggedin = true;
        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
        String loggedinDate = sharedPref.getString("loggedinDate", null);
        if(loggedinDate == null || loggedinDate.contains(currentDate) == false) {
            isloggedin = false;
        }
        return isloggedin;
    }

    private void getOrders () {
        mDialog.StartProcessDialog();

        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
        String deviceID = sharedPref.getString("deviceID", null);
        String accountID = sharedPref.getString("accountID", null);
        String accountname = sharedPref.getString("accountname", null);

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
                Object orders = snapshot.getValue();
                if (orders != null) {
                    ListView listview = (ListView) findViewById(android.R.id.list);
                    listview.setVisibility(View.VISIBLE);
                    TextView nolist = (TextView) findViewById(R.id.nolist);
                    nolist.setVisibility(View.GONE);

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
                                if (entry.getKey() != "LoggedOn" && entry.getKey() != "Loggedat") {
                                    OrderDetails orderdet = mapper.convertValue(entry.getValue(), OrderDetails.class);
                                    if (IsOrderValid(orderdet)) {
                                        String[] timesplit = new String[0];
                                        boolean ispickupam = true;
                                        boolean isdeliveryam = true;

                                        if(!(orderdet.Time.contains("Mid"))) {
                                            orderdet.Time = orderdet.Time.toLowerCase();
                                            if(orderdet.Time.contains(":")) {
                                                orderdet.Time = orderdet.Time.replaceAll(":", ".");
                                            }

                                            timesplit = orderdet.Time.split("-");
                                            if(timesplit.length >= 2) {
                                                timesplit[0] = timesplit[0].replaceAll(" ", "");
                                                timesplit[1] = timesplit[1].replaceAll(" ", "");

                                                Boolean ispm = false;
                                                isdeliveryam = !(timesplit[1].contains("pm"));
                                                if(timesplit[0].contains("am")) {
                                                    ispm = false;
                                                    ispickupam = true;
                                                    timesplit[1] = timesplit[1].replaceAll("pm", "");
                                                    if (Double.parseDouble(timesplit[1]) == 12)
                                                        isdeliveryam = true;
                                                }
                                                else if(timesplit[0].contains("pm")){
                                                    ispm = true;
                                                    ispickupam = false;
                                                    timesplit[1] = timesplit[1].replaceAll("pm", "");
                                                    timesplit[1] = timesplit[1].replaceAll("pm", "");
                                                    if (Double.parseDouble(timesplit[1]) == 12)
                                                        isdeliveryam = true;
                                                }
                                                else {
                                                    if (timesplit[1].indexOf("pm") >= 0 && Double.parseDouble(timesplit[0]) >= 1 && Double.parseDouble(timesplit[0]) <= 11) {
                                                        timesplit[1] = timesplit[1].replaceAll("pm", "");
                                                        if (Double.parseDouble(timesplit[1]) != 12) {
                                                            ispm = true;
                                                            ispickupam = false;
                                                        }
                                                        else {
                                                            isdeliveryam = true;
                                                        }
                                                    }
                                                }

                                                timesplit[0] = timesplit[0].replaceAll("am", "");
                                                timesplit[0] = timesplit[0].replaceAll("pm", "");

                                                orderdet.TimeSort = (Double.isNaN(Double.parseDouble(timesplit[0])) ? 24 : (ispm ? (Double.parseDouble(timesplit[0]) + 12) : Double.parseDouble(timesplit[0])));
                                            }
                                            else
                                                orderdet.TimeSort = 0.0;
                                        }
                                        else {
                                            orderdet.TimeSort = 24.0;
                                        }

                                        orderdet.Name = upperCaseFirst(orderdet.Name);
                                        orderdet.Id = entry.getKey();

                                        if(orderdet.Deliveredon != null && orderdet.Deliveredon != "") {
                                            orderdet.Status = "Delivered";
                                            ((MyApp) context.getApplicationContext()).removeOrders(orderdet.Id, false);
                                        }
                                        else if(orderdet.Pickedon != null && orderdet.Pickedon != "") {
                                            orderdet.Status = "Picked up";
                                            ((MyApp) context.getApplicationContext()).removeOrders(orderdet.Id, true);
                                            ((MyApp) context.getApplicationContext()).addOrders(orderdet.Id, timesplit[1], isdeliveryam, false);
                                        }
                                        else {
                                            orderdet.Status = "Yet to pick";
                                            ((MyApp) context.getApplicationContext()).addOrders(orderdet.Id, timesplit[0], ispickupam, true);
                                            ((MyApp) context.getApplicationContext()).addOrders(orderdet.Id, timesplit[1], isdeliveryam, false);
                                        }
                                        orderDetaillist.add(orderdet);
                                    }
                                }
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
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                System.out.println("The read failed: " + firebaseError.getMessage());
            }
        });
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
        ListView listview = (ListView)findViewById(android.R.id.list);
        listview.setVisibility(View.GONE);
        TextView nolist = (TextView) findViewById(R.id.nolist);
        nolist.setVisibility(View.VISIBLE);
    }

    public static String upperCaseFirst(String value) {
        value = value.toLowerCase();
        char[] array = value.toCharArray();
        array[0] = Character.toUpperCase(array[0]);
        return new String(array);
    }

    public void changeDeviceId(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("ChangeDevice","true");
        startActivity(intent);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        ((MyApp) context.getApplicationContext()).setCurrentActivity(this);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        currentDate = sdf.format(new java.util.Date());

        TextView nolist = (TextView) findViewById(R.id.nolist);
        nolist.setVisibility(View.GONE);

        mDialog.StartProcessDialog();

        if(CheckLoggedIn()) {
            Firebase.setAndroidContext(this);
            getOrders();
        }
        else{
            mDialog.StopProcessDialog();
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        }
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
        intent.putExtra("Order",orderjson);
        startActivity(intent);
    }
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        OrderDetails orderDetails = orderDetaillist.get(position);
        showOrderDetails(orderDetails);
    }
}