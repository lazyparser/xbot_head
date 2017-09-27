package cn.ac.iscas.xlab.droidfacedog.entity;

/**
 * Created by lisongting on 2017/6/5.
 */

//该类用来描述当前MediaPlayer的播放情况
public class AudioStatus {

    //当前正在播放的解说词音频的id。
    private int id;
    //当前的播放状态
    private boolean isComplete;

    public AudioStatus(int id, boolean isComplete) {
        this.id = id;
        this.isComplete = isComplete;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean isComplete() {
        return isComplete;
    }

    public void setComplete(boolean complete) {
        isComplete = complete;
    }

    @Override
    public String toString() {
        return "AudioStatus{" +
                "id=" + id +
                ", isComplete=" + isComplete +
                '}';
    }
}
