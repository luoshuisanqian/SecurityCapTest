package com.gjp.facecamera_0401.activity;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.arcsoft.face.AgeInfo;
import com.arcsoft.face.ErrorInfo;
import com.arcsoft.face.FaceEngine;
import com.arcsoft.face.FaceFeature;
import com.arcsoft.face.GenderInfo;
import com.arcsoft.face.LivenessInfo;
import com.arcsoft.face.VersionInfo;
import com.arcsoft.face.enums.DetectFaceOrientPriority;
import com.arcsoft.face.enums.DetectMode;
import com.gjp.facecamera_0401.R;
import com.gjp.facecamera_0401.application.MyApplication;
import com.gjp.facecamera_0401.arcsoft.faceserver.CompareResult;
import com.gjp.facecamera_0401.arcsoft.faceserver.FaceServer;
import com.gjp.facecamera_0401.arcsoft.model.DrawInfo;
import com.gjp.facecamera_0401.arcsoft.model.FacePreviewInfo;
import com.gjp.facecamera_0401.arcsoft.util.ConfigUtil;
import com.gjp.facecamera_0401.arcsoft.util.DrawHelper;
import com.gjp.facecamera_0401.arcsoft.util.camera.CameraHelper;
import com.gjp.facecamera_0401.arcsoft.util.camera.CameraListener;
import com.gjp.facecamera_0401.arcsoft.util.face.FaceHelper;
import com.gjp.facecamera_0401.arcsoft.util.face.FaceListener;
import com.gjp.facecamera_0401.arcsoft.util.face.LivenessType;
import com.gjp.facecamera_0401.arcsoft.util.face.RecognizeColor;
import com.gjp.facecamera_0401.arcsoft.util.face.RequestFeatureStatus;
import com.gjp.facecamera_0401.arcsoft.util.face.RequestLivenessStatus;
import com.gjp.facecamera_0401.arcsoft.widget.FaceRectView;
import com.gjp.facecamera_0401.arcsoft.widget.FaceSearchResultAdapter;
import com.gjp.facecamera_0401.arcsoft.widget.RoundBorderView;
import com.gjp.facecamera_0401.arcsoft.widget.RoundTextureView;
import com.gjp.facecamera_0401.postbean.CheckOneImgQualityRequest;
import com.gjp.facecamera_0401.postbean.LoginPostBean;
import com.gjp.facecamera_0401.postbean.OneToNPostBean;
import com.gjp.facecamera_0401.promptdialog.PromptDialog;
import com.gjp.facecamera_0401.utils.API;
import com.gjp.facecamera_0401.utils.GsonUtil;
import com.gjp.facecamera_0401.utils.ImageUtils;
import com.gjp.facecamera_0401.utils.LogUtils;
import com.gjp.facecamera_0401.utils.PermissionConstants;
import com.gjp.facecamera_0401.utils.PermissionUtils;
import com.gjp.facecamera_0401.utils.ToastUtil;
import com.gjp.responbean.OneToNRespon;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.StringCallback;
import com.lzy.okgo.model.Response;

import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.RequestBody;

public class FaceCameraActivity extends BaseActivity implements ViewTreeObserver.OnGlobalLayoutListener, SeekBar.OnSeekBarChangeListener {
    private static final String TAG = "RegisterAndRecognize";
    private static final int MAX_DETECT_NUM = 10;
    /**
     * 当FR成功，活体未成功时，FR等待活体的时间
     */
    private static final int WAIT_LIVENESS_INTERVAL = 100;
    /**
     * 失败重试间隔时间（ms）
     */
    private static final long FAIL_RETRY_INTERVAL = 150;
    /**
     * 出错重试最大次数
     */
    private static final int MAX_RETRY_TIME = 3;
    private static final int REFRESH_PAGE_DATA = 1;

    private CameraHelper cameraHelper;
    private DrawHelper drawHelper;
    private Camera.Size previewSize;
    /**
     * 优先打开的摄像头，本界面主要用于单目RGB摄像头设备，因此默认打开前置
     */
    private Integer rgbCameraID = Camera.CameraInfo.CAMERA_FACING_FRONT;

    /**
     * VIDEO模式人脸检测引擎，用于预览帧人脸追踪
     */
    private FaceEngine ftEngine;
    /**
     * 用于特征提取的引擎
     */
    private FaceEngine frEngine;
    /**
     * IMAGE模式活体检测引擎，用于预览帧人脸活体检测
     */
    private FaceEngine flEngine;

    private int ftInitCode = -1;
    private int frInitCode = -1;
    private int flInitCode = -1;
    private FaceHelper faceHelper;
    private List<CompareResult> compareResultList;
    private FaceSearchResultAdapter adapter;
    /**
     * 活体检测的开关
     */
    private boolean livenessDetect = true;
    /**
     * 注册人脸状态码，准备注册
     */
    private static final int REGISTER_STATUS_READY = 0;
    /**
     * 注册人脸状态码，注册中
     */
    private static final int REGISTER_STATUS_PROCESSING = 1;
    /**
     * 注册人脸状态码，注册结束（无论成功失败）
     */
    private static final int REGISTER_STATUS_DONE = 2;

    private int registerStatus = REGISTER_STATUS_DONE;
    /**
     * 用于记录人脸识别相关状态
     */
    private ConcurrentHashMap<Integer, Integer> requestFeatureStatusMap = new ConcurrentHashMap<>();
    /**
     * 用于记录人脸特征提取出错重试次数
     */
    private ConcurrentHashMap<Integer, Integer> extractErrorRetryMap = new ConcurrentHashMap<>();
    /**
     * 用于存储活体值
     */
    private ConcurrentHashMap<Integer, Integer> livenessMap = new ConcurrentHashMap<>();
    /**
     * 用于存储活体检测出错重试次数
     */
    private ConcurrentHashMap<Integer, Integer> livenessErrorRetryMap = new ConcurrentHashMap<>();

    private CompositeDisposable getFeatureDelayedDisposables = new CompositeDisposable();
    private CompositeDisposable delayFaceTaskCompositeDisposable = new CompositeDisposable();
    /**
     * 相机预览显示的控件，可为SurfaceView或TextureView
     */
    private RoundTextureView previewView;
    /**
     * 绘制人脸框的控件
     */
    private FaceRectView faceRectView;

    private Switch switchLivenessDetect;

