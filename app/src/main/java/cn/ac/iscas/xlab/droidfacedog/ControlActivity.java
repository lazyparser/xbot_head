package cn.ac.iscas.xlab.droidfacedog;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

/**
 * Created by lisongting on 2017/6/26.
 */

public class ControlActivity extends AppCompatActivity implements View.OnClickListener{

    public static final String TAG = "ControlActivity";
    public static final int CTRL_FORWARD = 0x11;
    public static final int CTRL_BACK = 0x12;
    public static final int CTRL_LEFT = 0x13;
    public static final int CTRL_RIGHT = 0x14;

    private ImageButton btForward;
    private ImageButton btBack;
    private ImageButton btLeft;
    private ImageButton btRight;
    private RosConnectionService.ServiceBinder serviceProxy;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(TAG, "ServiceConnection -- onServiceConnected()");
            serviceProxy = (RosConnectionService.ServiceBinder) service;

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i(TAG, "ServiceConnection -- onServiceDisconnected()");
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control);
        getSupportActionBar().setTitle("控制器");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        btForward = (ImageButton) findViewById(R.id.image_button_up);
        btBack = (ImageButton) findViewById(R.id.image_button_down);
        btLeft = (ImageButton) findViewById(R.id.image_button_left);
        btRight = (ImageButton) findViewById(R.id.image_button_right);
        btForward.setOnClickListener(this);
        btBack.setOnClickListener(this);
        btLeft.setOnClickListener(this);
        btRight.setOnClickListener(this);

        Intent intent = new Intent(this, RosConnectionService.class);
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);
    }

    @Override
    public void onClick(View v) {
        if (!serviceProxy.isConnected()) {
            Toast.makeText(this, "未连接Ros服务端", Toast.LENGTH_SHORT).show();
            return;
        }
        switch (v.getId()) {
            case R.id.image_button_up:
                serviceProxy.publishMoveTopic(CTRL_FORWARD);
                break;
            case R.id.image_button_down:
                serviceProxy.publishMoveTopic(CTRL_BACK);
                break;
            case R.id.image_button_left:
                serviceProxy.publishMoveTopic(CTRL_LEFT);
                break;
            case R.id.image_button_right:
                serviceProxy.publishMoveTopic(CTRL_RIGHT);
                break;
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
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause(){
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(serviceProxy.isConnected()){
            unbindService(serviceConnection);
        }
    }

}
