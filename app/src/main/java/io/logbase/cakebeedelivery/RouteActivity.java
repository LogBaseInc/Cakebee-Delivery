package io.logbase.cakebeedelivery;

import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import android.os.AsyncTask;
import android.app.ProgressDialog;
import android.graphics.Color;
import java.util.ArrayList;
import org.w3c.dom.Document;
import com.google.android.gms.maps.CameraUpdateFactory;
import android.content.Context;
import android.widget.Toast;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;

public class RouteActivity extends FragmentActivity {

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    Document document;
    GMapV2GetRouteDirection v2GetRouteDirection;
    LatLng fromPosition;
    LatLng toPosition;
    MarkerOptions markerOptions;
    LBProcessDialog mDialog= null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route);
        mDialog = new LBProcessDialog(this);
        setUpMapIfNeeded();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        Bundle b = getIntent().getExtras();

        v2GetRouteDirection = new GMapV2GetRouteDirection();
        mMap.getUiSettings().setZoomControlsEnabled(true);

        mMap.animateCamera(CameraUpdateFactory.zoomTo(14));
        markerOptions = new MarkerOptions();
        fromPosition = new LatLng(b.getDouble("fromlat"), b.getDouble("fromlng"));
        toPosition = new LatLng(b.getDouble("tolat"), b.getDouble("tolng"));

        GetRouteTask getRoute = new GetRouteTask();
        getRoute.execute();
    }

    private class GetRouteTask extends AsyncTask<String, Void, String> {
        String response = "";
        @Override
        protected void onPreExecute() {
            mDialog.StartProcessDialog();
        }

        @Override
        protected String doInBackground(String... urls) {
            //Get All Route values
            document = v2GetRouteDirection.getDocument(fromPosition, toPosition, GMapV2GetRouteDirection.MODE_DRIVING);
            response = "Success";
            return response;
        }

        @Override
        protected void onPostExecute(String result) {
            if(response.equalsIgnoreCase("Success")){
                ArrayList<LatLng> directionPoint = v2GetRouteDirection.getDirection(document);
                if(directionPoint.size() > 0) {
                    PolylineOptions rectOptions = new PolylineOptions().width(10).color(
                            Color.RED);
                    int midpoint = (directionPoint.size() / 2);
                    for (int i = 0; i < directionPoint.size(); i++) {
                        rectOptions.add(directionPoint.get(i));
                    }
                    // Get back the mutable Polyline
                    mMap.addPolyline(rectOptions);

                    mMap.moveCamera(CameraUpdateFactory.newLatLng(directionPoint.get(midpoint)));
                }
                else {
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(toPosition));
                    showToast("No route found");
                }

                mMap.animateCamera(CameraUpdateFactory.zoomTo(14));

                MarkerOptions tomarker = new MarkerOptions().position(toPosition);
                tomarker.icon(BitmapDescriptorFactory.fromResource(R.drawable.reddot));
                mMap.addMarker(tomarker);

                MarkerOptions frommarker = new MarkerOptions().position(fromPosition);
                frommarker.icon(BitmapDescriptorFactory.fromResource(R.drawable.greendot));
                mMap.addMarker(frommarker);
                mDialog.StopProcessDialog();

            }
            else {
                showToast("Something went wrong, try after sometime");
            }

        }
    }
    @Override
    protected void onStop() {
        super.onStop();
        finish();
    }

    private void showToast(String message) {
        mDialog.StopProcessDialog();
        Toast toast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        toast.show();
    }
}
