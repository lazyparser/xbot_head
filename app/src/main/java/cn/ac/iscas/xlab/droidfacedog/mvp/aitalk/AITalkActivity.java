package cn.ac.iscas.xlab.droidfacedog.mvp.aitalk;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import cn.ac.iscas.xlab.droidfacedog.CustomViews.WaveView;
import cn.ac.iscas.xlab.droidfacedog.R;

/**
 * Created by lisongting on 2017/7/31.
 */

public class AITalkActivity extends AppCompatActivity implements AITalkContract.View {

    private AITalkContract.Presenter presenter;
    WaveView waveView;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_ai_talk);
        waveView = (WaveView) findViewById(R.id.id_waveView);


    }

    @Override
    public void onResume() {
        super.onResume();

//        presenter.start();

        waveView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (waveView.isWorking()) {
//                    presenter.stopAiTalk();
                    waveView.setInWorkingState(false);
                    waveView.endAnimation();
                } else {
//                    presenter.startAiTalk();
                    waveView.setInWorkingState(true);
                    waveView.startAnimation();
                }
            }
        });
    }

    @Override
    public void initView() {

    }

    @Override
    public void setPresenter(AITalkContract.Presenter presenter) {

    }
}
