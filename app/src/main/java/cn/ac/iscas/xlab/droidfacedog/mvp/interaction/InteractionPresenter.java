package cn.ac.iscas.xlab.droidfacedog.mvp.interaction;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.Log;

import com.iflytek.cloud.SpeechError;

import java.util.Timer;

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
    public static final int HANDLER_PLAY_TTS = 0x13;

    private Context mContext;
    private AiTalkModel aiTalkModel;
    private TTSModel ttsModel;
    private InteractionContract.View view;

    private AudioManager audioManager;

    //用来发布tts_status和操控RosConnectionService
    private RosConnectionService.ServiceBinder rosProxy;
    private boolean hasGreeted ;
    private YoutuConnection youtuConnection;
    private Handler youtuHandler;

    private Timer publishTopicTimer;
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

        //该用来接收优图的识别结果
        youtuHandler = new Handler(){
            public void handleMessage(Message msg) {
                if (msg.what == HANDLER_PLAY_TTS) {
                    Bundle data = msg.getData();
                    String userId = (String) data.get("userId");
                    //当识别人脸成功后，说出问候语，当问候语说完后，进入AI对话模式
                    if (!hasGreeted) {
                        greetToUser(userId);
                        hasGreeted = true;
                    }
                }
            }
        };


        youtuConnection = new YoutuConnection(mContext,youtuHandler);

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
        audioManager.play(0);
//        publishTtsStatus();




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
//                    youtuHandler.post(new Runnable() {
//                        @Override
//                        public void run() {
//                            view.setWaveViewEnable(false);
//
//                        }
//                    });
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
        youtuConnection.sendBitmap(bitmap);
    }

//    public void publishTtsStatus(){
//        publishTopicTimer = new Timer();
//        publishTopicTimer.schedule(new TimerTask() {
//            @Override
//            public void run() {
//                if (rosProxy != null && audioManager !=null) {
//                    int id = audioManager.getCurrentId();
//                    boolean isPlaying = audioManager.isPlaying();
//                    AudioStatus status = new AudioStatus(id,isPlaying);
//                    rosProxy.publishTtsStatus(status);
//                }
//            }
//        },1000,200);
//    }
        
    
    //EventBus的回调，用来接收从Service中发回来的机器人状态
    public void onEvent(RobotStatus status) {
        int locationId = status.getLocationId();
//        boolean isMoving = status.isMoving();
        Log.i(TAG, "接收到来自Ros服务器的RobotStatus:"+status.toString());
        //如果到达了新的位置，并且audioManager并没有在播放音频，则开始播放指定id的音频
        //如果到达了指定位置而前面一段解说词没有播放完，则会等待其播放完
        if (locationId != audioManager.getCurrentId() && !audioManager.isPlaying()) {
            audioManager.play(locationId);
        }
    }

    public void log(String string) {
        Log.i(TAG, TAG + " -- " + string);
    }

}
