package cn.ac.iscas.xlab.droidfacedog.entity;

/**
 * Created by lisongting on 2017/6/5.
 */

//该类用来描述机器人的状态
public class RobotStatus {

    //当前所处的区域id
    private int locationId;

    //当前底盘是否正在移动
    private boolean isMoving;


    public RobotStatus(int locationId,boolean isMoving) {
        this.locationId = locationId;
        this.isMoving = isMoving;
    }

    public int getLocationId() {
        return locationId;
    }

    public void setLocationId(int locationId) {
        this.locationId = locationId;
    }

    public boolean isMoving() {
        return isMoving;
    }

    public void setMoving(boolean moving) {
        isMoving = moving;
    }

    @Override
    public String toString() {
        return "RobotStatus{" +
                "locationId=" + locationId +
                ", isMoving=" + isMoving +
                '}';
    }
}
