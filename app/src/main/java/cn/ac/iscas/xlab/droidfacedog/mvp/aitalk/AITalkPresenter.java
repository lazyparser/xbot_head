package cn.ac.iscas.xlab.droidfacedog.mvp.aitalk;

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

import cn.ac.iscas.xlab.droidfacedog.entity.AIMessage;

/**
 * Created by lisongting on 2017/7/31.
 */

public class AITalkPresenter implements AITalkContract.Presenter {

    public static final String TAG = "AITalkPresenter";
    //创建AIUIAgent所需的初始化参数
    public static final String AIAGENT_PARAMS = "{\"interact\":{\"interact_timeout\":\"60000\",\"result_timeout\":\"5000\"}," +
            "\"global\":{\"scene\":\"main\",\"clean_dialog_history\":\"auto\"}," +
            "\"vad\":{\"vad_enable\":\"1\",\"engine_type\":\"meta\",\"res_type\":\"assets\",\"res_path\":\"vad/meta_vad_16k.jet\"}," +
            "\"iat\":{\"sample_rate\":\"16000\"}," +
            "\"speech\":{\"data_source\":\"sdk\"}}";

    private AITalkContract.View view;
    private Context context;
    private long lastWakeUpTime;

    private AIUIAgent agent;
    private AIUIListener aiuiListener;
    private SpeechSynthesizer ttsSynthesizer;
    private SynthesizerListener synthesizerListener;
    private Timer timer;

    private int mAIUIState = AIUIConstant.STATE_IDLE;

    //构造器
    public AITalkPresenter(Context context,AITalkContract.View view) {
        this.context = context;
        this.view = view;
        view.setPresenter(this);
    }

    //初始化
    @Override
    public void start() {
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
                                AIMessage.IntentBean.AnswerBean answerBean = aiMessage.getIntent().getAnswer();
                                String str = "";
//                                    str = answerBean.getText();
                            if (answerBean != null) {
                                str = answerBean.getText();
                                speakOutResult(str);

                            } else {
//                                    str = "这个问题太难了，换一个吧";
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
                startAiTalk();
            }

            @Override
            public void onEvent(int i, int i1, int i2, Bundle bundle) {

            }
        };
        agent = AIUIAgent.createAgent(context, AIAGENT_PARAMS, aiuiListener);


    }

    private void startAutoCloseTask() {
        timer = new Timer();
        final Handler h = new Handler(Looper.getMainLooper());
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastWakeUpTime>60000) {
                    log("60 seconds passed,agent closed");
                    h.post(new Runnable() {
                        @Override
                        public void run() {
                            view.stopAnim();
                        }
                    });
                    this.cancel();
                }else{
//                    log("agent is awake");
                }
            }
        },10000,10000);//延迟10秒启动，每10秒查询一次状态

    }

    //使用TTS引擎将语音播放出来
    private void speakOutResult(String str) {
        log("Speak Out:" + str);
        ttsSynthesizer.startSpeaking(str,synthesizerListener );
        view.showResultInTextView(str);
    }

    @Override
    public void startAiTalk() {
        //唤醒AIUIAgent
        AIUIMessage wakeupMsg = new AIUIMessage(AIUIConstant.CMD_WAKEUP, 0, 0, "", null);
        agent.sendMessage(wakeupMsg);

        //开始录音
        String param = "sample_rate=16000,data_type=audio";
        AIUIMessage audioMessage = new AIUIMessage(AIUIConstant.CMD_START_RECORD, 0, 0, param, null);
        agent.sendMessage(audioMessage);

        lastWakeUpTime = System.currentTimeMillis();
        startAutoCloseTask();
    }

    @Override
    public void stopAiTalk() {
        // 停止录音
        String params = "sample_rate=16000,data_type=audio";
        AIUIMessage stopWriteMsg = new AIUIMessage(AIUIConstant.CMD_STOP_RECORD, 0, 0, params, null);

        agent.sendMessage(stopWriteMsg);
        timer.cancel();
    }

    @Override
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
