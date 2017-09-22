package cn.ac.iscas.xlab.droidfacedog.mvp.commentary;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import cn.ac.iscas.xlab.droidfacedog.R;
import cn.ac.iscas.xlab.droidfacedog.RosConnectionService;

/**
 * Created by lisongting on 2017/9/22.
 */

public class CommentaryActivity extends AppCompatActivity {

    private ServiceConnection serviceConnection;
    private CommentaryFragment commentaryFragment;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.container_layout);

        commentaryFragment = CommentaryFragment.newInstance();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, commentaryFragment)
                .commit();

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("解说模式");
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onStart() {
        super.onStart();

        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                commentaryFragment.setRosServiceBinder((RosConnectionService.ServiceBinder) service);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        };

        Intent intent = new Intent(this, RosConnectionService.class);
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        unbindService(serviceConnection);
        super.onDestroy();

    }
}
