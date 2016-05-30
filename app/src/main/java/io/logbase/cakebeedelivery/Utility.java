package io.logbase.cakebeedelivery;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.text.SimpleDateFormat;
import com.sendgrid.SendGrid;
import android.content.pm.PackageManager;
import android.support.v4.content.ContextCompat;
/**
 * Created by logbase on 26/04/16.
 */

public class Utility {

    public static void sendActivity(String accountID, String deviceID, String orderId, String activity, SharedPreferences sharedPref){
        String webhookUrl = null;
        boolean webhookEnabled =  sharedPref.getBoolean("WebhookEnabled", false);
        if(webhookEnabled == true) {
            webhookUrl = sharedPref.getString("WebhookUrl", null);
        }

        JSONObject order = new JSONObject();
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            String currentDate = sdf.format(new java.util.Date());

            order.put("order_id", orderId);
            order.put("account_id", accountID);
            order.put("hook_url", webhookUrl);
            order.put("delivery_date", currentDate);
            order.put("activity", activity);
            order.put("time_ms", System.currentTimeMillis());
            order.put("device_id", deviceID);

            System.out.println("order " + order);
            excutePost(order);

        } catch (JSONException e) {
            e.printStackTrace();
            System.out.println("JSONException " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());
        }
    }

    public static boolean checkLocationPermission()
    {
        int res = ContextCompat.checkSelfPermission(MyApp.getContext(), android.Manifest.permission.ACCESS_FINE_LOCATION);
        return (res == PackageManager.PERMISSION_GRANTED);
    }

    // HTTP POST request
    private static void excutePost(JSONObject order) throws Exception
    {
        final String body = order.toString();
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                String type = "application/json";
                URL url = null;
                boolean repeat = false;
                do {
                    String error = null;
                    int responsecode = 0;
                    HttpURLConnection conn = null;
                    try {

                        url = new URL("http://stick-write-dev.logbase.io/api/events/app");
                        conn = (HttpURLConnection) url.openConnection();
                        conn.setReadTimeout(10000 /* milliseconds */);
                        conn.setConnectTimeout(15000 /* milliseconds */);
                        conn.setInstanceFollowRedirects(false);
                        conn.setRequestMethod("POST");
                        conn.setDoInput(true);
                        conn.setDoOutput(true);
                        conn.setRequestProperty("Content-Type", "application/json");
                        conn.setRequestProperty("charset", "utf-8");
                        conn.setRequestProperty("Content-Length", "" + Integer.toString(body.getBytes().length));
                        conn.setUseCaches(false);

                        DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
                        wr.writeBytes(body);
                        wr.flush();
                        wr.close();

                        conn.connect();

                        responsecode = conn.getResponseCode();
                        System.out.println("responsecode " + responsecode);
                        if (responsecode == 200) {
                            repeat = false;
                            error = null;
                        } else {
                            repeat = !repeat;
                            error = conn.getResponseMessage();
                        }

                    } catch (MalformedURLException e) {
                        repeat = !repeat;
                        error = "MalformedURLException";
                        e.printStackTrace();
                    } catch (ProtocolException e) {
                        repeat = !repeat;
                        error = "ProtocolException";
                        e.printStackTrace();
                    } catch (IOException e) {
                        repeat = !repeat;
                        error = "IOException";
                        e.printStackTrace();
                    }

                    if (conn != null)
                        conn.disconnect();

                }
                while (repeat == true);
            }
        });
    }
}

class SendEmail extends AsyncTask<String, Void, Void> {

    private Exception exception;

    protected Void doInBackground(String... message) {
        try {
            SendGrid sendgrid = new SendGrid("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");

            SendGrid.Email email = new SendGrid.Email();
            email.addTo("kousik@logbase.io");
            email.setFrom("StickAgent-App@logbase.io");
            email.setSubject(message[0]);
            email.setText(message[1]);

            // Send email, execute http request
            SendGrid.Response response = sendgrid.send(email);
            String mMsgResponse = response.getMessage();
        } catch (Exception e) {
            System.out.println("SendAppExample " + e.toString());
        }
        return null;
    }
}
