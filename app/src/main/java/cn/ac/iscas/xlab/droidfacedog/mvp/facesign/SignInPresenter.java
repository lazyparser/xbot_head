package cn.ac.iscas.xlab.droidfacedog.mvp.facesign;

import android.graphics.Bitmap;
import android.os.Binder;
import android.support.annotation.NonNull;

import cn.ac.iscas.xlab.droidfacedog.network.YoutuConnection;

/**
 * Created by lisongting on 2017/9/12.
 */

public class SignInPresenter implements SignInContract.Presenter {
    @Override
    public void start() {

    }

    @Override
    public void speek(String str) {

    }

    @Override
    public void recognize(Bitmap bitmap, YoutuConnection.RecognitionCallback callback) {

    }

    @Override
    public void releaseMemory() {

    }

    @Override
    public void setServiceProxy(@NonNull Binder binder) {

    }
}
