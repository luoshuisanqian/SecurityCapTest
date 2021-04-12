package com.gjp.facecamera_0401.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.arcsoft.face.ActiveFileInfo;
import com.arcsoft.face.ErrorInfo;
import com.arcsoft.face.FaceEngine;
import com.arcsoft.face.enums.RuntimeABI;
import com.gjp.facecamera_0401.R;
import com.gjp.facecamera_0401.application.MyApplication;
import com.gjp.facecamera_0401.arcsoft.common.Constants;
import com.gjp.facecamera_0401.arcsoft.util.ConfigUtil;
import com.gjp.facecamera_0401.postbean.LoginPostBean;
import com.gjp.facecamera_0401.promptdialog.PromptDialog;
import com.gjp.facecamera_0401.utils.API;
import com.gjp.facecamera_0401.utils.GsonUtil;
import com.gjp.facecamera_0401.utils.LogUtils;
import com.gjp.facecamera_0401.utils.PermissionConstants;
import com.gjp.facecamera_0401.utils.PermissionUtils;
import com.gjp.facecamera_0401.utils.ToastUtil;
import com.gjp.responbean.LoginRespon;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.StringCallback;
import com.lzy.okgo.model.Response;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.RequestBody;

import static com.arcsoft.face.enums.DetectFaceOrientPriority.ASF_OP_0_ONLY;
import static com.arcsoft.face.enums.DetectFaceOrientPriority.ASF_OP_270_ONLY;
import static com.arcsoft.face.enums.DetectFaceOrientPriority.ASF_OP_90_ONLY;

public class MainActivity extends BaseActivity {

    boolean libraryExists = true;
    // arc人脸识别所需的动态库 所需的动态库文件
    private static final String[] LIBRARIES = new String[]{
            // 人脸相关
            "libarcsoft_face_engine.so",
            "libarcsoft_face.so",
            // 图像库相关
            "libarcsoft_image_util.so",
    };
    private Button test_btn;

    private TextView userName_et;//用户名
    private TextView password_et;//密码
    private TextView login_tv;//登录
    private String userName = "";
    private String passWord = "";

    private boolean arcRegisterSuccess;

    private PromptDialog promptDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        test_btn = (Button) findViewById(R.id.test_btn);
        //初始化人脸SDK
        initArcSDK();

        //用户名， 密码
        userName_et = (TextView) findViewById(R.id.userName_et);
        password_et = (TextView) findViewById(R.id.password_et);
        login_tv = (TextView) findViewById(R.id.login_tv);

        promptDialog = new PromptDialog(this);


