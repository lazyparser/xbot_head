package cn.ac.iscas.xlab.droidfacedog.mvp.userlist;

import android.graphics.Bitmap;

import java.util.List;

import cn.ac.iscas.xlab.droidfacedog.mvp.BasePresenter;
import cn.ac.iscas.xlab.droidfacedog.mvp.BaseView;

/**
 * Created by lisongting on 2017/7/11.
 * 采用MVP架构
 */

public interface UserListContract {

    interface View extends BaseView<Presenter> {

        void showUserList();


    }

    interface Presenter extends BasePresenter {

        List<Bitmap> requestUsersBitmap();

        List<String> requestUsersName();

    }

}
