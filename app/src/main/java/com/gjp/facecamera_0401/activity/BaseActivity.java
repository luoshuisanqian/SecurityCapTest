package com.gjp.facecamera_0401.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;

import com.gjp.facecamera_0401.utils.ActivityCollector;


@SuppressLint("Registered")
public  class BaseActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityCollector.addActivity(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ActivityCollector.removeActivity(this);
    }
    
    
    
}
