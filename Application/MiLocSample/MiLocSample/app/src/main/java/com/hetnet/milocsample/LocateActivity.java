package com.hetnet.milocsample;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PointF;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.hetnet.miloc.core.algo.ProfilePointF;
import com.hetnet.miloc.core.model.IProfile;
import com.hetnet.miloclib.LocEngine;
import com.hetnet.utility.core.LicenseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;


public class LocateActivity extends AppCompatActivity{

    private static final String KEY_PROFILE_ID = "ProfileId";
    private static final String KEY_POINTF = "PointF";

    private static final int REQUEST_CODE_GENERIC = 1;

    private Handler handler = null;
    private SimpleDateFormat df = null;

    private TextView noLocationText = null;
    private ScaleImageView mapView = null;
    private TextView tvName = null;
    private TextView tvTimestamp = null;
    private int mapWidth = 0;
    private int mapHeight = 0;

    private String prevProfileId = null;
    private PointF prevPoint = null;
    private ArrayList<IProfile> listProfile = new ArrayList<>();
    private String[] arrProfileName = null;
    private IProfile prevProfile = null;
    private boolean runLocAlgo = false;
    private Thread locAlgoThread = null;
    private LocEngine locEngine = null;  // locEngine is our backend engine
                                         // It is the only class you need to learn from our libraries

    private PointF prevShowPoint = null;
    private IProfile prevShowProfile = null;

