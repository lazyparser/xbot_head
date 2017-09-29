package cn.ac.iscas.xlab.droidfacedog;

import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.util.Log;

import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechUtility;
import com.taobao.sophix.PatchStatus;
import com.taobao.sophix.SophixManager;
import com.taobao.sophix.listener.PatchLoadStatusListener;

import cn.ac.iscas.xlab.droidfacedog.config.Config;

/**
 * Created by lisongting on 2017/7/10.
 */

public class XbotApplication extends Application {
    public static final String TAG = "XbotApplication";

    public ServiceConnection mServiceConnection;
    private RosConnectionService.ServiceBinder mServiceProxy;
    private Intent intent;

    public XbotApplication() {
        super();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "XbotApplication -- onCreate()");

        initSophix();

        //创建ServiceConnection对象
        mServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                Log.i(TAG, "mServiceConnection--onServiceConnected()");
                mServiceProxy = (RosConnectionService.ServiceBinder) service;
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.i(TAG, "mServiceConnection--onServiceDisconnected()");
            }
        };
        //绑定RosConnectionService
        intent = new Intent(this, RosConnectionService.class);
        //startService(intent);
        bindService(intent, mServiceConnection, BIND_AUTO_CREATE);
        //初始化讯飞TTS引擎
        SpeechUtility.createUtility(this, SpeechConstant.APPID +"="+ Config.APPID);

    }

    //初始化Sophix热修复组件
    private void initSophix() {
        String appVersion;
        try{
            //获取当前版本号
            appVersion = this.getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        }catch (PackageManager.NameNotFoundException e) {
            appVersion = "1.0.0.0";
        }

        SophixManager.getInstance().setContext(this)
                .setAppVersion(appVersion)
                .setAesKey(null)
                .setEnableDebug(true)
                .setPatchLoadStatusStub(new PatchLoadStatusListener() {
                    @Override
                    public void onLoad(final int mode, final int code, final String info, final int handlePatchVersion) {
                        // 补丁加载回调通知
                        if (code == PatchStatus.CODE_LOAD_SUCCESS) {
                            // 表明补丁加载成功
                            Log.d(TAG, "Sophix -- 补丁加载成功");
                        } else if (code == PatchStatus.CODE_LOAD_RELAUNCH) {
                            Log.d(TAG, "Sophix -- 补丁需要重启生效");
                            // 表明新补丁生效需要重启. 开发者可提示用户或者强制重启;
                            // 建议: 用户可以监听进入后台事件, 然后调用killProcessSafely自杀，以此加快应用补丁，详见1.3.2.3
                        } else {
                            Log.d(TAG, "Sophix -- 无更新补丁");
                            // 其它错误信息, 查看PatchStatus类说明
                        }
                    }
                }).initialize();
        //加载新的补丁包
        SophixManager.getInstance().queryAndLoadNewPatch();
    }

    @Override
    public void onTerminate() {
        Log.i(TAG, "XbotApplication -- onTerminate()");
        //stopService(intent);
        unbindService(mServiceConnection);
        super.onTerminate();
    }


    public RosConnectionService.ServiceBinder getServiceProxy() {
        if (mServiceProxy != null) {
            return mServiceProxy;
        }
        return null;
    }


}
