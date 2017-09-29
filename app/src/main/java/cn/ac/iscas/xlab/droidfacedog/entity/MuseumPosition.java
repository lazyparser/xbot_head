package cn.ac.iscas.xlab.droidfacedog.entity;

/**
 * Created by lisongting on 2017/9/25.
 */

public class MuseumPosition {

    private int locationId;

    private boolean isMoving;

    public MuseumPosition(int locationId, boolean isMoving) {
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
        return "MuseumPosition{" +
                "locationId=" + locationId +
                ", isMoving=" + isMoving +
                '}';
    }
}
