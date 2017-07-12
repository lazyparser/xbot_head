package cn.ac.iscas.xlab.droidfacedog.mvp.userlist;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;

import java.util.ArrayList;
import java.util.List;

import cn.ac.iscas.xlab.droidfacedog.entity.UserInfo;
import cn.ac.iscas.xlab.droidfacedog.network.YoutuConnection;

/**
 * Created by lisongting on 2017/7/11.
 */

public class UserListPresenter implements UserListContract.Presenter {

    public static final String TAG = "UserListPresenter";
    private Context context;
    private UserListContract.View view;
    private YoutuConnection youtuConnection;
    private Handler handler;

    public UserListPresenter(Context context,UserListContract.View view) {
        this.context = context;
        this.view = view;
        handler = new Handler();
        //在这里就给View绑定了presenter
        view.setPresenter(this);

    }

    @Override
    public void start() {
        youtuConnection = new YoutuConnection(context, handler);

    }

    @Override
    public void requestUserData() {
        youtuConnection.getUserInfoList(new YoutuConnection.UserListCallback() {
            @Override
            public void onUserInfoReady(UserInfo userInfo) {
                view.showUserInList(userInfo);
            }

            @Override
            public void onBitmapReady(Bitmap bitmap) {

            }

            @Override
            public void onError() {
                view.showError();
            }

        });
    }

}
