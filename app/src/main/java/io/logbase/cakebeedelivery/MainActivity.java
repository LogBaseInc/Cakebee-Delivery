package io.logbase.cakebeedelivery;

import android.app.Activity;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.content.Intent;
import android.widget.Button;
import android.content.SharedPreferences;
import android.content.Context;
import android.widget.EditText;
import android.widget.Toast;

import com.firebase.client.ChildEventListener;
import com.firebase.client.Firebase;
import com.firebase.client.ValueEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.FirebaseError;

import java.text.SimpleDateFormat;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import android.media.RingtoneManager;
import android.net.Uri;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.widget.TextView;
import android.media.AudioManager;

public class MainActivity extends Activity {

    Context context;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;
        Firebase.setAndroidContext(this);

        Button savebutton = (Button)findViewById(R.id.savebutton);
        savebutton.getBackground().setColorFilter(0xFF00b5ad, PorterDuff.Mode.MULTIPLY);

        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
        String deviceID = sharedPref.getString("deviceID", null);
        String accountID = sharedPref.getString("accountID", null);
        if(accountID != null && deviceID != null) {
            String username = sharedPref.getString("username", null);
            String accountname = sharedPref.getString("accountname", null);

            EditText usernametext = (EditText)findViewById(R.id.username);
            usernametext.setText(username);

            EditText accountnametext = (EditText)findViewById(R.id.accountname);
            accountnametext.setText(accountname);

            OrderAdded(accountID, deviceID);

            Bundle b = getIntent().getExtras();
            if(b == null || (b.getString("ChangeDevice") == null)) {
                Intent intent = new Intent(this, LoginActivity.class);
                startActivity(intent);
            }
        }
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

    public void saveDeviceID(View view) {
        EditText accountnameText = (EditText)findViewById(R.id.accountname);
        String accountname = accountnameText.getText().toString();

        EditText usernameText = (EditText)findViewById(R.id.username);
        String username = usernameText.getText().toString();

        if((accountname != null) && (!accountname.equals("")) && (username != null) && (!username.equals(""))) {
            CheckDeviceId(accountname, username);
        }
        else {
            ShowToast("Unable to save blank ID");
        }
    }

    private void CheckDeviceId(String actname, String username) {
        final String accountname = actname.toLowerCase();
        username = username.toLowerCase();

        final Button savebutton = (Button)findViewById(R.id.savebutton);
        savebutton.setEnabled(false);

        Firebase myFirebaseRef = new Firebase(getString(R.string.friebaseurl)+"/accountusers/"+accountname+"/users/"+username);
        myFirebaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                savebutton.setEnabled(true);

                if (snapshot.getValue() != null) {
                    GetAccountId(accountname, snapshot.getValue().toString());
                } else {
                    ShowToast("Account name or User name is incorrect");
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                System.out.println("The read failed: " + firebaseError.getMessage());
            }
        });
    }

    private void GetAccountId(String accountname, String devID) {
        final String deviceId = devID;
        Firebase myFirebaseRef = new Firebase(getString(R.string.friebaseurl)+"/accountusers/"+accountname+"/accountid");
        myFirebaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                GoToLogin(snapshot.getValue().toString(), deviceId);
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

    private void GoToLogin(String accountID, String deviceID) {
        EditText accountnameText = (EditText)findViewById(R.id.accountname);
        String accountname = accountnameText.getText().toString();

        EditText usernameText = (EditText)findViewById(R.id.username);
        String username = usernameText.getText().toString();

        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("deviceID", deviceID);
        editor.putString("accountID", accountID);
        editor.putString("accountname", accountname);
        editor.putString("username", username);
        editor.commit();
        OrderAdded(accountID, deviceID);

        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
    }

    private void OrderAdded(String accountID, String deviceID){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String currentDate = sdf.format(new java.util.Date());

        Firebase myFirebaseRef = new Firebase(getString(R.string.friebaseurl)+"accounts/"+accountID+"/orders/"+deviceID+"/"+currentDate);
        myFirebaseRef.addChildEventListener(new ChildEventListener() {
            // Retrieve new posts as they are added to the database
            @Override
            public void onChildAdded(DataSnapshot snapshot, String previousChildKey) {
                if (snapshot.getKey().toString().contains("Logged") == false) {
                    ArrayList<String> orderlist;
                    SharedPreferences sharedPref = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    Set orders = sharedPref.getStringSet("OrderIds", null);
                    if (orders != null) {
                        orderlist = new ArrayList<String>(orders);
                        if (orderlist.indexOf(snapshot.getKey()) < 0) {
                            System.out.println("orderlist" + orderlist);
                            orders.add(snapshot.getKey());
                            editor.putStringSet("OrderIds", orders);
                            editor.commit();

                            NotifyNewOrder();
                        }
                    } else {
                        orders = new HashSet<String>();
                        orders.add(snapshot.getKey());
                        editor.putStringSet("OrderIds", orders);
                        editor.commit();

                        NotifyNewOrder();
                    }
                }
            }

            @Override
            public void onChildRemoved(DataSnapshot var1) {

            }

            @Override
            public void onChildChanged(DataSnapshot var1, String var2) {

            }

            @Override
            public void onChildMoved(DataSnapshot var1, String var2) {

            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                System.out.println("The read failed: " + firebaseError.getMessage());
            }
        });
    }

    private void NotifyNewOrder(){
        try {
            // define sound URI, the sound to be played when there's a notification
            Intent notificationIntent = new Intent(context, OrdersActivity.class);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            PendingIntent intent = PendingIntent.getActivity(context, 0,
                    notificationIntent, 0);

            Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            NotificationManager notificationManager = (NotificationManager) context
                    .getSystemService(Context.NOTIFICATION_SERVICE);
            Notification notification = new Notification.Builder(this)
                    .setContentTitle("Cakebee")
                    .setContentText("New order has been assigned")
                    .setSmallIcon(R.drawable.icon)
                    .setSound(soundUri)
                    .setFullScreenIntent(intent, true)
                    .setVibrate(new long[] {1000, 1000, 1000, 1000, 1000})
                    .getNotification();

            //notification.setLatestEventInfo(context, "Cakebee", "New order has been assigned", intent);
            notification.flags |= Notification.FLAG_AUTO_CANCEL;
            notificationManager.notify(0, notification);
        }
        catch (Exception e) {
            ShowToast(e.getMessage());
        }

    }

    public class NotificationReceiver extends Activity {

        @Override
        protected void onCreate(Bundle savedInstanceState) {

            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);

            TextView tv = new TextView(this);
            tv.setText("New Order!");

            setContentView(tv);
        }
    }

}
