package cn.ac.iscas.xlab.droidfacedog.mvp.interaction;

import android.content.Context;
import android.graphics.Bitmap;

import cn.ac.iscas.xlab.droidfacedog.model.AiTalkModel;

/**
 * Created by lisongting on 2017/8/7.
 */

public class InteractionPresenter implements InteractionContract.Presenter {

    public static final String TAG = "InteractionPresenter";
    private Context mContext;
    private AiTalkModel aiTalkModel;
    private InteractionContract.View view;


    public InteractionPresenter(Context context) {
        this.mContext = context;
    }

    @Override
    public void start() {
        aiTalkModel = AiTalkModel.getInstance(mContext);

    }

    @Override
    public void greetToUser(String userName) {

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

    @Override
    public String recognizeUserFace(Bitmap bitmap) {
        return null;
    }
}
