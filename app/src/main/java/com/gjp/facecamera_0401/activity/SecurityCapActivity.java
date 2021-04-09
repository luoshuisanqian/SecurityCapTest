package com.gjp.facecamera_0401.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

import com.gjp.facecamera_0401.R;

public class SecurityCapActivity extends BaseActivity {

	private TextView capStatus_tv;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_security_cap);
		capStatus_tv = (TextView) findViewById(R.id.capStatus_tv);
		String status = getIntent().getStringExtra("status");
		if (status.equals("0")) {
			capStatus_tv.setText("安全帽已佩戴");
		} else {
			capStatus_tv.setText("安全帽未佩戴");
		}
	}
}
