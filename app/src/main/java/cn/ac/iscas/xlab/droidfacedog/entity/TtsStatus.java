package cn.ac.iscas.xlab.droidfacedog.entity;

/**
 * Created by lisongting on 2017/6/5.
 */

//该类用来描述当前MediaPlayer的播放情况
public class TtsStatus {

    //当前正在播放的解说词音频的id。现在一共就三部分音频,取值[0,2]
    private int id;
    //当前的播放状态
    private boolean isplaying;

    public TtsStatus() {
        this.id = -1;
        this.isplaying = false;
    }

    public TtsStatus(int id, boolean isplaying) {
        this.id = id;
        this.isplaying = isplaying;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean isplaying() {
        return isplaying;
    }

    public void setIsplaying(boolean isplaying) {
        this.isplaying = isplaying;
    }

    public String toString() {
        return "[" + id + "," + isplaying + "]";
    }
}
