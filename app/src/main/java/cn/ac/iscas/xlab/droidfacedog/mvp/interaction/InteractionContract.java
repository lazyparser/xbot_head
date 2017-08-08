package cn.ac.iscas.xlab.droidfacedog.mvp.interaction;

import android.graphics.Bitmap;
import android.os.Binder;
import android.support.annotation.NonNull;

import cn.ac.iscas.xlab.droidfacedog.mvp.BasePresenter;
import cn.ac.iscas.xlab.droidfacedog.mvp.BaseView;

/**
 * Created by lisongting on 2017/8/7.
 */

public interface InteractionContract {

    interface Presenter extends BasePresenter{

        //说出用户名字，进行问候
        void greetToUser(String userId);

        //开始解说
        void startCommentary();

        void stopCommentary();

        //开启AI对话模式
        void startAiTalk();

        //关闭AI对话模式
        void stopAiTalk();

        //释放资源
        void releaseMemory();

        //进行人脸识别
        String recognizeUserFace(Bitmap bitmap);

        void setServiceProxy(@NonNull Binder binder);
    }


    interface View extends BaseView<Presenter>{

        void startAnimation();

        void stopAnimation();

        void startCamera();

        void stopCamera();

    }

}
