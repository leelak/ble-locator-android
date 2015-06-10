package com.tgaubert.blefinder;

import android.app.Dialog;
import android.app.NotificationManager;
import android.app.Service;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private BLEDataTracker bleDataTracker;
    private NotificationManager notifyMgr;
    private Handler refreshHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bleDataTracker = new BLEDataTracker(getApplicationContext());
        notifyMgr = (NotificationManager) getSystemService(Service.NOTIFICATION_SERVICE);
        if(!bleDataTracker.isTracking())
            notifyMgr.cancel(1);

        setContentView(R.layout.activity_main);

        refreshHandler = new Handler();
        refreshHandler.postDelayed(refreshRunner, 5000);
    }

    @Override
    public void onPause() {
        super.onPause();
        BeaconIO.saveBeacons(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        notifyMgr.cancel(1);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if(id == R.id.action_reset) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Reset Beacons");
            builder.setMessage("Are you sure you want to reset beacon data?");
            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    BeaconIO.getSeenBeacons().clear();
                    Snackbar.make(findViewById(R.id.floating_btn), "Beacon data reset.", Snackbar.LENGTH_LONG).show();
                }
            });
            builder.setNegativeButton("No", null);
            builder.show();
        }

        if (id == R.id.action_about) {
            final Dialog dialog = new Dialog(this);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.dialog_about);
            WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
            params.width = WindowManager.LayoutParams.MATCH_PARENT;
            dialog.getWindow().setAttributes(params);
            dialog.show();

            PackageInfo pInfo = null;
            try {
                pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }

            if(pInfo != null)
                ((TextView)dialog.findViewById(R.id.appVersion)).setText(pInfo.versionName);

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public BLEDataTracker getBleDataTracker() {
        return bleDataTracker;
    }

    private final Runnable refreshRunner = new Runnable() {
        public void run() {
            if(bleDataTracker != null && bleDataTracker.isTracking()) {
                BeaconListAdapter adapter = ((BeaconListAdapter) ((EmptyRecyclerView) findViewById(R.id.beaconList)).getAdapter());
                adapter.set(bleDataTracker.getValidBeacons());
                adapter.notifyDataSetChanged();

                bleDataTracker.updateState();
                FloatingActionButton floatingActionButton = (FloatingActionButton) findViewById(R.id.floating_btn);
                if(bleDataTracker.isTracking()) {
                    floatingActionButton.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.bt_scan, null));
                    ((TextView) findViewById(R.id.msgSubtitle)).setText("Start scanning to find beacons.");
                } else {
                    floatingActionButton.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.bluetooth, null));
                    ((TextView) findViewById(R.id.msgSubtitle)).setText("No beacons found nearby.");
                }
            }

            refreshHandler.postDelayed(refreshRunner, 1000);
        }
    };
}
