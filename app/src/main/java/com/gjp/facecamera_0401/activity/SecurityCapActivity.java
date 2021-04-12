package com.gjp.facecamera_0401.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import com.gjp.facecamera_0401.R;

public class SecurityCapActivity extends BaseActivity {

	private ImageView securityStatus_iv;
	private TextView securityStatus_tv;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_security_cap);
		securityStatus_iv = (ImageView) findViewById(R.id.securityStatus_iv);
		securityStatus_tv = (TextView) findViewById(R.id.securityStatus_tv);



		String status = getIntent().getStringExtra("status");
		if (status.equals("0")) {
			securityStatus_iv.setImageResource(R.mipmap.right_icon);
			securityStatus_tv.setText("人脸识别成功，安全帽已佩戴");
			securityStatus_tv.setTextColor(ContextCompat.getColor(SecurityCapActivity.this, R.color.green));
		} else {
			securityStatus_iv.setImageResource(R.mipmap.error_icon);
			securityStatus_tv.setText("人脸识别成功，安全帽未佩戴");
			securityStatus_tv.setTextColor(ContextCompat.getColor(SecurityCapActivity.this, R.color.red));
		}
	}
}
