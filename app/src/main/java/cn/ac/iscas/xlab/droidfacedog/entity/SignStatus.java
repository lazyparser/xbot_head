package cn.ac.iscas.xlab.droidfacedog.entity;

/**
 * Created by lisongting on 2017/9/12.
 * 用与人脸打卡功能，用来描述是否人脸识别完成
 */

public class SignStatus {

    //是否识别完成
    private boolean isComplete;

    //是否识别出已注册的用户
    private boolean isRecogSuccess;

    
    public SignStatus(boolean isComplete, boolean isRecogSuccess) {
        this.isComplete = isComplete;
        this.isRecogSuccess = isRecogSuccess;
    }

    public boolean isComplete() {
        return isComplete;
    }

    public void setComplete(boolean isComplete) {
        this.isComplete = isComplete;
    }

    public boolean isRecogSuccess() {
        return isRecogSuccess;
    }

    public void setSuccess(boolean isRecogSuccess) {
        this.isRecogSuccess = isRecogSuccess;
    }

    @Override
    public String toString() {
        return "SignStatus{" +
                "isComplete=" + isComplete +
                ", isRecogSuccess=" + isRecogSuccess +
                '}';
    }
}
