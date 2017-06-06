package cn.ac.iscas.xlab.droidfacedog;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;

import com.jilk.ros.ROSClient;
import com.jilk.ros.rosbridge.ROSBridgeClient;

import org.json.JSONException;
import org.json.JSONObject;

import cn.ac.iscas.xlab.droidfacedog.bean.PublishEvent;
import cn.ac.iscas.xlab.droidfacedog.bean.RobotStatus;
import de.greenrobot.event.EventBus;

/**
 * Created by lisongting on 2017/6/5.
 */

public class RosConnectionService extends Service{

    public static final String TAG = "RosConnectionService";
    public static final String SUBSCRIBE_TOPIC = "/museum_position";

    public Binder proxy = new ServiceBinder();
    private ROSBridgeClient rosBridgeClient;

    private boolean isConnected;

    public class ServiceBinder extends Binder {
        public boolean connect(){
            return isConnected;
        }

        public void publishTtsStatus() {

        }
        
    }

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
        Log.i(TAG, "onEvent:" + event.msg);
        if (topicName.equals(SUBSCRIBE_TOPIC)) {
            String msg = event.msg;
            JSONObject msgInfo;
            try {
                msgInfo = new JSONObject(msg);
                int id = msgInfo.getInt("id");
                boolean isMoving = msgInfo.getBoolean("ismoving");
                Log.i(TAG, "onEvent:" + event.msg);
                RobotStatus robotStatus = new RobotStatus(id, isMoving);
                EventBus.getDefault().post(robotStatus);

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
        Log.i("tag", "RosConnectionService onBind()");
        new Thread() {
            public void run() {
                String rosIP ;
                String rosPort = "9090";
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                rosIP = prefs.getString("rosserver_ip_address", "192.168.1.151");
                String rosURL = "ws://" + rosIP + ":" + rosPort;
                Log.d(TAG, "Connecting ROS " + rosURL);
                rosBridgeClient = new ROSBridgeClient(rosURL);
                Log.i("tag", rosURL);
                boolean conneSucc = rosBridgeClient.connect(new ROSClient.ConnectionStatusListener() {
                    @Override
                    public void onConnect() {
                        rosBridgeClient.setDebug(true);
//                        ((XbotApplication)getApplication()).setRosClient(rosBridgeClient);
                        Log.i("tag","ConnectionStatusListener--onConnect");
                    }

                    @Override
                    public void onDisconnect(boolean normal, String reason, int code) {
                        Log.i("tag","ConnectionStatusListener--disconnect");
                    }

                    @Override
                    public void onError(Exception ex) {
                        ex.printStackTrace();
                        Log.i("tag","ConnectionStatusListener--ROS communication error");
                    }
                });
                if (conneSucc) {
                    //订阅Ros即将发布的topic
//                    JSONObject strSubscribe = new JSONObject();
//                    try {
//                        strSubscribe.put("op", "subscribe");
//                        strSubscribe.put("topic", SUBSCRIBE_TOPIC);
//                    } catch (JSONException e) {
//                        e.printStackTrace();
//                    }
//                    rosBridgeClient.send(strSubscribe.toString());
                    Log.i("tag", "RosConnectionService连接Ros成功");
                } else {
                    Log.i("tag", "RosConnectionService连接Ros失败");
                }
                isConnected = conneSucc;
            }
        }.run();
        return proxy;
    }
}
