package cn.ac.iscas.xlab.droidfacedog.mvp.facesign;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;

import cn.ac.iscas.xlab.droidfacedog.R;
import cn.ac.iscas.xlab.droidfacedog.RosConnectionService;

/**
 * Created by lisongting on 2017/9/12.
 */

public class SignInActivity extends AppCompatActivity{

    public static final String TAG = "SignInActivity";
    SignInFragment signInFragment;
    ServiceConnection serviceConnection;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.container_layout);

        signInFragment = SignInFragment.newInstance();
        getSupportFragmentManager().beginTransaction()
                .add(R.id.container, signInFragment)
                .commit();

        ActionBar bar = getSupportActionBar();
        bar.setTitle("人脸签到");
        bar.setDisplayHomeAsUpEnabled(true);

    }

    @Override
    protected void onStart() {
        super.onStart();
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                signInFragment.setRosServiceBinder((RosConnectionService.ServiceBinder) service);
                Log.i(TAG, "rosServiceConnection -- onServiceConnected");
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        };

        Intent intent = new Intent(this, RosConnectionService.class);
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        unbindService(serviceConnection);
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }
}
