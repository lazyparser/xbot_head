package cn.ac.iscas.xlab.droidfacedog.mvp.interaction;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;

import cn.ac.iscas.xlab.droidfacedog.R;
import cn.ac.iscas.xlab.droidfacedog.RosConnectionService;

/**
 * Created by lisongting on 2017/8/7.
 */

public class InteractionActivity extends AppCompatActivity {

    InteractionFragment interactionFragment;
    ServiceConnection rosServiceConnection;
    public static final String TAG = "InteractionActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.container_layout);
        interactionFragment = InteractionFragment.newInstance();

        getSupportFragmentManager().beginTransaction()
                .add(R.id.container, interactionFragment)
                .commit();

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle("综合交互");

    }

    @Override
    protected void onStart() {
        super.onStart();
        rosServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                //Service代理对象的两次传递
                interactionFragment.setRosServiceBinder((RosConnectionService.ServiceBinder)service);
                Log.i(TAG, "rosServiceConnection -- onServiceConnected");
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        };
        Intent intent = new Intent(this, RosConnectionService.class);
        bindService(intent, rosServiceConnection, 0);

    }

    @Override
    protected void onResume() {
        super.onResume();


    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return true;
    }
}
