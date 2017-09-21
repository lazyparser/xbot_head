package cn.ac.iscas.xlab.droidfacedog;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.jilk.ros.ROSClient;
import com.jilk.ros.rosbridge.ROSBridgeClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

import cn.ac.iscas.xlab.droidfacedog.config.Config;
import cn.ac.iscas.xlab.droidfacedog.entity.PublishEvent;
import cn.ac.iscas.xlab.droidfacedog.entity.RobotStatus;
import cn.ac.iscas.xlab.droidfacedog.entity.SignStatus;
import cn.ac.iscas.xlab.droidfacedog.entity.AudioStatus;
import de.greenrobot.event.EventBus;

/**
 * Created by lisongting on 2017/6/5.
 *
 */

//TODO:人脸签到场景,topic的发布和订阅
public class RosConnectionService extends Service{

    public static final String TAG = "RosConnectionService";
    public static final String SUBSCRIBE_TOPIC = "/robot_status";
    //解说词播放状态
    public static final String PUBLISH_TOPIC_AUDIO_STATUS = "/pad_audio_status";
    //签到状态
    public static final String PUBLISH_TOPIC_SIGN_COMPLETION = "/pad_sign_completion";

    public Binder proxy = new ServiceBinder();
    private ROSBridgeClient rosBridgeClient;

    private boolean isConnected = false;
    private Timer rosConnectionTimer;
    private TimerTask connectionTask;


    public class ServiceBinder extends Binder {
        public boolean isConnected(){
            return isConnected;
        }

//        public void publishTtsStatus(AudioStatus status) {
//
//            if (isConnected) {
//                JSONObject body = new JSONObject();
//                try {
//                    JSONObject jsonMsg = new JSONObject();
//                    jsonMsg.put("id", status.getId());
//                    jsonMsg.put("isplaying", status.isplaying());
//
//                    body.put("op", "publish");
//                    body.put("topic",PUBLISH_TOPIC_AUDIO_STATUS);
//                    body.put("msg", jsonMsg);
//
//                    rosBridgeClient.send(body.toString());
//
//                    Log.v(TAG, "publish 'tts_status' To Ros Server:" + body.toString());
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }
//            }
//        }

        public void publishSignStatus(SignStatus signStatus){
            JSONObject body = new JSONObject();
            if (isConnected) {
                JSONObject jsonMsg = new JSONObject();
                try {
                    jsonMsg.put("complete", signStatus.isComplete());
                    jsonMsg.put("success", signStatus.isSuccess());

                    body.put("op", "publish");
                    body.put("topic", PUBLISH_TOPIC_SIGN_COMPLETION);
                    body.put("msg", jsonMsg);
                    rosBridgeClient.send(body.toString());
                    Log.i(TAG, "publish 'pad_sign_completion' topic to Ros Server :" + body.toString());

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        public void publishAudioStatus(AudioStatus audioStatus) {
            JSONObject body = new JSONObject();
            if (isConnected()) {
                JSONObject jsonMsg = new JSONObject();
                try {
                    jsonMsg.put("id", audioStatus.getId());
                    jsonMsg.put("iscomplete", audioStatus.isComplete());

                    body.put("op", "publish");
                    body.put("topic", PUBLISH_TOPIC_AUDIO_STATUS);
                    body.put("msg", jsonMsg);
                    rosBridgeClient.send(body.toString());
                    Log.i(TAG, "publish 'pad_audio_status' topic to Ros Server :" + body.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

        }


        public void disConnect() {
            connectionTask.cancel();
            rosBridgeClient.disconnect();
//            onDestroy();
        }
    }

    //onCreate()会自动触发Ros服务器的连接
    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "RosConnService--onCreate()");
        rosConnectionTimer = new Timer();
        connectionTask = new TimerTask() {
            @Override
            public void run() {
                if (!isConnected) {
                    String rosURL = "ws://" + Config.ROS_SERVER_IP + ":" + Config.ROS_SERVER_PORT;
                    Log.v(TAG, "Connecting to ROS Server: " + rosURL);
                    rosBridgeClient = new ROSBridgeClient(rosURL);
                    boolean conneSucc = rosBridgeClient.connect(new ROSClient.ConnectionStatusListener() {
                        @Override
                        public void onConnect() {
                            rosBridgeClient.setDebug(true);
                            Log.i(TAG, "ConnectionStatusListener--onConnect");
                        }

                        @Override
                        public void onDisconnect(boolean normal, String reason, int code) {
                            Log.v(TAG, "ConnectionStatusListener--disconnect");
                        }

                        @Override
                        public void onError(Exception ex) {
                            ex.printStackTrace();
                            Log.i(TAG, "ConnectionStatusListener--ROS communication error");
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
                        Log.i(TAG, TAG+" -- 连接Ros Server成功");
                    } else {
                        Log.v(TAG, TAG+" -- 连接Ros Server失败");
                    }
                    isConnected = conneSucc;
                    Intent broadcastIntent = new Intent(MainActivity.ROS_RECEIVER_INTENTFILTER);
                    Bundle data = new Bundle();
                    if (!isConnected) {
                        data.putInt("ros_conn_status", MainActivity.CONN_ROS_SERVER_ERROR);
                        broadcastIntent.putExtras(data);
                        sendBroadcast(broadcastIntent);
                    } else{
                        data.putInt("ros_conn_status", MainActivity.CONN_ROS_SERVER_SUCCESS);
                        broadcastIntent.putExtras(data);
                        sendBroadcast(broadcastIntent);
                    }
                } else {
                    //如果连接成功则取消该定时任务
                    cancel();
                }
            }
        };

        startRosConnectionTimer();
        //注册Eventbus
        EventBus.getDefault().register(this);
    }

    @Override
    public void onRebind(Intent intent) {
        Log.i(TAG, TAG + " -- onRebind()");
        super.onRebind(intent);
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
        return proxy;
    }

    //订阅某个topic后，接收到Ros服务器返回的message，回调此方法
    public void onEvent(PublishEvent event) {
        //topic的名称
        String topicName = event.name;
        Log.i(TAG, "onEvent:" + event.msg);
        //Topic为RobotStatus
        if (topicName.equals(SUBSCRIBE_TOPIC)) {
            String msg = event.msg;
            JSONObject msgInfo;
            try {
                msgInfo = new JSONObject(msg);
                int id = msgInfo.getInt("id");
                boolean isMoving = msgInfo.getBoolean("ismoving");
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

    public void startRosConnectionTimer() {
        if (isConnected) {
            return;
        } else {
            rosConnectionTimer.schedule(connectionTask,0,3000);
        }
    }

}
