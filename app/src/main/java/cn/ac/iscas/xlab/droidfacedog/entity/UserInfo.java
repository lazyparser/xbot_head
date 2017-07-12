package cn.ac.iscas.xlab.droidfacedog.entity;

import android.graphics.Bitmap;

/**
 * Created by lisongting on 2017/7/12.
 */

public class UserInfo {
    private String name;
    private Bitmap face;

    public UserInfo() {

    }

    public UserInfo(String name, Bitmap face) {
        this.name = name;
        this.face = face;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Bitmap getFace() {
        return face;
    }

    public void setFace(Bitmap face) {
        this.face = face;
    }

    @Override
    public String toString() {
        return "UserInfo{" +
                "name='" + name + '\'' +
                ", face:" + face.getWidth()+"x"+face.getHeight() +
                '}';
    }
}
