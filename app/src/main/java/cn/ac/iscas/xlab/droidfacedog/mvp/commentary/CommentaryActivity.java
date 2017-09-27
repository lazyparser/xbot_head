package cn.ac.iscas.xlab.droidfacedog.mvp.commentary;

import android.content.ServiceConnection;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.WindowManager;

import cn.ac.iscas.xlab.droidfacedog.R;
import cn.ac.iscas.xlab.droidfacedog.RosConnectionService;
import cn.ac.iscas.xlab.droidfacedog.XbotApplication;

/**
 * Created by lisongting on 2017/9/22.
 */

public class CommentaryActivity extends AppCompatActivity {

    private ServiceConnection serviceConnection;
    private CommentaryFragment commentaryFragment;
    private RosConnectionService.ServiceBinder serviceProxy;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.container_layout);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

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
    }

    @Override
    protected void onResume() {
        super.onResume();
        XbotApplication app = (XbotApplication) getApplication();
        serviceProxy = app.getServiceProxy();
        if (serviceProxy != null) {
            commentaryFragment.setRosServiceBinder(serviceProxy);
            serviceProxy.manipulateTopic(RosConnectionService.SUBSCRIBE_MUSEUM_POSITION, true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (serviceProxy != null) {
            commentaryFragment.setRosServiceBinder(serviceProxy);
            serviceProxy.manipulateTopic(RosConnectionService.SUBSCRIBE_MUSEUM_POSITION, false);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }
}
