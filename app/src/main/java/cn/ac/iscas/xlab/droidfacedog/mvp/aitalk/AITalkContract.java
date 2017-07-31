package cn.ac.iscas.xlab.droidfacedog.mvp.aitalk;

import cn.ac.iscas.xlab.droidfacedog.mvp.BasePresenter;
import cn.ac.iscas.xlab.droidfacedog.mvp.BaseView;

/**
 * Created by lisongting on 2017/7/31.
 */

public interface AITalkContract {

    interface Presenter extends BasePresenter{

        void startAiTalk();

        void stopAiTalk();

    }

    interface View extends BaseView<Presenter>{

    }
}
