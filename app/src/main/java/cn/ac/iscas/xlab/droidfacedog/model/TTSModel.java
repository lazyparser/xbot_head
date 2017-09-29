package cn.ac.iscas.xlab.droidfacedog.model;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SynthesizerListener;
import com.iflytek.cloud.util.ResourceUtil;

import java.io.File;

import cn.ac.iscas.xlab.droidfacedog.util.Util;

/**
 * Created by lisongting on 2017/8/8.
 */

public class TTSModel {
    public static final String TAG = "TTSModel";
    private Context context;
    private OnTTSFinishListener onTTSFinishListener;
    //语音合成器
    private SpeechSynthesizer ttsSynthesizer;
    private SynthesizerListener synthesizerListener;
    private String speaker = "vinn";

    public interface OnTTSFinishListener {
        void onTTSFinish(SpeechError speechError);
    }

    public TTSModel(Context context) {
        this.context = context;
        init();
    }

    public TTSModel(Context context, String speaker) {
        this.speaker = speaker;
        this.context = context;
        init();
    }

    private void init() {
        //创建 SpeechSynthesizer 对象, 第二个参数：本地合成时传 InitListener，可以为Null
        ttsSynthesizer = SpeechSynthesizer.createSynthesizer(context, null);

        //xiaoyan：青年女声-普通话  xiaoyu：青年男声-普通话   vixx：小男孩-普通话    vinn：小女孩-普通话
        ttsSynthesizer.setParameter(SpeechConstant.VOICE_NAME, speaker); //设置发音人
        ttsSynthesizer.setParameter(SpeechConstant.SPEED, "50");//设置语速
        ttsSynthesizer.setParameter(SpeechConstant.VOLUME, "80");//设置音量，范围 0~100
        ttsSynthesizer.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD); //设置云端

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
                if (onTTSFinishListener != null) {
                    onTTSFinishListener.onTTSFinish(speechError);
                }
            }

            @Override
            public void onEvent(int i, int i1, int i2, Bundle bundle) {

            }
        };


    }

    //userId是优图服务器返回的16进制字符串
    public void speakUserName(String userId,OnTTSFinishListener listener) {
        this.onTTSFinishListener = listener;

        StringBuilder text = new StringBuilder();
        text.append("你好，");

        if (userId.length()==0) {
            text.append("游客。");
        } else {
            String name = Util.hexStringToString(userId);
            text.append(name+"。");
        }

        text.append("我是语音机器人，你有什么想对我说的吗");
        //设置声音文件的缓存。仅支持保存为 pcm 和 wav 格式
        String cacheFileName = context.getExternalCacheDir() + "/" + userId + ".pcm";
        //如果本地已经有离线缓存，则直接播放离线缓存文件
        if (isCacheExist(cacheFileName)) {
            ttsSynthesizer.setParameter(ResourceUtil.TTS_RES_PATH, cacheFileName);
            ttsSynthesizer.startSpeaking(text.toString(),synthesizerListener);
            Log.i(TAG, "播放离线缓存文件");
        } else {
            //如果本地没有缓存，则播放在线数据的同时缓存到本地
            ttsSynthesizer.setParameter(SpeechConstant.TTS_AUDIO_PATH, cacheFileName);
            //开始播放
            ttsSynthesizer.startSpeaking(text.toString(),synthesizerListener );
            Log.i(TAG, "离线文件不存在,在线播放");
        }


        Log.i(TAG, "greetToUser:" + text.toString());
    }

    public boolean isCacheExist(String cacheFileName) {
        File f = new File(cacheFileName);
        if (f.exists()) {
            return true;
        } else {
            return false;
        }
    }

    public void textToSpeech(String text,OnTTSFinishListener listener) {
        this.onTTSFinishListener = listener;
        ttsSynthesizer.startSpeaking(text,synthesizerListener );
    }

    public void releaseMemory() {
        ttsSynthesizer.stopSpeaking();
        ttsSynthesizer.destroy();
    }


}
