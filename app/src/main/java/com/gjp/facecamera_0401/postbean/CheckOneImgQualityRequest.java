package com.gjp.facecamera_0401.postbean;

/**
 * author: huangming
 * time: 2021/4/7
 * desc: 人脸属性检测接口
 */
public class CheckOneImgQualityRequest {
	private String imgBase64;

	public String getImgBase64() {
		return imgBase64;
	}

	public void setImgBase64(String imgBase64) {
		this.imgBase64 = imgBase64;
	}
}
