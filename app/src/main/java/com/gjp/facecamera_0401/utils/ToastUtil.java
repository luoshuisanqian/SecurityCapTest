package com.gjp.facecamera_0401.utils;

import android.content.Context;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.gjp.facecamera_0401.application.MyApplication;


/**
 * Created by Administrator on 2018/8/18.
 */

public class ToastUtil {

    //自定义toast对象
    private static Toast toast;

    /**
     * 取消显示Toast
     *
     */
    public static void cancelToast() {
        if (null != toast) {
            toast.cancel();
        }
    }


    /**
     * 将最长使用的显示方法单独提出来，方便使用。
     * 屏幕中心位置短时间显示Toast。
     * @param context
     * @param message
     */
    public static void show(Context context, String message) {
        ToastShortCenter(context,message);
    }

    /**
     * 屏幕底部中间位置显示短时间Toast
     *
     * @param context
     * @param message
     */
    public static void ToastShortBottomCenter(Context context, String message) {
        if (context != null) {
            if (toast == null) {
                toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
            }else {
                toast.setText(message);
            }
            toast.show();
        }
    }


    /**
     * 屏幕中心位置短时间显示Toast
     *
     * @param context
     * @param message
     */
    public static void ToastShortCenter(Context context, String message) {
        if (context != null) {
            if (toast == null){
                toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
            }else {
                toast.setText(message);
            }
            LinearLayout layout =(LinearLayout) toast.getView();
            TextView textView = (TextView) layout.getChildAt(0);
            if (MyApplication.isPad) {/***如果是平板**/
                textView.setTextSize(40f);
            } else {
                textView.setTextSize(18f);
            }
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        }
    }



}
