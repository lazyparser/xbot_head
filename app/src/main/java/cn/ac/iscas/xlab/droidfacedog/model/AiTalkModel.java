package cn.ac.iscas.xlab.droidfacedog.model;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;
import com.iflytek.aiui.AIUIAgent;
import com.iflytek.aiui.AIUIConstant;
import com.iflytek.aiui.AIUIEvent;
import com.iflytek.aiui.AIUIListener;
import com.iflytek.aiui.AIUIMessage;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SynthesizerListener;

import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

import cn.ac.iscas.xlab.droidfacedog.config.Config;
import cn.ac.iscas.xlab.droidfacedog.entity.AIMessage;

/**
 * Created by lisongting on 2017/8/7.
 * AI对话模块
 */

public class AiTalkModel {

    public static final String TAG = "AiTalkModel";


    private Context context;
    private long lastWakeUpTime;

    private AIUIAgent agent;
    private AIUIListener aiuiListener;
    //语音合成器
    private SpeechSynthesizer ttsSynthesizer;
    private SynthesizerListener synthesizerListener;
    private Timer timer;

    private int mAIUIState = AIUIConstant.STATE_IDLE;
    private TimerTask task;
    private boolean isRecording ;
    final Handler h = new Handler(Looper.getMainLooper());

    OnAiTalkerTimeout timeoutCallback;

    //ai对话超时回调接口。如果用户超过一定时间不说话，则自动关闭
    public interface OnAiTalkerTimeout{
        void onTimeOut ();
    }

    private AiTalkModel(Context context) {
        this.context = context;
        init();
    }

    public static AiTalkModel getInstance(Context context) {
        return new AiTalkModel(context);
    }

    private void init() {
        isRecording = false;
        //创建 SpeechSynthesizer 对象, 第二个参数：本地合成时传 InitListener，可以为Null
        ttsSynthesizer = SpeechSynthesizer.createSynthesizer(context, null);

        //xiaoyan：青年女声-普通话  xiaoyu：青年男声-普通话   vixx：小男孩-普通话    vinn：小女孩-普通话
        ttsSynthesizer.setParameter(SpeechConstant.VOICE_NAME, "vinn"); //设置发音人
        ttsSynthesizer.setParameter(SpeechConstant.SPEED, "50");//设置语速
        ttsSynthesizer.setParameter(SpeechConstant.VOLUME, "80");//设置音量，范围 0~100
        ttsSynthesizer.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD); //设置云端

        aiuiListener = new AIUIListener() {
            @Override
            public void onEvent(AIUIEvent event) {
                switch (event.eventType) {
                    case AIUIConstant.EVENT_WAKEUP:
                        log(  "on event: "+ event.eventType );
//                        log( "进入识别状态" );
                        break;

                    case AIUIConstant.EVENT_RESULT: {
                        log(  "on event: "+ event.eventType );
                        try {
                            JSONObject bizParamJson = new JSONObject(event.info);
                            JSONObject data = bizParamJson.getJSONArray("data").getJSONObject(0);
                            JSONObject params = data.getJSONObject("params");
                            JSONObject content = data.getJSONArray("content").getJSONObject(0);

                            if (content.has("cnt_id")) {
                                String cnt_id = content.getString("cnt_id");
                                byte[] byteArray = event.data.getByteArray(cnt_id);
                                if (byteArray != null) {
                                    JSONObject cntJson = new JSONObject(new String(byteArray, "utf-8"));

                                    AIMessage aiMessage;
                                    Gson gson = new Gson();
                                    aiMessage = gson.fromJson(cntJson.toString(), AIMessage.class);
                                    if (aiMessage.getIntent() != null) {
                                        AIMessage.IntentBean.AnswerBean answerBean = aiMessage.getIntent().getAnswer();
                                        String str = "";
                                        //str = answerBean.getText();
                                        if (answerBean != null) {
                                            str = answerBean.getText();
                                            speakOutResult(str);

                                        } else {
                                            //str = "这个问题太难了，换一个吧";
                                        }
                                    }
                                } else {
                                    log("cnt_id字段为null");
                                }

                            }
                        } catch (Throwable e) {
                            e.printStackTrace();
                            log( e.getLocalizedMessage() );
                        }
                    } break;

                    case AIUIConstant.EVENT_ERROR: {
                        log( "on event: "+ event.eventType );
                        log( "错误: "+event.arg1+"\n"+event.info );
                    } break;

                    case AIUIConstant.EVENT_START_RECORD: {
                        log( "on event: "+ event.eventType );
                        log("开始录音");
                    } break;

                    case AIUIConstant.EVENT_STOP_RECORD: {
                        log( "on event: "+ event.eventType );
                        log("停止录音");
                    } break;

                    case AIUIConstant.EVENT_STATE: {	// 状态事件
                        mAIUIState = event.arg1;

                        if (AIUIConstant.STATE_IDLE == mAIUIState) {
                            // 闲置状态，AIUI未开启
                            log("STATE_IDLE");
                        } else if (AIUIConstant.STATE_READY == mAIUIState) {
                            // AIUI已就绪，等待唤醒
                            log("STATE_READY");
                        } else if (AIUIConstant.STATE_WORKING == mAIUIState) {
                            // AIUI工作中，可进行交互
                            log("STATE_WORKING");
                        }
                    } break;

                    case AIUIConstant.EVENT_CMD_RETURN:{
                        if( AIUIConstant.CMD_UPLOAD_LEXICON == event.arg1 ){
                            log( "上传"+ (0==event.arg2?"成功":"失败") );
                        }
                    }break;

                    default:
                        break;
                }
            }
        };

