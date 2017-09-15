package cn.ac.iscas.xlab.droidfacedog.mvp.facesign;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Binder;
import android.support.annotation.NonNull;
import android.util.Log;

import cn.ac.iscas.xlab.droidfacedog.RosConnectionService;
import cn.ac.iscas.xlab.droidfacedog.entity.RobotStatus;
import cn.ac.iscas.xlab.droidfacedog.entity.SignStatus;
import cn.ac.iscas.xlab.droidfacedog.model.AiTalkModel;
import cn.ac.iscas.xlab.droidfacedog.network.YoutuConnection;
import cn.ac.iscas.xlab.droidfacedog.util.Util;
import de.greenrobot.event.EventBus;

/**
 * Created by lisongting on 2017/9/12.
 */

public class SignInPresenter implements SignInContract.Presenter {

    public static final String TAG = "SignInPresenter";
    private SignInContract.View view;
    private Context context;
    private YoutuConnection youtuConnection;
    private AiTalkModel aiTalkModel;
    private int recogFailureCount = 0;

    private RosConnectionService.ServiceBinder rosProxy;

    public SignInPresenter(SignInContract.View view, Context context) {
        this.view = view;
        this.context = context;
        view.setPresenter(this);
    }

    @Override
    public void start() {
        youtuConnection = new YoutuConnection(context);
        aiTalkModel = AiTalkModel.getInstance(context);
        EventBus.getDefault().register(this);
    }

    @Override
    public void speak(String str) {
        aiTalkModel.speakOutResult(str);
    }

    @Override
    public void recognize(Bitmap bitmap, YoutuConnection.RecognitionCallback callback) {

        youtuConnection.recognizeFace(bitmap, new YoutuConnection.RecognitionCallback() {
            //如果识别成功，则
            @Override
            public void onResponse(String personId) {
                //personId不为0表示识别成功
                if (personId.length() != 0) {
                    SignStatus signStatus = new SignStatus(true, true);
                    if (rosProxy != null) {
                        rosProxy.publishSignStatus(signStatus);
                    }
                    StringBuilder sb =new StringBuilder("你好，");
                    sb.append(Util.hexStringToString(personId));

                    speak(sb.toString());
                    view.displayInfo("识别成功："+Util.hexStringToString(personId));
                    log("识别成功，用户id:" + personId);
                    view.closeCamera();
                }else{
                    recogFailureCount++;
                    if (recogFailureCount == 3) {
                        SignStatus signStatus = new SignStatus(true, false);
                        if (rosProxy != null) {
                            rosProxy.publishSignStatus(signStatus);
                        }
                        recogFailureCount =0;
                        view.displayInfo("识别失败,请检查人脸服务器设置或降低人脸检测阈值");
                        speak("识别失败,请检查人脸服务器设置或降低人脸检测阈值");
                        log("识别失败");
                    }
                }
            }

            @Override
            public void onFailure(String errorInfo) {
                //onFailure表示优图服务器连接失败
                log("人脸识别服务器连接超时或网络错误");
                view.displayInfo("人脸识别服务器连接超时或网络错误");
            }
        });
    }

    @Override
    public void releaseMemory() {
        aiTalkModel.releaseMemory();

        EventBus.getDefault().unregister(this);

    }

    @Override
    public void setServiceProxy(@NonNull Binder binder) {
        rosProxy = (RosConnectionService.ServiceBinder) binder;
    }

    //EventBus的回调，用来接收从Service中发回来的机器人状态
    public void onEvent(RobotStatus status) {
        int locationId = status.getLocationId();
        boolean isMoving = status.isMoving();

        log(status.toString());

        if (locationId > 0 && isMoving == false) {
            //如果到达了新的位置，则开启摄像头进行人脸识别

            speak("请进行人脸打卡");

            //开启摄像头进行人脸识别
            view.startCamera();

        }

    }

    public static void log(String str) {
        Log.i(TAG, str);
    }
}
