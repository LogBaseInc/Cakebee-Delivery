package io.logbase.cakebeedelivery;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.firebase.client.Firebase;
import java.text.SimpleDateFormat;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by logbase on 06/02/16.
 */
public class TaskActivity extends Activity {
    boolean taskstopped = false;
    String starttime = null;
    MyApp myapp = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.task);
        Firebase.setAndroidContext(this);
        myapp = ((MyApp) this.getApplicationContext());

        Button starttrackingbtn = (Button)findViewById(R.id.starttrackingbtn);
        starttrackingbtn.getBackground().setColorFilter(0xFF00b5ad, PorterDuff.Mode.MULTIPLY);

        Button stoptrackingbtn = (Button)findViewById(R.id.stoptrackingbtn);
        stoptrackingbtn.getBackground().setColorFilter(0xFF00b5ad, PorterDuff.Mode.MULTIPLY);
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
        String stoptime = sdf.format(new java.util.Date());

        sdf = new SimpleDateFormat("yyyyMMdd");
        String currentDate = sdf.format(new java.util.Date());

        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
        String deviceID = sharedPref.getString("deviceID", null);
        String accountID = sharedPref.getString("accountID", null);

        EditText notetext = (EditText)findViewById(R.id.notetext);
        String notes = notetext.getText().toString();

        Map<String, String> task = new HashMap<String, String>();
        task.put("starttime", starttime);
        task.put("stoptime", stoptime);
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
    public void onBackPressed() {
        Button starttrackingbtn = (Button)findViewById(R.id.starttrackingbtn);
        if(starttrackingbtn.getVisibility() == View.VISIBLE || taskstopped == true)
            super.onBackPressed();
    }
}