    private static final int ACTION_REQUEST_PERMISSIONS = 0x001;
    /**
     * 识别阈值
     */
//    private static final float SIMILAR_THRESHOLD = 0.85F;
    /**
     * 所需的所有权限信息
     */
    private static final String[] NEEDED_PERMISSIONS = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.READ_PHONE_STATE,
    };
    /////////////////////////////
    private RoundBorderView roundBorderView;
    //用于调整圆角大小
    private SeekBar radiusSeekBar;
    private int facePreviewLeft = 0;//人脸框左边距
    private int facePreviewRight = 0;//人脸框右边距
    private TextView live_tv;
    private TextView tips_tv;
    private int faceAction = 0;
    private long faceActionTime = 0;
    private ImageView faceEyes_iv;//眨眼
    private ImageView faceHead_iv;//摇头
    private ImageView faceMouth_iv;//张嘴

    //tab1
    private ImageView tab_iv1;
    private TextView tab_tv1;
    private TextView tab_line_1;
    //tab2
    private ImageView tab_iv2;
    private TextView tab_tv2;
    private TextView tab_line_2;
    //tab3
    private ImageView tab_iv3;
    private TextView tab_tv3;
    private TextView tab_line3;
    //tab4
    private ImageView tab_iv4;
    private TextView tab_tv4;


    private PromptDialog promptDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_camera);

        //保持亮屏
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WindowManager.LayoutParams attributes = getWindow().getAttributes();
            attributes.systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            getWindow().setAttributes(attributes);
        }
        // Activity启动后就锁定为启动时的方向
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        //本地人脸库初始化
        FaceServer.getInstance().init(this);
        promptDialog = new PromptDialog(this);
        initView();

        //tab1
        tab_iv1 = (ImageView) findViewById(R.id.tab_iv1);
        tab_tv1 = (TextView) findViewById(R.id.tab_tv1);
        tab_line_1 = (TextView) findViewById(R.id.tab_line_1);
        //tab2
        tab_iv2 = (ImageView) findViewById(R.id.tab_iv2);
        tab_tv2 = (TextView) findViewById(R.id.tab_tv2);
        tab_line_2 = (TextView) findViewById(R.id.tab_line_2);
        //tab3
        tab_iv3 = (ImageView) findViewById(R.id.tab_iv3);
        tab_tv3 = (TextView) findViewById(R.id.tab_tv3);
        tab_line3 = (TextView) findViewById(R.id.tab_line_3);
        //tab4
        tab_iv4 = (ImageView) findViewById(R.id.tab_iv4);
        tab_tv4 = (TextView) findViewById(R.id.tab_tv4);


        live_tv = (TextView) findViewById(R.id.live_tv);//活体检测成功
        tips_tv = (TextView) findViewById(R.id.tips_tv);//提示信息
        faceEyes_iv = (ImageView) findViewById(R.id.faceEyes_iv);//眨眼
        faceHead_iv = (ImageView) findViewById(R.id.faceHead_iv);//摇头
        faceMouth_iv = (ImageView) findViewById(R.id.faceMouth_iv);//张嘴
    }


    private void initView() {
        previewView = findViewById(R.id.texture_preview);
        //在布局结束后才做初始化操作
        previewView.getViewTreeObserver().addOnGlobalLayoutListener(this);

        faceRectView = findViewById(R.id.face_rect_view);
        switchLivenessDetect = findViewById(R.id.switch_liveness_detect);
        switchLivenessDetect.setChecked(livenessDetect);
        switchLivenessDetect.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                livenessDetect = isChecked;
            }
        });
        RecyclerView recyclerShowFaceInfo = findViewById(R.id.recycler_view_person);
        compareResultList = new ArrayList<>();
        adapter = new FaceSearchResultAdapter(compareResultList, this);
        recyclerShowFaceInfo.setAdapter(adapter);
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int spanCount = (int) (dm.widthPixels / (getResources().getDisplayMetrics().density * 100 + 0.5f));
        recyclerShowFaceInfo.setLayoutManager(new GridLayoutManager(this, spanCount));
        recyclerShowFaceInfo.setItemAnimator(new DefaultItemAnimator());
    }


    /**
     * 在{@link #previewView}第一次布局完成后，去除该监听，并且进行引擎和相机的初始化
     */
    @Override
    public void onGlobalLayout() {
        previewView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
        previewView.turnRound();
        //申请权限
        PermissionUtils.permission(FaceCameraActivity.this, PermissionConstants.CAMERA, PermissionConstants.STORAGE)
                .rationale(new PermissionUtils.OnRationaleListener() {
                    @Override
                    public void rationale(ShouldRequest shouldRequest) {
                        shouldRequest.again(true);
                    }
                }).callback(new PermissionUtils.FullCallback() {
            @Override
            public void onGranted(List<String> permissionsGranted) {
                /**
                 *  初始化引擎
                 *  初始化相机
                 */
                new Thread(){
                    @Override
                    public void run() {
                        super.run();
                        initEngine();
                        initCamera();
                    }
                }.start();
//				initEngine();
//				initCamera();
            }

            @Override
            public void onDenied(List<String> permissionsDeniedForever, List<String> permissionsDenied) {
                ToastUtil.show(FaceCameraActivity.this, "权限拒绝");
            }
        }).request();
    }

    /**
     * 初始化引擎
     */
    private void initEngine() {
        ftEngine = new FaceEngine();
        ftInitCode = ftEngine.init(this, DetectMode.ASF_DETECT_MODE_VIDEO, ConfigUtil.getFtOrient(this),
                16, MAX_DETECT_NUM, FaceEngine.ASF_FACE_DETECT);

        frEngine = new FaceEngine();
        frInitCode = frEngine.init(this, DetectMode.ASF_DETECT_MODE_IMAGE, DetectFaceOrientPriority.ASF_OP_0_ONLY,
                16, MAX_DETECT_NUM, FaceEngine.ASF_FACE_RECOGNITION);

        flEngine = new FaceEngine();
        flInitCode = flEngine.init(this, DetectMode.ASF_DETECT_MODE_IMAGE, DetectFaceOrientPriority.ASF_OP_0_ONLY,
                16, MAX_DETECT_NUM, FaceEngine.ASF_LIVENESS);


        VersionInfo versionInfo = new VersionInfo();
        ftEngine.getVersion(versionInfo);
        Log.i(TAG, "initEngine:  init: " + ftInitCode + "  version:" + versionInfo);

        if (ftInitCode != ErrorInfo.MOK) {
            String error = getString(R.string.specific_engine_init_failed, "ftEngine", ftInitCode);
            Log.i(TAG, "initEngine: " + error);
            ToastUtil.show(FaceCameraActivity.this, error);
        }
        if (frInitCode != ErrorInfo.MOK) {
            String error = getString(R.string.specific_engine_init_failed, "frEngine", frInitCode);
            Log.i(TAG, "initEngine: " + error);
            ToastUtil.show(FaceCameraActivity.this, error);
        }
        if (flInitCode != ErrorInfo.MOK) {
            String error = getString(R.string.specific_engine_init_failed, "flEngine", flInitCode);
            Log.i(TAG, "initEngine: " + error);
            ToastUtil.show(FaceCameraActivity.this, error);
        }
    }

    /**
     * 销毁引擎，faceHelper中可能会有特征提取耗时操作仍在执行，加锁防止crash
     */
    private void unInitEngine() {
        if (ftInitCode == ErrorInfo.MOK && ftEngine != null) {
            synchronized (ftEngine) {
                int ftUnInitCode = ftEngine.unInit();
                Log.i(TAG, "unInitEngine: " + ftUnInitCode);
            }
        }
        if (frInitCode == ErrorInfo.MOK && frEngine != null) {
            synchronized (frEngine) {
                int frUnInitCode = frEngine.unInit();
                Log.i(TAG, "unInitEngine: " + frUnInitCode);
            }
        }
        if (flInitCode == ErrorInfo.MOK && flEngine != null) {
            synchronized (flEngine) {
                int flUnInitCode = flEngine.unInit();
                Log.i(TAG, "unInitEngine: " + flUnInitCode);
            }
        }
    }

    protected void onDestroy() {

        if (cameraHelper != null) {
            cameraHelper.release();
            cameraHelper = null;
        }
        unInitEngine();
        if (faceHelper != null) {
            ConfigUtil.setTrackedFaceCount(this, faceHelper.getTrackedFaceCount());
            faceHelper.release();
            faceHelper = null;
        }
        if (getFeatureDelayedDisposables != null) {
            getFeatureDelayedDisposables.clear();
        }
        if (delayFaceTaskCompositeDisposable != null) {
            delayFaceTaskCompositeDisposable.clear();
        }
        FaceServer.getInstance().unInit();
        super.onDestroy();
    }


    private void notifyBottom(int type) {
        if (type == 0) {/**活体检测成功**/
            //1
            tab_iv1.setImageResource(R.mipmap.tab_iv_one_blue);
            tab_tv1.setTextColor(ContextCompat.getColor(FaceCameraActivity.this, R.color.blue_44c1ff));
            tab_line_1.setBackgroundColor(ContextCompat.getColor(FaceCameraActivity.this, R.color.blue_44c1ff));
            //2
            tab_iv2.setImageResource(R.mipmap.tab_iv_two_gray);
            tab_tv2.setTextColor(ContextCompat.getColor(FaceCameraActivity.this, R.color.gray));
            tab_line_2.setBackgroundColor(ContextCompat.getColor(FaceCameraActivity.this, R.color.gray));
            //3
            tab_iv3.setImageResource(R.mipmap.tab_iv_three_gray);
            tab_tv3.setTextColor(ContextCompat.getColor(FaceCameraActivity.this, R.color.gray));
//            tab_line3.setBackgroundColor(ContextCompat.getColor(FaceCameraActivity.this, R.color.gray));
//            //4
//            tab_iv4.setImageResource(R.mipmap.tab_iv_four_gray);
//            tab_tv4.setTextColor(ContextCompat.getColor(FaceCameraActivity.this, R.color.gray));
        } else if (type == 1) {/**活体成---》人脸识别中**/
            //1
            tab_iv1.setImageResource(R.mipmap.tab_iv_one_blue);
            tab_tv1.setTextColor(ContextCompat.getColor(FaceCameraActivity.this, R.color.blue_44c1ff));
            tab_line_1.setBackgroundColor(ContextCompat.getColor(FaceCameraActivity.this, R.color.blue_44c1ff));
            //2
            tab_iv2.setImageResource(R.mipmap.tab_iv_two_blue);
            tab_tv2.setTextColor(ContextCompat.getColor(FaceCameraActivity.this, R.color.blue_44c1ff));
            tab_line_2.setBackgroundColor(ContextCompat.getColor(FaceCameraActivity.this, R.color.gray));
            //3
            tab_iv3.setImageResource(R.mipmap.tab_iv_three_gray);
            tab_tv3.setTextColor(ContextCompat.getColor(FaceCameraActivity.this, R.color.gray));
//            tab_line3.setBackgroundColor(ContextCompat.getColor(FaceCameraActivity.this, R.color.gray));
//            //4
//            tab_iv4.setImageResource(R.mipmap.tab_iv_four_gray);
//            tab_tv4.setTextColor(ContextCompat.getColor(FaceCameraActivity.this, R.color.gray));







        } else if (type == 2) {/**人脸识别中成功--》安全帽检测中**/
            //1
            tab_iv1.setImageResource(R.mipmap.tab_iv_one_blue);
            tab_tv1.setTextColor(ContextCompat.getColor(FaceCameraActivity.this, R.color.blue_44c1ff));
            tab_line_1.setBackgroundColor(ContextCompat.getColor(FaceCameraActivity.this, R.color.blue_44c1ff));
            //2
            tab_iv2.setImageResource(R.mipmap.tab_iv_two_blue);
            tab_tv2.setTextColor(ContextCompat.getColor(FaceCameraActivity.this, R.color.blue_44c1ff));
            tab_line_2.setBackgroundColor(ContextCompat.getColor(FaceCameraActivity.this, R.color.blue_44c1ff));
            //3
            tab_iv3.setImageResource(R.mipmap.tab_iv_three_blue);
            tab_tv3.setTextColor(ContextCompat.getColor(FaceCameraActivity.this, R.color.blue_44c1ff));
            tab_line3.setBackgroundColor(ContextCompat.getColor(FaceCameraActivity.this, R.color.gray));
            //4
            tab_iv4.setImageResource(R.mipmap.tab_iv_four_gray);
            tab_tv4.setTextColor(ContextCompat.getColor(FaceCameraActivity.this, R.color.gray));
        } else if (type == -1) {//归零
            //1
            tab_iv1.setImageResource(R.mipmap.tab_iv_one_gray);
            tab_tv1.setTextColor(ContextCompat.getColor(FaceCameraActivity.this, R.color.gray));
            tab_line_1.setBackgroundColor(ContextCompat.getColor(FaceCameraActivity.this, R.color.gray));
            //2
            tab_iv2.setImageResource(R.mipmap.tab_iv_two_gray);
            tab_tv2.setTextColor(ContextCompat.getColor(FaceCameraActivity.this, R.color.gray));
            tab_line_2.setBackgroundColor(ContextCompat.getColor(FaceCameraActivity.this, R.color.gray));
            //3
            tab_iv3.setImageResource(R.mipmap.tab_iv_three_gray);
            tab_tv3.setTextColor(ContextCompat.getColor(FaceCameraActivity.this, R.color.gray));
//            tab_line3.setBackgroundColor(ContextCompat.getColor(FaceCameraActivity.this, R.color.gray));
            //4
//            tab_iv4.setImageResource(R.mipmap.tab_iv_four_gray);
//            tab_tv4.setTextColor(ContextCompat.getColor(FaceCameraActivity.this, R.color.gray));

            faceEyes_iv.setVisibility(View.GONE);
            faceHead_iv.setVisibility(View.GONE);
            faceMouth_iv.setVisibility(View.GONE);

        }
    }

    /**上传Base64***/
    private void UpdateImgBase64() {
        promptDialog.showLoading("正在请求");
        OneToNPostBean bean = new OneToNPostBean();
        String pathName = FaceServer.SELF_DEFAULT_SAVE_FILE + File.separator + "imgs" + File.separator + "face_live" + ".jpg";
        String imgBase64 = ImageUtils.BitmapToString(pathName);
        bean.setImgBase64(imgBase64);
        bean.setLibraryId("2");
        String jsonStr = GsonUtil.GsonString(bean);
        LogUtils.i("jsonStr ===上传活体请求参数====" + jsonStr);
        //post请求body
        RequestBody requestBody = RequestBody.create(MediaType.parse(
                "application/json"), jsonStr);
        OkGo.<String>post(API.BASE_URL + API.UPDATE_FACE_LIVE)
                .headers("accessToken", MyApplication.accessToken)
                .upRequestBody(requestBody)
                .execute(new StringCallback() {
                    @Override
                    public void onSuccess(Response<String> response) {
                        Toast.makeText(FaceCameraActivity.this, "1:N接口请求成功=="+response.body(), Toast.LENGTH_SHORT).show();
                        OneToNRespon bean1 = GsonUtil.GsonToBean(response.body(), OneToNRespon.class);
                        OneToNRespon.ResultBean resultBean = bean1.getResult();//result
                        OneToNRespon.TargetVoBean targetVoBean = bean1.getTargetVo();//targetVo
                        if (resultBean == null || targetVoBean == null) {
                            promptDialog.dismissImmediately();
							startActivity(new Intent(FaceCameraActivity.this, OneToNResultActivity.class));
                        } else {
							String targetId = targetVoBean.getTargetId();
							if (MyApplication.userId == targetId) {/***如果id相等**/
								/**检测是否佩戴安全帽**/
								TestScurityCap();
							} else {
							    promptDialog.dismissImmediately();
								startActivity(new Intent(FaceCameraActivity.this, OneToNResultActivity.class));
							}
						}

                    }

                    @Override
                    public void onError(Response<String> response) {
                        super.onError(response);
                        promptDialog.dismissImmediately();
                        ToastUtil.show(FaceCameraActivity.this, API.NETWORK_ERROR);


                    }
                });

    }


	/**
	 * 检测是否带安全帽
	 */
	private void TestScurityCap() {
		/**
		 * 人脸识别成功, 等待2s,调用安全帽检测接口
		 */
		notifyBottom(2);



		CheckOneImgQualityRequest bean = new CheckOneImgQualityRequest();
		String pathName = FaceServer.SELF_DEFAULT_SAVE_FILE + File.separator + "imgs" + File.separator + "face_live" + ".jpg";
		String imgBase64 = ImageUtils.BitmapToString(pathName);
		bean.setImgBase64(imgBase64);
		String jsonStr = GsonUtil.GsonString(bean);
		LogUtils.i("jsonStr ===检测是否带安全帽====" + jsonStr);
		//post请求body
		RequestBody requestBody = RequestBody.create(MediaType.parse(
				"application/json"), jsonStr);
		OkGo.<String>post(API.BASE_URL + API.FACE_SECURITY_MAO)
				.headers("accessToken", MyApplication.accessToken)
				.upRequestBody(requestBody)
				.execute(new StringCallback() {
					@Override
					public void onSuccess(Response<String> response) {
						promptDialog.dismissImmediately();
						String respon = response.body();

						Toast.makeText(FaceCameraActivity.this, "安全帽检测接口请求成功=="+respon, Toast.LENGTH_LONG).show();
						String status = "0";
						if (!respon.contains("st_helmet_style_type_none")) {//不包含是“已配戴
							status = "0";
						} else {//未佩戴
							status = "1";
						}

						Intent intent = new Intent(FaceCameraActivity.this, SecurityCapActivity.class);
						intent.putExtra("status", status);
						startActivity(intent);
					}

					@Override
					public void onError(Response<String> response) {
						super.onError(response);
						promptDialog.dismissImmediately();
						ToastUtil.show(FaceCameraActivity.this, API.NETWORK_ERROR);
					}
				});



	}



	private void initCamera() {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        final FaceListener faceListener = new FaceListener() {
            @Override
            public void onFail(Exception e) {
                Log.e(TAG, "onFail: " + e.getMessage());
            }

            //请求FR的回调
            @Override
            public void onFaceFeatureInfoGet(@Nullable final FaceFeature faceFeature, final Integer requestId, final Integer errorCode) {
                //FR成功
                if (faceFeature != null) {
                    int facePreviewDistance = facePreviewRight - facePreviewLeft;/***获取人脸框宽度****/
//                    Log.i(TAG, "onPreview: fr end = " + System.currentTimeMillis() + " trackId = " + requestId);
                    Integer liveness = livenessMap.get(requestId);
                    //不做活体检测的情况，直接搜索
                    if (!livenessDetect) {
                        searchFace(faceFeature, requestId);
                    }
                    //活体检测通过，搜索特征
                    else if (liveness != null && liveness == LivenessInfo.ALIVE) {
                        if (facePreviewDistance >= MyApplication.minFaceWidth) {//如果大于等于最小人脸宽度
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (faceAction == 0) {
                                        notifyBottom(0);
                                        faceAction++;//1

                                        if (registerStatus == REGISTER_STATUS_DONE) {
                                            registerStatus = REGISTER_STATUS_READY;
                                        }
//                                        faceActionTime = System.currentTimeMillis();

                                    }

                                }
                            });

//                            searchFace(faceFeature, requestId);
                        } else {
                            requestFeatureStatusMap.put(requestId, RequestFeatureStatus.FAILED);
                            if (faceHelper == null) {} else {
                                faceHelper.setName(requestId, "VISITOR " + requestId);
                            }
                            retryRecognizeDelayed(requestId);
                        }
                    }
                    //活体检测未出结果，或者非活体，延迟执行该函数
                    else {
                        if (requestFeatureStatusMap.containsKey(requestId)) {
                            Observable.timer(WAIT_LIVENESS_INTERVAL, TimeUnit.MILLISECONDS)
                                    .subscribe(new io.reactivex.Observer<Long>() {
                                        Disposable disposable;

                                        @Override
                                        public void onSubscribe(Disposable d) {
                                            disposable = d;
                                            getFeatureDelayedDisposables.add(disposable);
                                        }

                                        @Override
                                        public void onNext(Long aLong) {
                                            onFaceFeatureInfoGet(faceFeature, requestId, errorCode);
                                        }

                                        @Override
                                        public void onError(Throwable e) {

                                        }

                                        @Override
                                        public void onComplete() {
                                            getFeatureDelayedDisposables.remove(disposable);
                                        }
                                    });
                        }
                    }

                }
                //特征提取失败
                else {
                    if (increaseAndGetValue(extractErrorRetryMap, requestId) > MAX_RETRY_TIME) {
                        extractErrorRetryMap.put(requestId, 0);

                        String msg;
                        // 传入的FaceInfo在指定的图像上无法解析人脸，此处使用的是RGB人脸数据，一般是人脸模糊
                        if (errorCode != null && errorCode == ErrorInfo.MERR_FSDK_FACEFEATURE_LOW_CONFIDENCE_LEVEL) {
                            msg = getString(R.string.low_confidence_level);
                        } else {
                            msg = "ExtractCode:" + errorCode;
                        }
                        if (faceHelper == null) {} else {/**未通过**/
//                            faceHelper.setName(requestId, getString(R.string.recognize_failed_notice, msg));
                        }
                        // 在尝试最大次数后，特征提取仍然失败，则认为识别未通过
                        requestFeatureStatusMap.put(requestId, RequestFeatureStatus.FAILED);
                        retryRecognizeDelayed(requestId);
                    } else {
                        requestFeatureStatusMap.put(requestId, RequestFeatureStatus.TO_RETRY);
                    }
                }
            }

            @Override
            public void onFaceLivenessInfoGet(@Nullable LivenessInfo livenessInfo, final Integer requestId, Integer errorCode) {
                if (livenessInfo != null) {
                    int liveness = livenessInfo.getLiveness();
                    livenessMap.put(requestId, liveness);
                    // 非活体，重试
                    if (liveness == LivenessInfo.NOT_ALIVE) {
                        if (faceHelper == null) {} else {
                            faceHelper.setName(requestId, getString(R.string.recognize_failed_notice, "NOT_ALIVE"));
                        }
                        // 延迟 FAIL_RETRY_INTERVAL 后，将该人脸状态置为UNKNOWN，帧回调处理时会重新进行活体检测
                        retryLivenessDetectDelayed(requestId);
                    }
                } else {
                    if (increaseAndGetValue(livenessErrorRetryMap, requestId) > MAX_RETRY_TIME) {
                        livenessErrorRetryMap.put(requestId, 0);
                        String msg;
                        // 传入的FaceInfo在指定的图像上无法解析人脸，此处使用的是RGB人脸数据，一般是人脸模糊
                        if (errorCode != null && errorCode == ErrorInfo.MERR_FSDK_FACEFEATURE_LOW_CONFIDENCE_LEVEL) {
                            msg = getString(R.string.low_confidence_level);
                        } else {
                            msg = "ProcessCode:" + errorCode;
                        }

                        if (faceHelper == null) {} else {
                            faceHelper.setName(requestId, getString(R.string.recognize_failed_notice, msg));
                        }
                        retryLivenessDetectDelayed(requestId);
                    } else {
                        livenessMap.put(requestId, LivenessInfo.UNKNOWN);
                    }
                }
            }


        };


        CameraListener cameraListener = new CameraListener() {
            @Override
            public void onCameraOpened(Camera camera, int cameraId, int displayOrientation, boolean isMirror) {
                Camera.Size lastPreviewSize = previewSize;
                previewSize = camera.getParameters().getPreviewSize();

                /////////////////////////////////////////////////////

//在相机打开时，添加右上角的view用于显示原始数据和预览数据
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //将预览控件和预览尺寸比例保持一致，避免拉伸
//						{
//							ViewGroup.LayoutParams layoutParams = previewView.getLayoutParams();
//							//横屏
//							if (displayOrientation % 180 == 0) {
//								layoutParams.height = layoutParams.width * previewSize.height / previewSize.width;
//							}
//							//竖屏
//							else {
//								layoutParams.height = layoutParams.width * previewSize.width / previewSize.height;
//							}
//							previewView.setLayoutParams(layoutParams);
//						}
                        if (radiusSeekBar != null) {
                            return;
                        }
                        roundBorderView = new RoundBorderView(FaceCameraActivity.this);
//						roundBorderView.setVisibility(View.GONE);
                        ((RelativeLayout) previewView.getParent()).addView(roundBorderView, previewView.getLayoutParams());
//
                        radiusSeekBar = new SeekBar(FaceCameraActivity.this);
                        radiusSeekBar.setVisibility(View.GONE);
                        radiusSeekBar.setOnSeekBarChangeListener(FaceCameraActivity.this);

                        DisplayMetrics displayMetrics = new DisplayMetrics();
                        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

                        RelativeLayout.LayoutParams radiusSeekBarLayoutParams = new RelativeLayout.LayoutParams(
                                displayMetrics.widthPixels, displayMetrics.heightPixels / 4
                        );

//						radiusSeekBarLayoutParams.gravity = Gravity.BOTTOM;
                        radiusSeekBar.setLayoutParams(radiusSeekBarLayoutParams);
                        ((RelativeLayout) previewView.getParent()).addView(radiusSeekBar);
                        radiusSeekBar.post(new Runnable() {
                            @Override
                            public void run() {
                                radiusSeekBar.setProgress(radiusSeekBar.getMax());
                            }
                        });
                    }
                });

                /////////////////////////////////////////////////////




                drawHelper = new DrawHelper(previewSize.width, previewSize.height, previewView.getWidth(), previewView.getHeight(), displayOrientation
                        , cameraId, isMirror, false, false);
                Log.i(TAG, "onCameraOpened: " + drawHelper.toString());
                // 切换相机的时候可能会导致预览尺寸发生变化
                if (faceHelper == null ||
                        lastPreviewSize == null ||
                        lastPreviewSize.width != previewSize.width || lastPreviewSize.height != previewSize.height) {
                    Integer trackedFaceCount = null;
                    // 记录切换时的人脸序号
                    if (faceHelper != null) {
                        trackedFaceCount = faceHelper.getTrackedFaceCount();
                        faceHelper.release();
                    }
                    faceHelper = new FaceHelper.Builder()
                            .ftEngine(ftEngine)
                            .frEngine(frEngine)
                            .flEngine(flEngine)
                            .frQueueSize(MAX_DETECT_NUM)
                            .flQueueSize(MAX_DETECT_NUM)
                            .previewSize(previewSize)
                            .faceListener(faceListener)
                            .trackedFaceCount(trackedFaceCount == null ? ConfigUtil.getTrackedFaceCount(FaceCameraActivity.this.getApplicationContext()) : trackedFaceCount)
                            .build();
                }
            }


            @Override
            public void onPreview(final byte[] nv21, Camera camera) {
                if (faceRectView != null) {
                    faceRectView.clearFaceInfo();
                }
                List<FacePreviewInfo> facePreviewInfoList = faceHelper.onPreviewFrame(nv21);
                if (facePreviewInfoList != null && faceRectView != null && drawHelper != null) {
                    drawPreviewInfo(facePreviewInfoList);
                    if (facePreviewInfoList.size() > 0) {
                        facePreviewLeft = facePreviewInfoList.get(0).getFaceInfo().getRect().left;
                        facePreviewRight = facePreviewInfoList.get(0).getFaceInfo().getRect().right;
//                        LogUtil.e("left=====================" + facePreviewLeft);
//                        LogUtil.i("right=====================" + facePreviewRight);
                    }
                }
                registerFace(nv21, facePreviewInfoList);
                clearLeftFace(facePreviewInfoList);

                if (facePreviewInfoList != null && facePreviewInfoList.size() > 0 && previewSize != null) {
                    for (int i = 0; i < facePreviewInfoList.size(); i++) {
                        Integer status = requestFeatureStatusMap.get(facePreviewInfoList.get(i).getTrackId());
                        /**
                         * 在活体检测开启，在人脸识别状态不为成功或人脸活体状态不为处理中（ANALYZING）且不为处理完成（ALIVE、NOT_ALIVE）时重新进行活体检测
                         */
                        if (livenessDetect && (status == null || status != RequestFeatureStatus.SUCCEED)) {
                            Integer liveness = livenessMap.get(facePreviewInfoList.get(i).getTrackId());
                            if (liveness == null
                                    || (liveness != LivenessInfo.ALIVE && liveness != LivenessInfo.NOT_ALIVE && liveness != RequestLivenessStatus.ANALYZING)) {
                                livenessMap.put(facePreviewInfoList.get(i).getTrackId(), RequestLivenessStatus.ANALYZING);
                                faceHelper.requestFaceLiveness(nv21, facePreviewInfoList.get(i).getFaceInfo(), previewSize.width, previewSize.height, FaceEngine.CP_PAF_NV21, facePreviewInfoList.get(i).getTrackId(), LivenessType.RGB);
                            }
                        }
                        /**
                         * 对于每个人脸，若状态为空或者为失败，则请求特征提取（可根据需要添加其他判断以限制特征提取次数），
                         * 特征提取回传的人脸特征结果在{@link FaceListener#onFaceFeatureInfoGet(FaceFeature, Integer, Integer)}中回传
                         */
                        if (status == null
                                || status == RequestFeatureStatus.TO_RETRY) {
                            requestFeatureStatusMap.put(facePreviewInfoList.get(i).getTrackId(), RequestFeatureStatus.SEARCHING);
                            faceHelper.requestFaceFeature(nv21, facePreviewInfoList.get(i).getFaceInfo(), previewSize.width, previewSize.height, FaceEngine.CP_PAF_NV21, facePreviewInfoList.get(i).getTrackId());
//                            Log.i(TAG, "onPreview: fr start = " + System.currentTimeMillis() + " trackId = " + facePreviewInfoList.get(i).getTrackedFaceCount());
                        }
                    }
                } else {//人脸移开

                    faceAction = 0;
                    notifyBottom(-1);
                }
            }

            @Override
            public void onCameraClosed() {
                Log.i(TAG, "onCameraClosed: ");
            }

            @Override
            public void onCameraError(Exception e) {
                Log.i(TAG, "onCameraError: " + e.getMessage());
            }

            @Override
            public void onCameraConfigurationChanged(int cameraID, int displayOrientation) {
                if (drawHelper != null) {
                    drawHelper.setCameraDisplayOrientation(displayOrientation);
                }
                Log.i(TAG, "onCameraConfigurationChanged: " + cameraID + "  " + displayOrientation);
            }
        };

        cameraHelper = new CameraHelper.Builder()
