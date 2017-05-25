package cn.ac.iscas.xlab.droidfacedog.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by lisongting on 2017/5/10.
 */

public class RegexCheckUtil {

    public static final String REGEX_THRESHOLD = "^0\\.[1-9]+?";

    public static final String REGEX_IP = "^0*([1-9]?\\d|1\\d\\d|2[0-4]\\d|25[0-5])\\.0*([1-9]?" +
            "\\d|1\\d\\d|2[0-4]\\d|25[0-5])\\.0*([1-9]?\\d|1\\d\\d|2[0-4]\\d|25[0-5])\\.0*([1-9]?\\" +
            "d|1\\d\\d|2[0-4]\\d|25[0-5])$";

    public static boolean isRightThreshold(String str){
        Pattern pattern = Pattern.compile(REGEX_THRESHOLD);
        Matcher matcher = pattern.matcher(str);
        return matcher.find();
    }

    public static boolean isRightIP(String str) {
        Pattern pattern = Pattern.compile(REGEX_IP);
        Matcher matcher = pattern.matcher(str);
        return matcher.find();
    }

    public static boolean isRightPersonName(String str) {
        Pattern pattern = Pattern.compile("[\\u4E00-\\u9FA5]{2,5}(?:·[\\u4E00-\\u9FA5]{2,5})*");
        Matcher matcher = pattern.matcher(str);
        return matcher.find();
    }

}
