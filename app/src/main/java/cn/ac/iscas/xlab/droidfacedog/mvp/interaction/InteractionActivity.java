package cn.ac.iscas.xlab.droidfacedog.mvp.interaction;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import cn.ac.iscas.xlab.droidfacedog.R;

/**
 * Created by lisongting on 2017/8/7.
 */

public class InteractionActivity extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.container_layout);

        getSupportFragmentManager().beginTransaction()
                .add(R.id.container, InteractionFragment.newInstance())
                .commit();
    }
}
