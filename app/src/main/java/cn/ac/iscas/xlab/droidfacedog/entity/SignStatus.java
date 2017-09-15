package cn.ac.iscas.xlab.droidfacedog.entity;

/**
 * Created by lisongting on 2017/9/12.
 * 用与人脸打卡功能，用来描述是否人脸识别完成
 */

public class SignStatus {

    //是否识别完成
    private boolean isComplete;

    //是否识别出已注册的用户
    private boolean isSuccess;

    
    public SignStatus(boolean isComplete, boolean isSuccess) {
        this.isComplete = isComplete;
        this.isSuccess = isSuccess;
    }

    public boolean isComplete() {
        return isComplete;
    }

    public void setComplete(boolean isComplete) {
        this.isComplete = isComplete;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public void setSuccess(boolean isSuccess) {
        this.isSuccess = isSuccess;
    }

    @Override
    public String toString() {
        return "SignStatus{" +
                "isComplete=" + isComplete +
                ", isSuccess=" + isSuccess +
                '}';
    }
}
