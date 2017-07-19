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

    //科大讯飞命令词识别功能所需的语法文本
    public static final String SPEECH_GRAMMAR = "#ABNF 1.0 UTF-8;\n" +
            "language zh-CN;\n" +
            "mode voice;\n" +
            "root command;\n" +
            "$command = $action [$speech];\n" +
            "$action = 暂停|停止|继续|恢复|开始;\n" +
            "$speech = 解说|播放;";

    //讯飞开放平台中获得的APPID
    public static final String APPID = "59198461";

    public static String string() {
        StringBuffer sb = new StringBuffer();
        sb.append("\n人脸识别服务端：" + RECOGNITION_SERVER_IP + ":" + RECOGNITION_SERVER_PORT);
        sb.append("\nROS服务端：" + ROS_SERVER_IP + ":" + ROS_SERVER_PORT);
        sb.append("\n阈值:" + RECOG_THRESHOLD);
        sb.append("\n开启通知：" + ENABLE_MESSAGE_NOTIFICATION);
        return sb.toString();
    }
}
