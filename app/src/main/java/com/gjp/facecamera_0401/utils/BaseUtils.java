package com.gjp.facecamera_0401.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author : huangm
 * @time : 2021/4/12
 * @subscri :
 */

public class BaseUtils {
    /**
     * 使用正则表达式来判断字符串中是否包含字母
     * @param str 待检验的字符串
     * @return 返回是否包含
     * true: 包含字母 ;false 不包含字母
     */
    public static boolean judgeContainsStr(String str) {
        String regex=".*[a-zA-Z]+.*";
        Matcher m= Pattern.compile(regex).matcher(str);
        return m.matches();
    }
}
