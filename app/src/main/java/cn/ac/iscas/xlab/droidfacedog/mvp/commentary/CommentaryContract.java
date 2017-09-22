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

    interface Presenter extends BasePresenter {

        void recognize(Bitmap bitmap);

        void releaseMemory();

        void setServiceProxy(@NonNull Binder binder);

    }

    interface View extends BaseView<Presenter> {

        void startCamera();

        void closeCamera();

        void displayInfo(String str);
    }

}
