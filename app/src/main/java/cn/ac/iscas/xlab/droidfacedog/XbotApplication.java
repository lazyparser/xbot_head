package cn.ac.iscas.xlab.droidfacedog;

import android.app.Application;
import android.util.Log;

import com.jilk.ros.rosbridge.ROSBridgeClient;

/**
 * Created by lazyparser on 4/3/17.
 */

public class XbotApplication extends Application {
    private static final String TAG = "XBotApplication";
    ROSBridgeClient rosClient;

    public RosBridgeCommunicateThread<PublishEvent> getRosThread() {
        return mRosThread;
    }

    public void setRosThread(RosBridgeCommunicateThread<PublishEvent> rosThread) {
        mRosThread = rosThread;
    }

    RosBridgeCommunicateThread<PublishEvent> mRosThread;

    public XbotApplication() {
        Log.d(TAG, "XbotApplication Instance has been created.");
    }

    @Override
    public void onTerminate() {
        if(rosClient != null)
            rosClient.disconnect();
        Log.i(TAG, "ROSBridge client destroyed");
        if (mRosThread != null)
            mRosThread.quit();
        Log.i(TAG, "Background RosBridgeCommunicateThread thread destroyed");
        super.onTerminate();
    }

    public ROSBridgeClient getRosClient() {
        return rosClient;
    }

    public void setRosClient(ROSBridgeClient client) {
        this.rosClient = client;
    }
}
