package cn.ac.iscas.xlab.droidfacedog;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;

import cn.ac.iscas.xlab.droidfacedog.CustomViews.CircleRotateView;
import cn.ac.iscas.xlab.droidfacedog.config.Config;
import cn.ac.iscas.xlab.droidfacedog.mvp.aitalk.AITalkActivity;

/**
 * Created by Nguyen on 5/20/2016.
 */

public class MainActivity extends AppCompatActivity {

    public static final String TAG = MainActivity.class.getSimpleName();
    public static final String ROS_RECEIVER_INTENTFILTER = "main_activity.receiver";

    public static final int REGISTER_ACTIVITY = 1;
    public static final int XBOTFACE_ACTIVITY = 2;
    public static final int AITALK_ACTIVITY = 3;
    public static final int CONN_ROS_SERVER_SUCCESS = 0x11;
    public static final int CONN_ROS_SERVER_ERROR = 0x12;

    private Context mContext;
    private Handler hanlder;
    private Button btnConnBackground;
    private RelativeLayout funRegister;
    private RelativeLayout funCommentary;
    private RelativeLayout funSettings;
    private RelativeLayout funAiTalk;
    private CircleRotateView circleRotateView;
    private FragmentManager fragmentManager;
    private WaitingDialogFragment waitingDialogFragment;
    private RosBroadcastReceiver receiver;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "MainActivity -- onCreate()");
        setContentView(R.layout.activity_main);
        mContext = this;

        initView();

        initConfiguration();

        showWaitingDialog();

        hanlder = new Handler(){
            @Override
            public void handleMessage(Message msg) {

                if (msg.what == CONN_ROS_SERVER_SUCCESS) {
                    if (waitingDialogFragment.isVisible()) {
                        waitingDialogFragment.dismiss();
                        Toast.makeText(MainActivity.this, "连接成功", Toast.LENGTH_SHORT).show();
                    }
                }else if(msg.what == CONN_ROS_SERVER_ERROR){
                    Log.i(TAG, "Ros连接失败");
                }
            }
        };
        initBroadcastReceiver();

    }

    private void initBroadcastReceiver() {
        receiver = new RosBroadcastReceiver(new RosBroadcastReceiver.RosCallback() {
            @Override
            public void onSuccess() {
                if (waitingDialogFragment.isVisible()) {
                    circleRotateView.endAnimation();
                    waitingDialogFragment.dismiss();
                    Toast.makeText(mContext, "Ros服务器连接成功", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(mContext, "Ros服务器连接成功", Toast.LENGTH_SHORT).show();
                }

            }
            @Override
            public void onFailure() {
            }
        });
        IntentFilter filter = new IntentFilter(ROS_RECEIVER_INTENTFILTER);
        registerReceiver(receiver,filter);

    }

    private void initView() {

        funRegister = (RelativeLayout) findViewById(R.id.function_register);
        funCommentary = (RelativeLayout) findViewById(R.id.function_commentary);
        funSettings = (RelativeLayout) findViewById(R.id.function_settings);
        funAiTalk = (RelativeLayout) findViewById(R.id.function_aitalk);

        funRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int rc = ActivityCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA);
                if (rc == PackageManager.PERMISSION_GRANTED) {
                    Intent intent = new Intent(mContext, RegisterActivity.class);
                    startActivity(intent);
                } else {
                    requestCameraPermission(REGISTER_ACTIVITY);
                }
            }
        });

        funCommentary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int rc = ActivityCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA);
                if (rc == PackageManager.PERMISSION_GRANTED) {
                    Intent intent = new Intent(mContext, XBotFaceActivity.class);
                    startActivity(intent);
                } else {
                    requestCameraPermission(XBOTFACE_ACTIVITY);
                }
            }
        });


        funAiTalk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int rc = ActivityCompat.checkSelfPermission(mContext, Manifest.permission.RECORD_AUDIO);
                if (rc == PackageManager.PERMISSION_GRANTED) {
                    startActivity(new Intent(mContext, AITalkActivity.class));
                } else {
                    requestCameraPermission(AITALK_ACTIVITY);
                }

            }
        });

        funSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, SettingsActivity.class);
                startActivityForResult(intent, 1);
            }
        });
    }


    @Override
    protected void onResume() {
        Log.i(TAG, "MainActivity -- onResume()");
        super.onResume();

        btnConnBackground = waitingDialogFragment.getBtConnectBackGround();
        circleRotateView = waitingDialogFragment.getCircleRotateView();

        btnConnBackground.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                circleRotateView.endAnimation();
                waitingDialogFragment.dismiss();
            }
        });

    }

    //在该函数中，获取settings中设置的所有属性值，并将相关的值存放在Config中
    public void initConfiguration() {
        Resources res = getResources();

        SharedPreferences sharedPreference = PreferenceManager.getDefaultSharedPreferences(this);

        int intThreshold = sharedPreference.getInt(res.getString(R.string.key_recog_threshold),60);

        Config.RECOG_THRESHOLD = (double) intThreshold / 100;

        Config.ENABLE_MESSAGE_NOTIFICATION = sharedPreference.getBoolean(res.getString(R.string.key_enable_notification),true);

        Config.ROS_SERVER_IP = sharedPreference.getString(res.getString(R.string.key_ros_server_ip), "192.168.1.111");

        Config.RECOGNITION_SERVER_IP = sharedPreference.getString(res.getString(R.string.key_recognition_server_ip), "192.168.0.111");

        Log.i(TAG, "MainActivity启动时初始化：" + Config.string());
    }

    private void requestCameraPermission(final int requestCode) {
        Log.w(TAG, "Camera permission is not granted. Requesting permission");

        final String[] permissions = new String[]{Manifest.permission.CAMERA,Manifest.permission.RECORD_AUDIO};

        ActivityCompat.requestPermissions(this, permissions, requestCode);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Intent intent = null;
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            switch (requestCode) {
                case REGISTER_ACTIVITY:
                    intent = new Intent(mContext, RegisterActivity.class);
                    break;
                case XBOTFACE_ACTIVITY:
                    intent = new Intent(mContext, XBotFaceActivity.class);
                    break;
                case AITALK_ACTIVITY:
                    intent = new Intent(mContext, AITalkActivity.class);
                    break;
                default:
                    break;
            }
            if (intent != null) {
                startActivity(intent);
            }
        }

        Log.e(TAG, "Permission not granted: results len = " + grantResults.length +
                " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));
    }

    private void showWaitingDialog() {
        //使用自定义FragmentDialog来显示等待界面
        waitingDialogFragment = new WaitingDialogFragment();
        fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .add(waitingDialogFragment, "waitingDialog")
                .commit();
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(receiver);
        super.onDestroy();
    }

    public static class RosBroadcastReceiver extends BroadcastReceiver {

        RosCallback callback;
        interface RosCallback{
            void onSuccess();
            void onFailure();
        }

        public RosBroadcastReceiver() {

        }
        public RosBroadcastReceiver(RosCallback callback) {
            this.callback = callback;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle data = intent.getExtras();
            int rosStatus = data.getInt("ros_conn_status");
            if (rosStatus == CONN_ROS_SERVER_SUCCESS) {
                callback.onSuccess();
            } else if (rosStatus == CONN_ROS_SERVER_ERROR) {
                callback.onFailure();
            }
        }
    }

}
