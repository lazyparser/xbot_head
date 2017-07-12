package cn.ac.iscas.xlab.droidfacedog.mvp.userlist;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;

import java.util.List;

import cn.ac.iscas.xlab.droidfacedog.R;
import cn.ac.iscas.xlab.droidfacedog.UserListAdapter;

/**
 * Created by lisongting on 2017/7/11.
 * 采用MVP架构
 */

public class UserListActivity extends AppCompatActivity implements UserListContract.View{

    private UserListContract.Presenter presenter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private UserListAdapter userListAdapter;
    private List<Bitmap> bitmapList;
    private List<String> nameList;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_list);
        initView();
    }


    @Override
    public void initView() {
        recyclerView = (RecyclerView) findViewById(R.id.recyclerview_user_list);
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {

                nameList = presenter.requestUsersName();
                bitmapList  = presenter.requestUsersBitmap();
                if (nameList.size() != 0 && bitmapList.size() != 0) {
                    //展示用户列表
                    showUserList();
                    swipeRefreshLayout.setRefreshing(false);
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        presenter.start();
        swipeRefreshLayout.setRefreshing(true);
    }

    @Override
    public void setPresenter(UserListContract.Presenter presenter) {
        this.presenter = presenter;
    }


    @Override
    public void showUserList() {

        userListAdapter = new UserListAdapter(this, bitmapList, nameList);

        recyclerView.setAdapter(userListAdapter);

    }
}
