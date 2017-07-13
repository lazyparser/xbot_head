package cn.ac.iscas.xlab.droidfacedog.mvp.userlist;

import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.widget.Toast;

import cn.ac.iscas.xlab.droidfacedog.R;
import cn.ac.iscas.xlab.droidfacedog.UserListAdapter;
import cn.ac.iscas.xlab.droidfacedog.entity.UserInfo;

/**
 * Created by lisongting on 2017/7/11.
 * 采用MVP架构
 */

public class UserListActivity extends AppCompatActivity implements UserListContract.View{

    public static final String TAG = "UserListActivity";
    private UserListContract.Presenter presenter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private UserListAdapter userListAdapter;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_list);

        getSupportActionBar().setTitle("用户列表");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        presenter = new UserListPresenter(getBaseContext(),this);
        initView();
    }

    @Override
    public void initView() {
        recyclerView = (RecyclerView) findViewById(R.id.recyclerview_user_list);
        RecyclerView.LayoutManager manager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(manager);
        recyclerView.addItemDecoration(new DividerItemDecoration(this,DividerItemDecoration.VERTICAL));

        userListAdapter = new UserListAdapter(this);
        recyclerView.setAdapter(userListAdapter);

        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorAccent, R.color.fab_Ripple_color,R.color.snack_bar_background);
        //从列表顶部向下拉动的时候触发
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                presenter.requestUserData();
            }
        });

    }

    @Override
    public void onResume() {
        super.onResume();
        presenter.start();
        swipeRefreshLayout.setRefreshing(true);
        presenter.requestUserData();

    }

    @Override
    public void setPresenter(UserListContract.Presenter presenter) {
        this.presenter = presenter;
    }


    @Override
    public void showUserInList(UserInfo info) {
        if (info != null) {
            userListAdapter.addUser(info);
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    @Override
    public void showError() {
        swipeRefreshLayout.setRefreshing(false);
        Toast.makeText(this, "连接优图服务器超时，下拉重试", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId()==android.R.id.home){
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }
    
    
}
