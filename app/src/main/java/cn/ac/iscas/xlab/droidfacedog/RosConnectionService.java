package cn.ac.iscas.xlab.droidfacedog;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import cn.ac.iscas.xlab.droidfacedog.bean.PublishEvent;
import de.greenrobot.event.EventBus;

/**
 * Created by lisongting on 2017/6/5.
 */

public class RosConnectionService extends Service{

    public static final String TAG = "RosConnectionService";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "RosConnService--onCreate()");
        //注册Eventbus
        EventBus.getDefault().register(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "RosConnService--onStartCommand()");
        return super.onStartCommand(intent, flags, startId);

    }

    //订阅某个topic后，接收到Ros服务器返回的message，回调此方法
    public void onEvent(PublishEvent event) {
        //topic的名称
        String topicName = event.name;
        if (topicName.endsWith("/cmd_vel")) {
            String msg = event.msg;
            JSONObject msgInfo ;
            try {
                msgInfo = new JSONObject(msg);
                Log.i("tag", "onEvent:" + event.msg);
//                EventBus.getDefault().post(new RobotStatus(666,false));

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "RosConnService--onDestroy()");
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
