package cn.ac.iscas.xlab.droidfacedog.mvp.interaction;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Binder;
import android.support.annotation.NonNull;
import android.util.Log;

import com.iflytek.cloud.SpeechError;

import cn.ac.iscas.xlab.droidfacedog.RosConnectionService;
import cn.ac.iscas.xlab.droidfacedog.entity.RobotStatus;
import cn.ac.iscas.xlab.droidfacedog.model.AiTalkModel;
import cn.ac.iscas.xlab.droidfacedog.model.AudioManager;
import cn.ac.iscas.xlab.droidfacedog.model.TTSModel;
import cn.ac.iscas.xlab.droidfacedog.network.YoutuConnection;
import de.greenrobot.event.EventBus;

/**
 * Created by lisongting on 2017/8/7.
 */

public class InteractionPresenter implements InteractionContract.Presenter {

    public static final String TAG = "InteractionPresenter";
    private Context mContext;
    private AiTalkModel aiTalkModel;
    private TTSModel ttsModel;
    private InteractionContract.View view;
    private AudioManager audioManager;

    //用来发布tts_status和操控RosConnectionService
    private RosConnectionService.ServiceBinder rosProxy;

    private OnFaceRecognitionResult callback;


    private YoutuConnection youtuConnection;
    public interface OnFaceRecognitionResult{
        void onResult(String userId);
    }

    public InteractionPresenter(InteractionContract.View view,Context context) {
        this.view = view;
        this.mContext = context;
        this.view.setPresenter(this);
    }

    @Override
    public void start() {
        aiTalkModel = AiTalkModel.getInstance(mContext);
        audioManager = new AudioManager(mContext);
        audioManager.loadTts();
        ttsModel = TTSModel.getInstance(mContext);

        EventBus.getDefault().register(this);

    }

    @Override
    public void greetToUser(String userId) {

        if (!audioManager.isPlaying()) {
            //当用户问候完之后，进入AI对话模式
            ttsModel.speakUserName(userId, new TTSModel.OnTTSFinishListener() {
                @Override
                public void onTTSFinish(SpeechError speechError) {

                }
            });
        } else {
            return;
        }

    }

    @Override
    public void startCommentary() {

    }

    @Override
    public void stopCommentary() {

    }

    @Override
    public void startAiTalk() {
        aiTalkModel.startAiTalk(new AiTalkModel.OnAiTalkerTimeout() {
            @Override
            public void onTimeOut() {
                view.stopAnimation();
            }
        });
    }

    @Override
    public void stopAiTalk() {
        aiTalkModel.stopAiTalk();

    }

    @Override
    public void releaseMemory() {


    }

    //Service只能由Activity进行绑定，这里由外部传入
    @Override
    public void setServiceProxy(@NonNull Binder binder) {
        this.rosProxy = (RosConnectionService.ServiceBinder) binder;
    }

    @Override
    public String recognizeUserFace(Bitmap bitmap) {
        return null;
    }

    //EventBus的回调，用来接收从Service中发回来的机器人状态
    public void onEvent(RobotStatus status) {
        int locationId = status.getLocationId();
//        boolean isMoving = status.isMoving();
        Log.i(TAG, "OnRobotStatus:"+status.toString());
        //如果到达了新的位置，并且mAudioManager并没有在播放音频，则开始播放指定id的音频
        if (locationId != audioManager.getCurrentId() && !audioManager.isPlaying()) {
            audioManager.play(locationId);
        }
    }

    public void log(String string) {
        Log.i(TAG, TAG + " -- " + string);
    }

}
