package cn.ac.iscas.xlab.droidfacedog;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

/**
 * Created by lisongting on 2017/7/5.
 */

public class WatingDialogFragment extends DialogFragment {

    private Button btCancel;
    private Button btRecogDirect;
    private CircleRotateView circleRotateView;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(false);
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_wating_layout, container, false);
        btCancel = (Button) view.findViewById(R.id.id_bt_cancel);
        btRecogDirect = (Button) view.findViewById(R.id.id_bt_recog_direct);
        circleRotateView = (CircleRotateView) view.findViewById(R.id.loading_view);


    }
}
