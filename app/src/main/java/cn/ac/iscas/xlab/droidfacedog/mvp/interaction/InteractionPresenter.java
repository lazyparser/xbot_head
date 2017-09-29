package cn.ac.iscas.xlab.droidfacedog.mvp.interaction;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Binder;
import android.support.annotation.NonNull;
import android.util.Log;

import com.iflytek.cloud.SpeechError;

import cn.ac.iscas.xlab.droidfacedog.RosConnectionService;
import cn.ac.iscas.xlab.droidfacedog.entity.AudioStatus;
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
    private boolean hasGreeted ;
    private YoutuConnection youtuConnection;

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
        ttsModel = new TTSModel(mContext);

        hasGreeted = false;

        youtuConnection = new YoutuConnection(mContext);

        EventBus.getDefault().register(this);

    }

    @Override
    public void greetToUser(String userId) {
        view.stopFaceDetectTask();
        view.stopCamera();
        view.showRobotImg();

        ttsModel.speakUserName(userId, new TTSModel.OnTTSFinishListener() {
            @Override
            public void onTTSFinish(SpeechError speechError) {
                if (speechError != null) {
                    log(speechError.getErrorCode() + ":" + speechError.getErrorDescription());
                }
                view.startAnimation();
                view.setWaveViewEnable(true);
                view.setCommentaryButtonEnable(true);
                //当进入AI对话模式之后，停止一系列与照相头相关的工作
                startAiTalk();
            }
        });


    }

    @Override
    public void startCommentary() {
        if (audioManager.isPlaying()) {
            view.showTip("当前正在解说");
        } else {
            //给出提示，当进入解说模式的时候，会关闭AI对话功能
            view.showTip("Tip:在解说模式，Ai语音对话功能将被关闭");
        }
        final int audioId  = 0;
        audioManager.playAsync(audioId, new AudioManager.AudioCompletionCallback() {
            @Override
            public void onComplete(int id) {
                rosProxy.publishAudioStatus(new AudioStatus(audioId,true));
            }
        });

    }

    @Override
    public void stopCommentary() {
        audioManager.pause();
    }

    @Override
    public void startAiTalk() {
        aiTalkModel.startAiTalk(new AiTalkModel.OnAiTalkerResult() {
            @Override
            public void onAiTalkerSpeak(String words) {
                //如果用户说了“带我参观博物馆”，AI机器人会回答"好的，接下来开始播放解说词"
                if (words.equals("好的，接下来开始播放解说词")) {
                    log("startAiTalk()");
                    startCommentary();
                    stopAiTalk();
                    view.setWaveViewEnable(false);
                }
            }
        }, new AiTalkModel.OnAiTalkerTimeout() {
            @Override
            public void onTimeOut() {
                view.stopAnimation();
                stopAiTalk();
            }
        });
    }

    @Override
    public void stopAiTalk() {
        aiTalkModel.stopAiTalk();

    }

    @Override
    public void releaseMemory() {
        audioManager.pause();
        audioManager.releaseMemory();
        ttsModel.releaseMemory();

    }

    //Service只能由Activity进行绑定，这里由外部传入
    @Override
    public void setServiceProxy(@NonNull Binder binder) {
        this.rosProxy = (RosConnectionService.ServiceBinder) binder;
    }

    @Override
    public void recognizeUserFace(Bitmap bitmap) {
        youtuConnection.recognizeFace(bitmap, new YoutuConnection.RecognitionCallback() {
            @Override
            public void onResponse(String personId) {
                //当识别人脸成功后，说出问候语，当问候语说完后，进入AI对话模式
                if (!hasGreeted) {
                    greetToUser(personId);
                    hasGreeted = true;
                }

            }

            @Override
            public void onFailure(String errorInfo) {
                //识别服务器连接超时
                if (!hasGreeted) {
                    greetToUser("");
                    hasGreeted = true;
                }
            }
        });
    }

    //EventBus的回调，用来接收从Service中发回来的机器人状态
    public void onEvent(RobotStatus status) {
        final int locationId = status.getLocationId();
//        boolean isMoving = status.isMoving();
        Log.i(TAG, "接收到来自Ros服务器的RobotStatus:"+status.toString());
        //如果到达了新的位置，并且audioManager并没有在播放音频，则开始播放指定id的音频
        //如果到达了指定位置而前面一段解说词没有播放完，则会等待其播放完
        if (locationId != audioManager.getCurrentId() && !audioManager.isPlaying()) {
            audioManager.playAsync(locationId, new AudioManager.AudioCompletionCallback() {
                @Override
                public void onComplete(int id) {
                    rosProxy.publishAudioStatus(new AudioStatus(id,true));
                }
            });
        }
    }

    public void log(String string) {
        Log.i(TAG, TAG + " -- " + string);
    }

}
