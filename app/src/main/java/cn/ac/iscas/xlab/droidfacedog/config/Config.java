package cn.ac.iscas.xlab.droidfacedog.config;

/**
 * Created by lisongting on 2017/5/10.
 */

public class Config {

    public static String ROS_SERVER_IP = "192.168.1.111";

    public static String ROS_SERVER_PORT = "9090";

    public static String RECOGNITION_SERVER_IP = "192.168.0.111";

    public static String RECOGNITION_SERVER_PORT = "8000";

    public static double RECOG_THRESHOLD ;

    public static boolean ENABLE_MESSAGE_NOTIFICATION;

    public static String string() {
        StringBuffer sb = new StringBuffer();
        sb.append("人脸识别服务端：" + RECOGNITION_SERVER_IP + ":" + RECOGNITION_SERVER_PORT);
        sb.append("\nROS服务端：" + ROS_SERVER_IP + ":" + ROS_SERVER_PORT);
        sb.append("\n阈值:" + RECOG_THRESHOLD);
        sb.append("\n开启通知：" + ENABLE_MESSAGE_NOTIFICATION);
        return sb.toString();
    }
}
