package cn.ac.iscas.xlab.droidfacedog;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import cn.ac.iscas.xlab.droidfacedog.CustomViews.CircleRotateView;

/**
 * Created by lisongting on 2017/7/5.
 */

public class WaitingDialogFragment extends DialogFragment {

    public static final String TAG = "WaitingDialogFragment";
    private Button btConnectBackGround;
    private CircleRotateView circleRotateView;

    public WaitingDialogFragment() {

    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "WaitingDialogFragment -- onCreate()");
        super.onCreate(savedInstanceState);
        setCancelable(false);
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.i(TAG, "WaitingDialogFragment -- onCreateView()");
        View view = inflater.inflate(R.layout.fragment_wating_layout, container, false);
        btConnectBackGround = (Button) view.findViewById(R.id.id_bt_connect_back);
        circleRotateView = (CircleRotateView) view.findViewById(R.id.id_loading_view);
        return view;
    }

    public Button getBtConnectBackGround() {
        return btConnectBackGround;
    }

    public CircleRotateView getCircleRotateView() {
        return circleRotateView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.i(TAG, "WaitingDialogFragment -- onActivityCreated()");
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onAttach(Context context) {
        Log.i(TAG, "WaitingDialogFragment -- onAttach()");
        super.onAttach(context);
    }

    @Override
    public void onStart() {
        Log.i(TAG, "WaitingDialogFragment -- onStart()");
        super.onStart();
    }

    @Override
    public void onResume() {
        Log.i(TAG, "WaitingDialogFragment -- onResume()");
        super.onResume();

    }

    @Override
    public void onDetach() {
        super.onDetach();
    }


    @Override
    public void onHiddenChanged(boolean hidden) {

    }
}
