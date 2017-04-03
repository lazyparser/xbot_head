package cn.ac.iscas.xlab.droidfacedog;

import android.os.HandlerThread;
import android.util.Log;

import com.jilk.ros.rosbridge.ROSBridgeClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by lazyparser on 4/3/17.
 */

public final class RosBridgeCommunicateThread<T> extends HandlerThread {

    private static final String TAG = "RosThread";
    public static final int DEFAULT_FREQ;
    public static final int DEFAULT_DELAY;

    static {
        DEFAULT_DELAY = 1000; // 1000ms = delay 1s
        DEFAULT_FREQ = 100; // 100ms = 10Hz
    }

    private boolean mSpeakerDone;
    private Timer timer;
    private ROSBridgeClient mRosClient;

    public RosBridgeCommunicateThread(ROSBridgeClient rosClient) {
        super(TAG);
        timer = null;
        mSpeakerDone = false;
        mRosClient = rosClient;
    }

    public boolean publishTopic(String topic, String init, int freq) {
        // TODO: Stub
        return true;
    }

    public boolean publishTopic(final String topic, final boolean init, int freq) {
        // TODO: stub

        return true;
    }

    public boolean beginPublishTopicSpeakerDone() {
        final String topic = "/speaker_done";
        // Spec: Advertise before publish
        // https://github.com/RobotWebTools/rosbridge_suite/blob/groovy-devel/ROSBRIDGE_PROTOCOL.md
        //        { "op": "advertise",
        //                (optional) "id": <string>,
        //                "topic": <string>,
        //                "type": <string>
        //        }

        // unadvertise the topic for debug convenience.
        final String msgUnadvertise = "{\"op\": \"unadvertise\", \"topic\": \"" +
                topic + "\"}";
        mRosClient.send(msgUnadvertise);
        Log.d(TAG, "ROS SEND " + msgUnadvertise);

        try {
            // sleep 3 sec for server delay.
            sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        final String msgAdvertise = "{\"op\": \"advertise\", \"topic\": \"" +
                topic + "\", \"type\": \"std_msgs/Bool\"}";
        mRosClient.send(msgAdvertise);
        Log.d(TAG, "ROS SEND " + msgAdvertise);

        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                // TODO: spent tooooooo much time on this string. Should be more efficient.
                String msg = "{\"op\":\"publish\"," +
                        "\"topic\":\"" + topic + "\"," +
                        "\"msg\":{\"data\":" +
                        (mSpeakerDone?"true":"false") +
                        "}}";

                mRosClient.send(msg);
                Log.w(TAG,"ros send json: " + msg);
            }
        };

        if (timer != null)
            stopPublishTopicSpeakerDone();
        timer = new Timer();
        timer.schedule(timerTask, DEFAULT_DELAY, DEFAULT_FREQ);
        return true;
    }

    public void stopPublishTopicSpeakerDone() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    public void updateSpeakerState(boolean state) {
        mSpeakerDone = state;
    }
}
