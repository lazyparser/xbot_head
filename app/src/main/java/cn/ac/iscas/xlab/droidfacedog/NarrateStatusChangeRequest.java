package cn.ac.iscas.xlab.droidfacedog;

/**
 * Created by lazyparser on 4/9/17.
 */

public class NarrateStatusChangeRequest {
    public enum PlayStatus {START, PAUSE, RESUME, STOP};

    public PlayStatus getRequest() {
        return request;
    }

    PlayStatus request;

    NarrateStatusChangeRequest(PlayStatus s) {
        request = s;
    }


}
