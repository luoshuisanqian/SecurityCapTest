package com.gjp.facecamera_0401.postbean;

/**
 * author: huangming
 * time: 2021/4/7
 * desc:1:N请求参数
 */
public class OneToNPostBean {
	private String imgBase64;
	private String libraryId;

	public String getImgBase64() {
		return imgBase64;
	}

	public void setImgBase64(String imgBase64) {
		this.imgBase64 = imgBase64;
	}

	public String getLibraryId() {
		return libraryId;
	}

	public void setLibraryId(String libraryId) {
		this.libraryId = libraryId;
	}
}
