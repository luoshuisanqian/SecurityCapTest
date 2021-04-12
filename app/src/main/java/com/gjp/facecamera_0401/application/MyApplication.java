package com.gjp.facecamera_0401.application;

import android.app.Application;

import com.lzy.okgo.OkGo;

/**
 * @author : huangm
 * @time : 2021/4/2
 * @subscri :
 */

public class MyApplication extends Application {
	public static boolean isPad = false;//是否是平板
	public static int minFaceWidth = 60;//最小的人脸识别宽度    @Override
	/***人脸识别相似度阈值*****/
	public static int faceSimilarity = 80;
	public static long subTime = 3000;
	public static String accessToken = "";//用户票据
	public static String userId = "";//用户id
	public static String realname = "";//用户名
	public void onCreate() {
		super.onCreate();
		//初始化OkGo网络配置
		initOkGo();
	}

	private void initOkGo() {
		OkGo.getInstance().init(this);//默认配置
	}
}