//				.previewViewSize(new Point(previewView.getMeasuredWidth(), previewView.getMeasuredHeight()))
                .previewViewSize(new Point(previewView.getLayoutParams().width, previewView.getLayoutParams().height))
                .rotation(getWindowManager().getDefaultDisplay().getRotation())
                .specificCameraId(rgbCameraID != null ? rgbCameraID : Camera.CameraInfo.CAMERA_FACING_FRONT)
                .isMirror(false)
                .previewOn(previewView)
                .cameraListener(cameraListener)
                .build();
        cameraHelper.init();
        cameraHelper.start();
    }


    private void registerFace(final byte[] nv21, final List<FacePreviewInfo> facePreviewInfoList) {

        if (faceAction == 2) {
            long currentTime = System.currentTimeMillis();
            long subTime = currentTime - faceActionTime;
            if (subTime > API.WAIT_TIME_2000) {
                notifyBottom(1);//展示"人脸识别中"
                faceActionTime = System.currentTimeMillis();
                faceAction++;//3


                /**
                 * 请求人脸识别中接口
                 */
                //UpdateImgBase64
                UpdateImgBase64();
            }
        } /*else if (faceAction == 3) {
            long currentTime = System.currentTimeMillis();
            long subTime = currentTime - faceActionTime;
            if (subTime > API.WAIT_TIME_2000) {
                notifyBottom(2);//展示安全帽检测中
                faceActionTime = System.currentTimeMillis();
                faceAction++;//4
            }
        }*/


        if (registerStatus == REGISTER_STATUS_READY && facePreviewInfoList != null && facePreviewInfoList.size() > 0) {

            Observable.create(new ObservableOnSubscribe<Boolean>() {
                @Override
                public void subscribe(ObservableEmitter<Boolean> emitter) {

                    if (faceAction == 1) {//活体检测成功
                        registerStatus = REGISTER_STATUS_PROCESSING;
                        boolean success = FaceServer.getInstance().registerNv21(FaceCameraActivity.this, nv21.clone(), previewSize.width, previewSize.height,
                                facePreviewInfoList.get(0).getFaceInfo(), "registered " + faceHelper.getTrackedFaceCount(), "face" + "_" + "live");
                        emitter.onNext(success);

                    }


                }
            })
                    .subscribeOn(Schedulers.computation())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new io.reactivex.Observer<Boolean>() {
                        @Override
                        public void onSubscribe(Disposable d) {

                        }

                        @Override
                        public void onNext(Boolean success) {
                            String result = success ? "register success!" : "register failed!";
//                            ToastUtil.show(FaceCameraActivity.this, result);
                            registerStatus = REGISTER_STATUS_DONE;
                            if (result.equals("register success!")) {
                                if (faceAction == 1) {//活体成功--》展示人脸识别中
//                                   String pathName = FaceServer.SELF_DEFAULT_SAVE_FILE + File.separator + "imgs" + File.separator + "face_live" + ".jpg";
//                                   File imgFile = new File(pathName);
//                                   faceEyes_iv.setImageBitmap(BitmapFactory.decodeFile(imgFile.toString()));
                                   faceAction++;//2:展示"人脸识别中"
                                    faceActionTime = System.currentTimeMillis();/**活体检测成功，下载图片成功**/



                                }
                            }

                        }

                        @Override
                        public void onError(Throwable e) {
                            e.printStackTrace();
                            ToastUtil.show(FaceCameraActivity.this, "register failed!");
                            registerStatus = REGISTER_STATUS_DONE;
                        }

                        @Override
                        public void onComplete() {

                        }
                    });
        }
    }


    /**
     * 将map中key对应的value增1回传
     *
     * @param countMap map
     * @param key      key
     * @return 增1后的value
     */
    public int increaseAndGetValue(Map<Integer, Integer> countMap, int key) {
        if (countMap == null) {
            return 0;
        }
        Integer value = countMap.get(key);
        if (value == null) {
            value = 0;
        }
        countMap.put(key, ++value);
        return value;
    }

    /**
     * 延迟 FAIL_RETRY_INTERVAL 重新进行人脸识别
     *
     * @param requestId 人脸ID
     */
    private void retryRecognizeDelayed(final Integer requestId) {
        requestFeatureStatusMap.put(requestId, RequestFeatureStatus.FAILED);
        Observable.timer(FAIL_RETRY_INTERVAL, TimeUnit.MILLISECONDS)
                .subscribe(new io.reactivex.Observer<Long>() {
                    Disposable disposable;

                    @Override
                    public void onSubscribe(Disposable d) {
                        disposable = d;
                        delayFaceTaskCompositeDisposable.add(disposable);
                    }

                    @Override
                    public void onNext(Long aLong) {

                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onComplete() {
                        // 将该人脸特征提取状态置为FAILED，帧回调处理时会重新进行活体检测
                        if (faceHelper == null) {} else {
                            faceHelper.setName(requestId, Integer.toString(requestId));
                        }
                        requestFeatureStatusMap.put(requestId, RequestFeatureStatus.TO_RETRY);
                        delayFaceTaskCompositeDisposable.remove(disposable);
                    }
                });
    }

    /**
     * 延迟 FAIL_RETRY_INTERVAL 重新进行活体检测
     *
     * @param requestId 人脸ID
     */
    private void retryLivenessDetectDelayed(final Integer requestId) {
        Observable.timer(FAIL_RETRY_INTERVAL, TimeUnit.MILLISECONDS)
                .subscribe(new io.reactivex.Observer<Long>() {
                    Disposable disposable;

                    @Override
                    public void onSubscribe(Disposable d) {
                        disposable = d;
                        delayFaceTaskCompositeDisposable.add(disposable);
                    }

                    @Override
                    public void onNext(Long aLong) {

                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onComplete() {
                        // 将该人脸状态置为UNKNOWN，帧回调处理时会重新进行活体检测
                        if (livenessDetect) {
                            if (faceHelper != null) {} else {
                                faceHelper.setName(requestId, Integer.toString(requestId));
                            }
                        }
                        livenessMap.put(requestId, LivenessInfo.UNKNOWN);
                        delayFaceTaskCompositeDisposable.remove(disposable);
                    }
                });
    }

    private void drawPreviewInfo(List<FacePreviewInfo> facePreviewInfoList) {
        List<DrawInfo> drawInfoList = new ArrayList<>();
        for (int i = 0; i < facePreviewInfoList.size(); i++) {
            String name = faceHelper.getName(facePreviewInfoList.get(i).getTrackId());
            if (name != null && name.contains("100")) {
                name = null;
            }
            Integer liveness = livenessMap.get(facePreviewInfoList.get(i).getTrackId());
            Integer recognizeStatus = requestFeatureStatusMap.get(facePreviewInfoList.get(i).getTrackId());

            // 根据识别结果和活体结果设置颜色
            int color = RecognizeColor.COLOR_UNKNOWN;
            if (recognizeStatus != null) {
                if (recognizeStatus == RequestFeatureStatus.FAILED) {
                    color = RecognizeColor.COLOR_FAILED;
                }
                if (recognizeStatus == RequestFeatureStatus.SUCCEED) {
                    color = RecognizeColor.COLOR_SUCCESS;
                }
            }
            if (liveness != null && liveness == LivenessInfo.NOT_ALIVE) {
                color = RecognizeColor.COLOR_FAILED;
            }

            drawInfoList.add(new DrawInfo(drawHelper.adjustRect(facePreviewInfoList.get(i).getFaceInfo().getRect()),
                    GenderInfo.UNKNOWN, AgeInfo.UNKNOWN_AGE, liveness == null ? LivenessInfo.UNKNOWN : liveness, color,
                    name == null ? "" : name));
        }
        drawHelper.draw(faceRectView, drawInfoList);
    }

    /**
     * 删除已经离开的人脸
     *
     * @param facePreviewInfoList 人脸和trackId列表
     */
    private void clearLeftFace(List<FacePreviewInfo> facePreviewInfoList) {
        if (compareResultList != null) {
            for (int i = compareResultList.size() - 1; i >= 0; i--) {
                if (!requestFeatureStatusMap.containsKey(compareResultList.get(i).getTrackId())) {
                    compareResultList.remove(i);
                    adapter.notifyItemRemoved(i);
                }
            }
        }
        if (facePreviewInfoList == null || facePreviewInfoList.size() == 0) {
            requestFeatureStatusMap.clear();
            livenessMap.clear();
            livenessErrorRetryMap.clear();
            extractErrorRetryMap.clear();
            if (getFeatureDelayedDisposables != null) {
                getFeatureDelayedDisposables.clear();
            }
            return;
        }
        Enumeration<Integer> keys = requestFeatureStatusMap.keys();
        while (keys.hasMoreElements()) {
            int key = keys.nextElement();
            boolean contained = false;
            for (FacePreviewInfo facePreviewInfo : facePreviewInfoList) {
                if (facePreviewInfo.getTrackId() == key) {
                    contained = true;
                    break;
                }
            }
            if (!contained) {
                requestFeatureStatusMap.remove(key);
                livenessMap.remove(key);
                livenessErrorRetryMap.remove(key);
                extractErrorRetryMap.remove(key);
            }
        }


    }


    private void searchFace(final FaceFeature frFace, final Integer requestId) {
        Observable
                .create(new ObservableOnSubscribe<CompareResult>() {
                    @Override
                    public void subscribe(ObservableEmitter<CompareResult> emitter) {
//                        Log.i(TAG, "subscribe: fr search start = " + System.currentTimeMillis() + " trackId = " + requestId);
                        CompareResult compareResult = FaceServer.getInstance().getTopOfFaceLib(frFace);
//                        Log.i(TAG, "subscribe: fr search end = " + System.currentTimeMillis() + " trackId = " + requestId);
                        emitter.onNext(compareResult);

                    }
                })
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<CompareResult>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(CompareResult compareResult) {
                        if (compareResult == null || compareResult.getUserName() == null) {
                            requestFeatureStatusMap.put(requestId, RequestFeatureStatus.FAILED);
                            if (faceHelper == null) {} else {
                                faceHelper.setName(requestId, "VISITOR " + requestId);
                            }
                            retryRecognizeDelayed(requestId);
                            return;
                        }

//                        Log.i(TAG, "onNext: fr search get result  = " + System.currentTimeMillis() + " trackId = " + requestId + "  similar = " + compareResult.getSimilar());
                        LogUtils.e("匹配的特征值相似度===compareResult.getSimilar()===" + compareResult.getSimilar());
                        if ((compareResult.getSimilar() * 100) > MyApplication.faceSimilarity) {
                            boolean isAdded = false;
                            if (compareResultList == null) {
                                requestFeatureStatusMap.put(requestId, RequestFeatureStatus.FAILED);
                                if (faceHelper == null) {} else {
                                    faceHelper.setName(requestId, "VISITOR " + requestId);
                                }
                                return;
                            }
//                            for (CompareResult compareResult1 : compareResultList) {
//                                if (compareResult1.getTrackId() == requestId) {
//                                    isAdded = true;
//                                    break;
//                                }
//                            }
                            if (!isAdded) {
                                //对于多人脸搜索，假如最大显示数量为 MAX_DETECT_NUM 且有新的人脸进入，则以队列的形式移除
                                if (compareResultList.size() >= MAX_DETECT_NUM) {
                                    compareResultList.remove(0);
                                    adapter.notifyItemRemoved(0);
                                }
                                //添加显示人员时，保存其trackId
                                compareResult.setTrackId(requestId);
                                compareResultList.clear();
                                compareResultList.add(compareResult);
                                adapter.notifyItemInserted(compareResultList.size() - 1);
                                String compareName = compareResultList.get(0).getUserName();
                                LogUtils.e("compareName===识别到成功======" + compareName);

                            }

                            requestFeatureStatusMap.put(requestId, RequestFeatureStatus.SUCCEED);
                            if (faceHelper == null) {} else {
                                faceHelper.setName(requestId, compareResult.getUserName());
                            }
                            retryRecognizeDelayed(requestId);

                        } else {
                            if (faceHelper == null) {} else {
                                faceHelper.setName(requestId, getString(R.string.recognize_failed_notice, "NOT_REGISTERED"));
                            }
                            retryRecognizeDelayed(requestId);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        try {
                            if (requestId == null) {} else {
                                faceHelper.setName(requestId, getString(R.string.recognize_failed_notice, "NOT_REGISTERED"));
                                retryRecognizeDelayed(requestId);
                            }
                        } catch (Exception e1) {
                            e1.printStackTrace();
                        }
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }


    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        previewView.setRadius(progress * Math.min(previewView.getWidth(), previewView.getHeight()) / 2 / seekBar.getMax());
        previewView.turnRound();

        roundBorderView.setRadius(progress * Math.min(roundBorderView.getWidth(), roundBorderView.getHeight()) / 2 / seekBar.getMax());
        roundBorderView.turnRound();
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

}