    private ArrayList<String> listPerm = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_locate);

        Toolbar toolbar = (Toolbar) findViewById(R.id.tool_bar);
        setSupportActionBar(toolbar);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        mapWidth = displayMetrics.widthPixels;
        mapHeight = displayMetrics.heightPixels;

        noLocationText = (TextView) findViewById(R.id.noLocation);
        tvName = (TextView) findViewById(R.id.name);
        mapView = (ScaleImageView) findViewById(R.id.map);
        mapView.setViewMode(true);
        mapView.setViewPointSelect(true);
        tvTimestamp = (TextView) findViewById(R.id.timestamp);

        handler = new Handler();
        df = new SimpleDateFormat(getString(R.string.format_timestamp), Locale.getDefault());

        if (savedInstanceState != null) {
            prevProfileId = savedInstanceState.getString(KEY_PROFILE_ID);
            prevPoint = savedInstanceState.getParcelable(KEY_POINTF);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_locate, menu);
        return true;
    }

    //String[] NAMES = {"Connect", "Subscribe & Publish"};

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_me:
                mapView.setViewPointSelect(true);
                mapView.invalidate();
                return true;

            case R.id.action_profile:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.action_profile)
                        .setItems(arrProfileName, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                locEngine.switchProfile(listProfile.get(which));
                                dialog.dismiss();
                            }
                        });
                builder.create().show();
                return true;

            case R.id.action_refresh:
                Toast.makeText(this, "Refreshed", Toast.LENGTH_SHORT).show();
                return true;

            case R.id.action_mqtt:

            //    AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
            //    builder1.setTitle("")
            //            .setItems(NAMES, new DialogInterface.OnClickListener() {
             //               @Override
             //               public void onClick(DialogInterface dialog, int which) {
            //                    switch (which) {
            //                        case 0:
                                        Intent intent_connection = new Intent(LocateActivity.this, connection.class);
                                        startActivity(intent_connection);
            //                            break;


            //                    }
            //                    dialog.dismiss();
            //                }
            //            });
            //    builder1.create().show();
        }
                return super.onOptionsItemSelected(item);
        }


    @Override
    protected void onStart(){
        super.onStart();

        // more complete version see http://developer.android.com/training/permissions/requesting.html

        listPerm.clear();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            listPerm.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        // No GPS, use coarse location
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            listPerm.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        if (listPerm.size() > 0) {
            ActivityCompat.requestPermissions(this, listPerm.toArray(new String[0]), REQUEST_CODE_GENERIC);
        } else {
            startLocating();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        int numGranted = 0;
        for (int i = 0; i < grantResults.length; i++) {
            if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                numGranted++;
            }
        }
        if (listPerm.size() == numGranted) {
            startLocating();
        }
    }

    @Override
    protected void onStop() {
        if (locAlgoThread != null && locAlgoThread.isAlive()) {
            runLocAlgo = false;
            locAlgoThread.interrupt();
        }

        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (prevProfile != null)
            outState.putString(KEY_PROFILE_ID, prevProfile.getId());
        outState.putParcelable(KEY_POINTF, prevPoint);
    }

    private void startLocating() {
        locEngine = new LocEngine(this);  // initiate the locEngine
        locEngine.loadData();  // load data from database into locEngine
        for (IProfile profile : locEngine.getAllProfile())
            listProfile.add(profile);  // IProfile has the map, filename of the map, name, floor no., etc
        arrProfileName = new String[listProfile.size()];
        int i = 0;
        for (IProfile profile : listProfile) {
            arrProfileName[i] = profile.getName();
            i++;
        }

        if (prevProfileId != null) {
            prevProfile = locEngine.getProfile(prevProfileId);
            prevProfile.loadMap(mapWidth, mapHeight);  // call loadMap() you want to ask IProfile to load bitmap for you
            // else, just use getMapPath() to know where the map is
        }

        if (locAlgoThread == null || !locAlgoThread.isAlive()) {
            runLocAlgo = true;
            locAlgoThread = new Thread(locAlgoRunnable);
            locAlgoThread.setName("Location algorithm");
            locAlgoThread.start();
        }
    }

    private class ShowMyLocation implements Runnable {

        private IProfile currShowProfile;
        private PointF currShowPoint;

        public ShowMyLocation(IProfile currShowProfile, PointF currShowPoint) {
            this.currShowProfile = currShowProfile;
            this.currShowPoint = currShowPoint;
        }

        @Override
        public void run() {
            if (currShowPoint == null) {
                if (prevShowPoint != null) {
                    tvName.setText("");
                    mapView.setImage(null);
                    noLocationText.setVisibility(View.VISIBLE);
                }
            }
            else {
                if (prevShowPoint == null) {
                    tvName.setText(currShowProfile.getName());
                    mapView.setImage(currShowProfile.getMap());
                }
                else if (!prevShowProfile.getId().equals(currShowProfile.getId())) {
                    tvName.setText(currShowProfile.getName());
                    mapView.setImage(currShowProfile.getMap());
                }
                float scale = currShowProfile.getAfterMapScale();
                mapView.setPointSelect(new PointF(currShowPoint.x * scale, currShowPoint.y * scale));
                noLocationText.setVisibility(View.INVISIBLE);
            }
            tvTimestamp.setText(df.format(new Date()));

            prevShowProfile = currShowProfile;
            prevShowPoint = currShowPoint;
        }
    }

    private void showLicenseError(final String errMsg) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                android.support.v7.app.AlertDialog.Builder builder =
                        new android.support.v7.app.AlertDialog.Builder(LocateActivity.this);
                builder.setTitle(R.string.error_title_license);
                builder.setMessage(errMsg);
                builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                builder.create().show();
            }
        });
    }

    private Runnable locAlgoRunnable = new Runnable() {
        @Override
        public void run() {
            long threshold = 2000;  // ms
            long duration;
            long startTime = 0;
            long endTime = 0;

            IProfile currProfile;
            PointF currPoint;

            try {
                locEngine.start();  // start locEngine
            } catch (LicenseException e) {
                showLicenseError(e.getMessage());
                runLocAlgo = false;
                return;
            }
            locEngine.switchProfile(prevProfile);

            // draw previously saved location
            handler.post(new ShowMyLocation(prevProfile, prevPoint));  // use ShowMyLocation
            // to show the location, or use your own way to display the user location

            while(runLocAlgo) {
                try {
                    duration = startTime + threshold - endTime;
                    if (duration > 0)
                        Thread.sleep(duration);
                } catch (InterruptedException e) {
                    // ignore
                }

                if (!runLocAlgo)
                    break;

                startTime = new Date().getTime();
                ProfilePointF userLoc = locEngine.getLoc();  // get current location
                if (userLoc == null) {
                    // location not found
                    currPoint = null;
                    currProfile = prevProfile;
                }
                else {
                    currPoint = new PointF(userLoc.pointF.x, userLoc.pointF.y);
                    if (prevProfile == null || !prevProfile.getId().equals(userLoc.profileId)) {
                        currProfile = locEngine.getProfile(userLoc.profileId);
                        currProfile.loadMap(mapWidth, mapHeight);
                    } else {
                        currProfile = prevProfile;
                    }
                }
                handler.post(new ShowMyLocation(currProfile, currPoint));  // use ShowMyLocation
                // to show the location, or use your own way to display the user location
                prevProfile = currProfile;
                prevPoint = currPoint;
                endTime = new Date().getTime();
            }
            locEngine.stop();  // stop the locEngine when no longer needed
        }
    };

}