        login_tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                userName = userName_et.getText().toString().trim();
                passWord = password_et.getText().toString().trim();
                if (TextUtils.isEmpty(userName)) {
                    ToastUtil.show(MainActivity.this, "请输入用户名");
                } else if (TextUtils.isEmpty(passWord)) {
                    ToastUtil.show(MainActivity.this, "请输入密码");
                } else {


                    if (arcRegisterSuccess) {//如果激活成功
                        //TODO 测试
//                    startActivity(new Intent(MainActivity.this, FaceCameraActivity.class));

                        //登录请求
                        PostLogin();
                    } else {
                        ToastUtil.show(MainActivity.this, "请先联外网激活人脸识别SDK");
                    }
                }
            }
        });


    }

    private void PostLogin() {
        promptDialog.showLoading("正在登录");

        LoginPostBean bean = new LoginPostBean();
        bean.setUsername(userName);
        bean.setPassword(passWord);
        String jsonStr = GsonUtil.GsonString(bean);
        LogUtils.i("jsonStr ===登录接口请求实体====" + jsonStr);
        //post请求body
        RequestBody requestBody = RequestBody.create(MediaType.parse(
                "application/json"), jsonStr);
        OkGo.<String>post(API.BASE_URL + API.LOGIN_URL)
                .upRequestBody(requestBody)
                .execute(new StringCallback() {
                    @Override
                    public void onSuccess(Response<String> response) {
                        promptDialog.dismissImmediately();
                        LoginRespon respon = GsonUtil.GsonToBean(response.body(), LoginRespon.class);
                        MyApplication.accessToken = respon.getAccessToken();
                        MyApplication.userId = respon.getUserId();
                        MyApplication.realname = respon.getRealname();//用户名
                        startActivity(new Intent(MainActivity.this, FaceCameraActivity.class));

                    }

                    @Override
                    public void onError(Response<String> response) {
                        super.onError(response);
                        promptDialog.dismissImmediately();
                        ToastUtil.show(MainActivity.this, API.NETWORK_ERROR);
                    }
                });
    }

    private void initArcSDK() {
        //初始化人脸识别
        libraryExists = checkSoFile(LIBRARIES);
        ApplicationInfo applicationInfo = getApplicationInfo();
        Log.e("TAG", "onCreate===: " + applicationInfo.nativeLibraryDir);
        if (!libraryExists) {
            ToastUtil.show(MainActivity.this, getResources().getString(R.string.library_not_found));
        } else {

            ConfigUtil.setFtOrient(MainActivity.this, ASF_OP_270_ONLY);//视频模式仅检查90度【POS机】2.横屏平板0度
            //激活方法
            activeEngine();
        }
    }

    /**
     * 激活引擎
     */
    public void activeEngine() {
        if (!libraryExists) {
            ToastUtil.show(this, getResources().getString(R.string.library_not_found));
            return;
        }
        //申请权限
        PermissionUtils.permission(this, PermissionConstants.PHONE, PermissionConstants.STORAGE)
                .rationale(new PermissionUtils.OnRationaleListener() {
                    @Override
                    public void rationale(ShouldRequest shouldRequest) {
                        shouldRequest.again(true);
                    }
                }).callback(new PermissionUtils.FullCallback() {
            @Override
            public void onGranted(List<String> permissionsGranted) {
                /**
                 *  异步激活
                 */
                AsyncActive();
            }

            @Override
            public void onDenied(List<String> permissionsDeniedForever, List<String> permissionsDenied) {
                ToastUtil.show(MainActivity.this, "权限拒绝, 请重启应用允许权限");
            }
        }).request();

    }


    private void AsyncActive() {
        Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> emitter) {
                RuntimeABI runtimeABI = FaceEngine.getRuntimeABI();
                Log.e("TAG", "subscribe: getRuntimeABI() ===" + runtimeABI);
                int activeCode = FaceEngine.activeOnline(MainActivity.this, Constants.APP_ID, Constants.SDK_KEY);
                emitter.onNext(activeCode);
            }
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Integer>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                    }

                    @Override
                    public void onNext(Integer activeCode) {
                        if (activeCode == ErrorInfo.MOK) {/**激活成功***/
                            Log.e("","激活成功=====");
                            arcRegisterSuccess = true;
                        } else if (activeCode == ErrorInfo.MERR_ASF_ALREADY_ACTIVATED) {/***已经激活***/
//                            showToast(getString(R.string.already_activated));
                            arcRegisterSuccess = true;

                        } else { /**激活失败****/
                            ToastUtil.show(MainActivity.this, getResources().getString(R.string.active_failed) + activeCode);
                        }

                        ActiveFileInfo activeFileInfo = new ActiveFileInfo();
                        int res = FaceEngine.getActiveFileInfo(MainActivity.this, activeFileInfo);
                        if (res == ErrorInfo.MOK) {
                            Log.i("TAG", activeFileInfo.toString());
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        ToastUtil.show(MainActivity.this, e.getMessage());
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }




    /**
     * 检查能否找到动态链接库，如果找不到，请修改工程配置
     *
     * @param libraries 需要的动态链接库
     * @return 动态库是否存在
     */
    private boolean checkSoFile(String[] libraries) {
        File dir = new File(getApplicationInfo().nativeLibraryDir);
        File[] files = dir.listFiles();
        if (files == null || files.length == 0) {
            return false;
        }
        List<String> libraryNameList = new ArrayList<>();
        for (File file : files) {
            libraryNameList.add(file.getName());
        }
        boolean exists = true;
        for (String library : libraries) {
            exists &= libraryNameList.contains(library);
        }
        return exists;
    }
}
