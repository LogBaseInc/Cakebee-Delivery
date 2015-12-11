package io.logbase.cakebeedelivery;

/**
 * Created by logbase on 20/11/15.
 */

import android.app.ListActivity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Button;
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
import io.logbase.cakebeedelivery.MainActivity.NotificationReceiver;

public class OrdersActivity extends ListActivity {
    List<OrderDetails> orderDetaillist;
    Context context;
    String currentDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.orders);
        context = this;
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
        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
        String deviceID = sharedPref.getString("deviceID", null);
        String accountID = sharedPref.getString("accountID", null);

        Firebase myFirebaseRef = new Firebase(getString(R.string.friebaseurl)+"accounts/"+accountID+"/orders/"+deviceID+"/"+currentDate);
        myFirebaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
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
                                    if ((orderdet.Pickedon == null || orderdet.Pickedon == "") && IsOrderValid(orderdet)) {
                                        orderdet.Name = upperCaseFirst(orderdet.Name);
                                        orderdet.Id = entry.getKey();
                                        orderDetaillist.add(orderdet);
                                    }
                                }
                            }

                            OrderlistAdapter listAdapter = new OrderlistAdapter(context, orderDetaillist);
                            setListAdapter(listAdapter);
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if (orderDetaillist.size() <= 0) {
                    NoOrders();
                }

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

    private void ShowToast(String message) {
        Toast toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
        toast.show();
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
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        currentDate = sdf.format(new java.util.Date());

        TextView nolist = (TextView) findViewById(R.id.nolist);
        nolist.setVisibility(View.GONE);

        if(CheckLoggedIn()) {
            Firebase.setAndroidContext(this);
            getOrders();
        }
        else{
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        OrderDetails orderDetails = orderDetaillist.get(position);
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
}