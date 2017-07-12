package cn.ac.iscas.xlab.droidfacedog.mvp.userlist;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;

import java.util.ArrayList;
import java.util.List;

import cn.ac.iscas.xlab.droidfacedog.network.YoutuConnection;

/**
 * Created by lisongting on 2017/7/11.
 */

public class UserListPresenter implements UserListContract.Presenter {

    private List<String> userIds ;
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
        userIds = new ArrayList<>();
        youtuConnection = new YoutuConnection(context, handler);

    }

    @Override
    public List<Bitmap> requestUsersBitmap() {
        if (userIds.size() <= 0) {
            return null;
        }
        List<Bitmap> bitmaps = new ArrayList<>();
        for (String s : userIds) {


        }
        return bitmaps;

    }

    @Override
    public List<String> requestUsersName() {
        List<String> names = new ArrayList<>();

        return names;
    }

}
