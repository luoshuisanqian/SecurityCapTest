package com.gjp.facecamera_0401.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import com.gjp.facecamera_0401.R;

public class OneToNResultActivity extends BaseActivity {

	private TextView oneToN_type_tv;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_one_to_nresult);
		oneToN_type_tv = (TextView) findViewById(R.id.oneToN_type_tv);
		Intent intent = getIntent();
		String oneToN_type = intent.getStringExtra("oneToN_type");
		if (oneToN_type.equals("1")) {
			oneToN_type_tv.setText("未匹配到人脸信息");
		} else if (oneToN_type.equals("2")) {
			oneToN_type_tv.setText("不是本人");
		}
	}
}
