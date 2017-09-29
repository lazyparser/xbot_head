package cn.ac.iscas.xlab.droidfacedog.mvp.facesign;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import cn.ac.iscas.xlab.droidfacedog.R;
import cn.ac.iscas.xlab.droidfacedog.RosConnectionService;
import cn.ac.iscas.xlab.droidfacedog.XbotApplication;

/**
 * Created by lisongting on 2017/9/12.
 */

public class SignInActivity extends AppCompatActivity{

    public static final String TAG = "SignInActivity";
    SignInFragment signInFragment;
    RosConnectionService.ServiceBinder serviceProxy;

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
    }

    @Override
    protected void onResume() {
        super.onResume();
        XbotApplication app = (XbotApplication) getApplication();
        serviceProxy = app.getServiceProxy();
        if (serviceProxy != null) {
            signInFragment.setRosServiceBinder(serviceProxy);
            serviceProxy.manipulateTopic(RosConnectionService.SUBSCRIBE_ROBOT_STATUS,true);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (serviceProxy != null) {
            signInFragment.setRosServiceBinder(serviceProxy);
            serviceProxy.manipulateTopic(RosConnectionService.SUBSCRIBE_ROBOT_STATUS,false);
        }
    }

    @Override
    protected void onDestroy() {
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
