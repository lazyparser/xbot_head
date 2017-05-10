package cn.ac.iscas.xlab.droidfacedog;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import cn.ac.iscas.xlab.droidfacedog.config.Config;

/**
 * Created by Nguyen on 5/20/2016.
 */

public class MainActivity extends AppCompatActivity {

    public static final String TAG = MainActivity.class.getSimpleName();

    private static final int RC_HANDLE_CAMERA_PERM_RGB = 1;

    private Context mContext;
    Button btnCameraRGB;
    Button btnXbotFace;
    Button btnRegisterVIP;
    Button btnSetting;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;

        btnCameraRGB = (Button) findViewById(R.id.button_detect);
        btnCameraRGB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int rc = ActivityCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA);
                if (rc == PackageManager.PERMISSION_GRANTED) {
                    Intent intent = new Intent(mContext, FaceDetectRGBActivity.class);
                    startActivity(intent);
                } else {
                    requestCameraPermission(RC_HANDLE_CAMERA_PERM_RGB);
                }
            }
        });

        btnXbotFace = (Button) findViewById(R.id.button_xbotface);
        btnXbotFace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int rc = ActivityCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA);
                if (rc == PackageManager.PERMISSION_GRANTED) {
                    Intent intent = new Intent(mContext, XBotFace.class);
                    startActivity(intent);
                } else {
                    requestCameraPermission(RC_HANDLE_CAMERA_PERM_RGB);
                }
            }
        });

//        btnRegisterVIP = (Button) findViewById(R.id.button_detect);
//        btnRegisterVIP.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                int rc = ActivityCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA);
//                if (rc == PackageManager.PERMISSION_GRANTED) {
//                    Intent intent = new Intent(mContext, btnRegisterVIP.class);
//                    startActivity(intent);
//                } else {
//                    requestCameraPermission(RC_HANDLE_CAMERA_PERM_RGB);
//                }
//            }
//        });

        btnSetting = (Button) findViewById(R.id.button_config);
        btnSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    Intent intent = new Intent(mContext, SettingsActivity.class);
                    // http://stackoverflow.com/questions/15172111/preferenceactivity-actionbar-home-icon-wont-return-home-unlike-et
                    startActivityForResult(intent, 1);
            }
        });

        initConfiguration();

    }

    //在该函数中，获取settings中设置的所有属性值，并将相关的值存放在Config中
    public void initConfiguration() {
        Resources res = getResources();

        SharedPreferences sharedPreference = PreferenceManager.getDefaultSharedPreferences(this);

        String strThreshold = sharedPreference.getString(res.getString(R.string.key_recog_threshold), "0.6");

        Config.RECOG_THRESHOLD = Double.parseDouble(strThreshold);

        Config.ENABLE_MESSAGE_NOTIFICATION = sharedPreference.getBoolean(res.getString(R.string.key_enable_notification),true);

        Config.ROS_SERVER_IP = sharedPreference.getString(res.getString(R.string.key_ros_server_ip), "192.168.1.111");

        Config.RECOGNITION_SERVER_IP = sharedPreference.getString(res.getString(R.string.key_recognition_server_ip), "192.168.0.111");

        Log.i("tag", "MainActivity启动时初始化：" + Config.string());
    }

    private void requestCameraPermission(final int RC_HANDLE_CAMERA_PERM) {
        Log.w(TAG, "Camera permission is not granted. Requesting permission");

        final String[] permissions = new String[]{Manifest.permission.CAMERA};

        ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED && requestCode == RC_HANDLE_CAMERA_PERM_RGB) {
            Intent intent = new Intent(mContext, FaceDetectRGBActivity.class);
            startActivity(intent);
            return;
        }


        Log.e(TAG, "Permission not granted: results len = " + grantResults.length +
                " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));
    }

}
