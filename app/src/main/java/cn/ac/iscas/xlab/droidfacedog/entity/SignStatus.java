package cn.ac.iscas.xlab.droidfacedog.entity;

/**
 * Created by lisongting on 2017/9/12.
 * 用与人脸打卡功能，用来描述是否人脸识别完成
 */

public class SignStatus {

    //是否识别完成
    private boolean isDone;

    //是否识别出已注册的用户
    private boolean isSuccess;

    public boolean isDone() {
        return isDone;
    }

    public void setDone(boolean done) {
        isDone = done;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public void setSuccess(boolean success) {
        isSuccess = success;
    }

    @Override
    public String toString() {
        return "SignStatus{" +
                "isDone=" + isDone +
                ", isSuccess=" + isSuccess +
                '}';
    }
}
