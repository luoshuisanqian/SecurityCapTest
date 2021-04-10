package com.gjp.responbean;

/**
 * author: huangming
 * time: 2021/4/9
 * desc: 1:N返回respon
 */
public class OneToNRespon {

	/**
	 * {
	 *     "result": {
	 *         "base64": "图片base64编码",
	 *         "score": "相似得分"
	 *     },
	 *     "targetVo": {
	 *         "targetId": "人像id",
	 *         "recordId": "第三方同步的记录ID"
	 *     }
	 * }
	 */


	/**
	 * result : {"base64":"图片base64编码","score":"相似得分"}
	 * targetVo : {"targetId":"人像id","recordId":"第三方同步的记录ID"}
	 */

	private ResultBean result;
	private TargetVoBean targetVo;

	public ResultBean getResult() {
		return result;
	}

	public void setResult(ResultBean result) {
		this.result = result;
	}

	public TargetVoBean getTargetVo() {
		return targetVo;
	}

	public void setTargetVo(TargetVoBean targetVo) {
		this.targetVo = targetVo;
	}

	public static class ResultBean {
		/**
		 * base64 : 图片base64编码
		 * score : 相似得分
		 */

		private String base64;
		private String score;

		public String getBase64() {
			return base64;
		}

		public void setBase64(String base64) {
			this.base64 = base64;
		}

		public String getScore() {
			return score;
		}

		public void setScore(String score) {
			this.score = score;
		}
	}

	public static class TargetVoBean {
		/**
		 * targetId : 人像id
		 * recordId : 第三方同步的记录ID
		 */

		private String targetId;
		private String recordId;

		public String getTargetId() {
			return targetId;
		}

		public void setTargetId(String targetId) {
			this.targetId = targetId;
		}

		public String getRecordId() {
			return recordId;
		}

		public void setRecordId(String recordId) {
			this.recordId = recordId;
		}
	}
}
