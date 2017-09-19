package cn.ac.iscas.xlab.droidfacedog.mvp.facesign;

import android.graphics.Bitmap;
import android.os.Binder;
import android.support.annotation.NonNull;

import cn.ac.iscas.xlab.droidfacedog.mvp.BasePresenter;
import cn.ac.iscas.xlab.droidfacedog.mvp.BaseView;

/**
 * Created by lisongting on 2017/9/12.
 * 人脸打卡/签到功能
 */

public interface SignInContract {

    int UI_STATE_READY = 11;
    int UI_STATE_ON_THE_WAY = 22;

    interface Presenter extends BasePresenter{

        void speak(String str);

        void recognize(Bitmap bitmap);

        //释放资源
        void releaseMemory();

        void setServiceProxy(@NonNull Binder binder);
    }

    interface View extends BaseView<Presenter>{

        //开启摄像头的同时进行人脸识别
        void startCamera();


        void closeCamera();

        void displayInfo(String str);

        void changeUiState(int state);
    }
}
