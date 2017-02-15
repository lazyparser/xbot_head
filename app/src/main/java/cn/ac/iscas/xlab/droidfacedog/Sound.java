package cn.ac.iscas.xlab.droidfacedog;

import android.content.Intent;
import android.support.annotation.IntegerRes;

/**
 * Created by wuwei on 02/14/17.
 */

public class Sound {
    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public String getAssetPath() {

        return mAssetPath;
    }

    public void setAssetPath(String assetPath) {
        mAssetPath = assetPath;
    }

    private String mAssetPath;
    private String mName;

    public Integer getSoundId() {
        return mSoundId;
    }

    public void setSoundId(Integer soundId) {
        mSoundId = soundId;
    }

    private Integer mSoundId;

    public Sound(String assetPath) {
        mAssetPath = assetPath;
        String[] components = assetPath.split("/");
        String filename = components[components.length - 1];
        mName = filename.replace(".mp3", "");
    }
}