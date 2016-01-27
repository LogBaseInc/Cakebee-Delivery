package io.logbase.cakebeedelivery;

/**
 * Created by logbase on 25/01/16.
 */
import android.app.IntentService;
import android.content.Intent;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

public class ActivityRecognitionIntentService extends IntentService {
    //LogCat
    private static final String TAG = ActivityRecognitionIntentService.class.getSimpleName();

    public ActivityRecognitionIntentService() {
        super("ActivityRecognitionIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if(ActivityRecognitionResult.hasResult(intent)) {
            //Extract the result from the Response
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
            DetectedActivity detectedActivity = result.getMostProbableActivity();

            //Get the Confidence and Name of Activity
            int confidence = detectedActivity.getConfidence();
            String mostProbableName = getActivityName(detectedActivity.getType());

            //Fire the intent with activity name & confidence
            Intent i = new Intent("ImActive");
            i.putExtra("activity", mostProbableName);
            i.putExtra("confidence", confidence);

            //System.out.println("Most Probable Name : " + mostProbableName + " Confidence : " + confidence);

            ((MyApp)this.getApplicationContext()).AddCurrentActivityLog("Activity: " + mostProbableName+ " Confidence:" + confidence);

            //Send Broadcast to be listen in MainActivity
            this.sendBroadcast(i);

        }else {
            System.out.println("Intent had no data returned");
        }
    }

    //Get the activity name
    private String getActivityName(int type) {
        switch (type)
        {
            case DetectedActivity.IN_VEHICLE:
                return "In Vehicle";
            case DetectedActivity.ON_BICYCLE:
                return "On Bicycle";
            case DetectedActivity.ON_FOOT:
                return "On Foot";
            case DetectedActivity.WALKING:
                return "Walking";
            case DetectedActivity.STILL:
                return "Still";
            case DetectedActivity.TILTING:
                return "Tilting";
            case DetectedActivity.RUNNING:
                return "Running";
            case DetectedActivity.UNKNOWN:
                return "Unknown";
        }
        return "N/A";
    }
}
