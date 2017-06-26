package cn.ac.iscas.xlab.droidfacedog;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.jilk.ros.ROSClient;
import com.jilk.ros.rosbridge.ROSBridgeClient;

import org.json.JSONException;
import org.json.JSONObject;

import cn.ac.iscas.xlab.droidfacedog.config.Config;
import cn.ac.iscas.xlab.droidfacedog.entity.PublishEvent;
import cn.ac.iscas.xlab.droidfacedog.entity.RobotStatus;
import cn.ac.iscas.xlab.droidfacedog.entity.TtsStatus;
import de.greenrobot.event.EventBus;

/**
 * Created by lisongting on 2017/6/5.
 */

public class RosConnectionService extends Service{

    public static final String TAG = "RosConnectionService";
    public static final String SUBSCRIBE_TOPIC = "/museum_position";
    public static final String PUBLISH_TOPIC = "/tts_status";
    public static final int CTRL_FORWARD = 0x11;
    public static final int CTRL_BACK = 0x12;
    public static final int CTRL_LEFT = 0x13;
    public static final int CTRL_RIGHT = 0x14;


    public Binder proxy = new ServiceBinder();
    private ROSBridgeClient rosBridgeClient;

    private boolean isConnected;

    public class ServiceBinder extends Binder {
        public boolean isConnected(){
            return isConnected;
        }

        public void publishTtsStatus(TtsStatus status) {

            if (isConnected) {
                JSONObject body = new JSONObject();
                try {
                    JSONObject jsonMsg = new JSONObject();
                    jsonMsg.put("id", status.getId());
                    jsonMsg.put("isplaying", status.isplaying());

                    body.put("op", "publish");
                    body.put("topic",PUBLISH_TOPIC);
                    body.put("msg", jsonMsg);

                    rosBridgeClient.send(body.toString());

                    Log.i(TAG, "Send To Ros Server:" + body.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        public void publishMoveTopic(int command) {

            //TODO:后续确定了topic之后，再进行具体实现
            switch (command) {
                case CTRL_FORWARD:
                    //test
                    Log.i(TAG, "点击了[前进]按钮");

                    break;
                case CTRL_BACK:
                    Log.i(TAG, "点击了[后退]按钮");

                    break;
                case CTRL_LEFT:
                    Log.i(TAG, "点击了[左转]按钮");

                    break;
                case CTRL_RIGHT:
                    Log.i(TAG, "点击了[右转]按钮");

                    break;
                default:
                    break;
            }

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

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "RosConnectionService onBind()");
        new Thread() {
            public void run() {
                String rosURL = "ws://" + Config.ROS_SERVER_IP + ":" + Config.ROS_SERVER_PORT;
                Log.d(TAG, "Connecting to ROS : " + rosURL);
                rosBridgeClient = new ROSBridgeClient(rosURL);
                Log.i(TAG, rosURL);
                boolean conneSucc = rosBridgeClient.connect(new ROSClient.ConnectionStatusListener() {
                    @Override
                    public void onConnect() {
                        rosBridgeClient.setDebug(true);
                        Log.i(TAG,"ConnectionStatusListener--onConnect");
                    }

                    @Override
                    public void onDisconnect(boolean normal, String reason, int code) {
                        Log.i(TAG,"ConnectionStatusListener--disconnect");
                    }

                    @Override
                    public void onError(Exception ex) {
                        ex.printStackTrace();
                        Log.i(TAG,"ConnectionStatusListener--ROS communication error");
                    }
                });
                if (conneSucc) {
                    // 订阅Ros即将发布的topic
                    JSONObject strSubscribe = new JSONObject();
                    try {
                        strSubscribe.put("op", "subscribe");
                        strSubscribe.put("topic", SUBSCRIBE_TOPIC);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    rosBridgeClient.send(strSubscribe.toString());
                    Log.i(TAG, "RosConnectionService连接Ros成功");
                } else {
                    Log.i(TAG, "RosConnectionService连接Ros失败");
                }
                isConnected = conneSucc;
                Log.i(TAG, "RosConnection Thread is Running in Thread:" + Thread.currentThread().getId());
            }
        }.start();
        return proxy;
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
}
