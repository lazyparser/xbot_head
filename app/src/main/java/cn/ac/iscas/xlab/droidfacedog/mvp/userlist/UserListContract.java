package cn.ac.iscas.xlab.droidfacedog.mvp.userlist;

import cn.ac.iscas.xlab.droidfacedog.entity.UserInfo;
import cn.ac.iscas.xlab.droidfacedog.mvp.BasePresenter;
import cn.ac.iscas.xlab.droidfacedog.mvp.BaseView;

/**
 * Created by lisongting on 2017/7/11.
 * 采用MVP架构
 */

public interface UserListContract {

    interface View extends BaseView<Presenter> {

        void showUserInList(UserInfo info);

        void showError();
    }

    interface Presenter extends BasePresenter {

        void requestUserData();
    }

}