        synthesizerListener = new SynthesizerListener() {
            @Override
            public void onSpeakBegin() {

            }

            @Override
            public void onBufferProgress(int i, int i1, int i2, String s) {

            }

            @Override
            public void onSpeakPaused() {

            }

            @Override
            public void onSpeakResumed() {

            }

            @Override
            public void onSpeakProgress(int i, int i1, int i2) {

            }
            //对话播放完后会停止录音，这里再次启动录音。
            @Override
            public void onCompleted(SpeechError speechError) {
                startAiTalk(timeoutCallback);
            }

            @Override
            public void onEvent(int i, int i1, int i2, Bundle bundle) {

            }
        };
        agent = AIUIAgent.createAgent(context, Config.AIAGENT_PARAMS, aiuiListener);

        timer = new Timer();
    }

    private void startAutoCloseTask() {

        //timerTask不能取消后再重用，只能使用一次便需要再次new
        task = new TimerTask() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastWakeUpTime>60000) {
                    log("60 seconds passed,agent closed");
                    h.post(new Runnable() {
                        @Override
                        public void run() {
                            if (timeoutCallback != null) {
                                timeoutCallback.onTimeOut();
                            }
                        }
                    });
                    isRecording = false;
                    this.cancel();
                }else{
                    isRecording = true;
//                    log("agent is awake");
                }
            }
        };
        if (!isRecording) {
            timer.schedule(task,10000,10000);//延迟10秒启动，每10秒查询一次状态
        }

    }

    //使用TTS引擎将语音播放出来
    private void speakOutResult(String str) {
        log("Speak Out:" + str);
        ttsSynthesizer.startSpeaking(str,synthesizerListener );
//        view.showResultInTextView(str);
    }

    public void startAiTalk(OnAiTalkerTimeout callback) {
        if (callback != null) {
            this.timeoutCallback = callback;
        }
        //唤醒AIUIAgent
        AIUIMessage wakeupMsg = new AIUIMessage(AIUIConstant.CMD_WAKEUP, 0, 0, "", null);
        agent.sendMessage(wakeupMsg);

        //开始录音
        String param = "sample_rate=16000,data_type=audio";
        AIUIMessage audioMessage = new AIUIMessage(AIUIConstant.CMD_START_RECORD, 0, 0, param, null);
        agent.sendMessage(audioMessage);

        lastWakeUpTime = System.currentTimeMillis();
        //启动超时检测
        startAutoCloseTask();
        isRecording = true;
    }

    public void stopAiTalk() {
        if (isRecording) {
            isRecording = false;
        }
        // 停止录音
        String params = "sample_rate=16000,data_type=audio";
        AIUIMessage stopWriteMsg = new AIUIMessage(AIUIConstant.CMD_STOP_RECORD, 0, 0, params, null);

        agent.sendMessage(stopWriteMsg);

    }

    public void releaseMemory() {
        //如果不等于，表示此时agent已经启动起来了
        if (mAIUIState != AIUIConstant.STATE_IDLE) {
            stopAiTalk();
        }

        if (ttsSynthesizer != null) {
            ttsSynthesizer.stopSpeaking();
            ttsSynthesizer.destroy();
        }
    }

    private void log(String str){
        Log.i(TAG, str);
    }


}
