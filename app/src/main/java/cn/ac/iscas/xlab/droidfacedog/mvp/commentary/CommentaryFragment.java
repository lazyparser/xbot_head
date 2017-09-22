package cn.ac.iscas.xlab.droidfacedog.mvp.commentary;

import android.support.v4.app.Fragment;

import cn.ac.iscas.xlab.droidfacedog.RosConnectionService;

/**
 * Created by lisongting on 2017/9/22.
 */

public class CommentaryFragment extends Fragment implements CommentaryContract.View {

    private CommentaryContract.Presenter presenter;

    public CommentaryFragment() {}

    public static CommentaryFragment newInstance(){
        return new CommentaryFragment();
    }

    @Override
    public void initView() {

    }

    @Override
    public void setPresenter(CommentaryContract.Presenter presenter) {

    }

    @Override
    public void startCamera() {

    }

    @Override
    public void closeCamera() {

    }

    @Override
    public void displayInfo(String str) {

    }

    public void setRosServiceBinder(RosConnectionService.ServiceBinder binder) {
        presenter.setServiceProxy(binder);
    }
}
