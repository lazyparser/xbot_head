package cn.ac.iscas.xlab.droidfacedog.mvp.aitalk;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import cn.ac.iscas.xlab.droidfacedog.CustomViews.WaveView;
import cn.ac.iscas.xlab.droidfacedog.R;

/**
 * Created by lisongting on 2017/7/31.
 */

public class AITalkActivity extends AppCompatActivity implements AITalkContract.View {

    public static final String TAG = "AITalkActivity";
    private AITalkContract.Presenter presenter;
    WaveView waveView;
    private TextView textView;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_talk);
        waveView = (WaveView) findViewById(R.id.id_waveView);
        textView = (TextView) findViewById(R.id.textView);
        initView();

        presenter = new AITalkPresenter(this,this);
    }

    @Override
    public void onResume() {
        super.onResume();

        presenter.start();

        waveView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (waveView.isWorking()) {
                    presenter.stopAiTalk();
                    waveView.endAnimation();
                } else {
                    presenter.startAiTalk();
                    waveView.startAnimation();
                }
            }
        });
    }

    @Override
    public void initView() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("AI对话模式");
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void setPresenter(AITalkContract.Presenter presenter) {
        this.presenter = presenter;

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (presenter != null) {
            presenter.releaseMemory();
            presenter = null;
        }
    }

    @Override
    public void showResultInTextView(String str) {
        textView.setText(str);
    }

    @Override
    public void startAnim() {
        waveView.startAnimation();
    }

    @Override
    public void stopAnim() {
        waveView.endAnimation();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.i(TAG, "onSaveInstanceState");
    }
}
