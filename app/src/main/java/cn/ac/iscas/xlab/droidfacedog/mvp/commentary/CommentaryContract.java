package cn.ac.iscas.xlab.droidfacedog.mvp.commentary;

import android.graphics.Bitmap;
import android.os.Binder;
import android.support.annotation.NonNull;

import cn.ac.iscas.xlab.droidfacedog.mvp.BasePresenter;
import cn.ac.iscas.xlab.droidfacedog.mvp.BaseView;

/**
 * Created by lisongting on 2017/9/22.
 */

public class CommentaryContract {

    public static final int STATE_IDLE = 0;//待机状态
    public static final int STATE_DETECTED = 1;//人脸检测完毕
    public static final int STATE_IDENTIFIED = 2;//人脸识别成功

    interface Presenter extends BasePresenter {

        void recognize(Bitmap bitmap);

        void releaseMemory();

        void setServiceProxy(@NonNull Binder binder);

    }

    interface View extends BaseView<Presenter> {

        void startCamera();

        void closeCamera();

        void displayInfo(String str);

        void changeUiState(int state);
    }

}
