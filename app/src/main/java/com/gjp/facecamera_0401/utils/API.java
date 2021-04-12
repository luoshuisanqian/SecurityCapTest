package com.gjp.facecamera_0401.utils;

/**
 * @author : huangm
 * @time : 2021/4/2
 * @subscri :
 */

public interface API {
//    String BASE_URL = "http://10.19.1.96:30080";
    String BASE_URL = "http://183.129.144.67:30080";

    //登录接口
    String LOGIN_URL = "/senseguard-oauth2/api/v1/login";
    //上传活体照片
    String UPDATE_FACE_LIVE = "/senseguard-tools/api/v1/tools/one_to_many_face_compare";
    //检测人脸是否带安全帽
    String FACE_SECURITY_MAO = "/senseguard-tools/api/v1/tools/face_attribute_detection";


    String NETWORK_ERROR = "连接服务器失败,请检查网络或稍后重试!";
    int WAIT_TIME_3000 = 3000;
    int WAIT_TIME_2000 = 2000;
}
