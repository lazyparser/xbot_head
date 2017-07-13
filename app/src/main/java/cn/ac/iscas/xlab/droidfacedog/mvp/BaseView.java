package cn.ac.iscas.xlab.droidfacedog.mvp;

/**
 * Created by lisongting on 2017/7/11.
 * 采用MVP架构
 */

public interface BaseView<T> {
    void initView();
    void setPresenter(T presenter);
}
