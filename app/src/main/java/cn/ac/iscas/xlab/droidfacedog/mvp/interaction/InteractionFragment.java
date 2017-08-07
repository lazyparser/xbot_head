package cn.ac.iscas.xlab.droidfacedog.mvp.interaction;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

import cn.ac.iscas.xlab.droidfacedog.CustomViews.WaveView;
import cn.ac.iscas.xlab.droidfacedog.R;

/**
 * Created by lisongting on 2017/8/7.
 */

public class InteractionFragment extends Fragment implements InteractionContract.View {

    public static final String TAG = "InteractionFragment";
    private WaveView waveView;
    private TextureView textureView;

    public InteractionFragment() {}

    public static InteractionFragment newInstance(){
        return new InteractionFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_interaction, container, false);


        return view;
    }

    @Override
    public void initView() {

    }

    @Override
    public void setPresenter(InteractionContract.Presenter presenter) {

    }

    @Override
    public void startAnimation() {

    }

    @Override
    public void stopAnimation() {

    }

    @Override
    public void startCamera() {

    }

    @Override
    public void stopCamera() {

    }

    public void log(String string) {
        Log.i(TAG, TAG + " -- " + string);
    }
}
