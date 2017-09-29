package cn.ac.iscas.xlab.droidfacedog.mvp.facesign;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.FaceDetector;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import cn.ac.iscas.xlab.droidfacedog.R;
import cn.ac.iscas.xlab.droidfacedog.RosConnectionService;
import cn.ac.iscas.xlab.droidfacedog.custom_views.FaceOverlayView;
import cn.ac.iscas.xlab.droidfacedog.entity.FaceResult;
import cn.ac.iscas.xlab.droidfacedog.util.ImageUtils;
import cn.ac.iscas.xlab.droidfacedog.util.Util;

import static android.content.Context.CAMERA_SERVICE;

/**
 * Created by lisongting on 2017/9/12.
 */

public class SignInFragment extends Fragment  implements SignInContract.View{

    public static final String TAG = "SignInFragment";
    public static final String TEXT_READY_TO_GO="准备出发";
    public static final String TEXT_ON_THE_WAY="正在前往目标点...";
    public static final int MAX_FACE_COUNT = 1;

    private LottieAnimationView lottieAnimationView;
    private TextView headTextView;
    private TextureView textureView;
    private SurfaceTexture surfaceTexture;
    private Surface surface;
    private FaceOverlayView faceOverlayView;

    private SignInContract.Presenter presenter;

    //与拍摄图像相关的成员变量
    private String cameraID;
    private Size previewSize;
    private CameraManager cameraManager;
    private CameraDevice cameraDevice;
    private CaptureRequest.Builder builder;
    private CameraCaptureSession cameraCaptureSession;
    private CameraDevice.StateCallback cameraStateCallback;
    private CameraCaptureSession.StateCallback sessionStateCallback;

    //用于定时识别人脸的Timer
    private Timer mDetectTimer;
    private TimerTask mDetectFaceTask;

    //用来存放检测到的人脸
    private FaceDetector.Face[] mDetectedFaces;
    private FaceResult[] mFaces;
    private FaceResult[] mPreviousFaces;
    private FaceDetector mFaceDetector;

    //要发送给人脸识别服务器的人脸bitmap
    private Bitmap mFaceBitmap;

    //识别到的人脸的id（不是注册在服务端的ID）,初始为0。
    private int mPersonId =0;
    //比例因子，将检测到的原始人脸图像按此比例缩小，以此可以加快FaceDetect的检测速度
    private double mScale = 0.15;
    //用来标记每个人脸共有几张图像,key是人脸的id，value是当前采集到的图像张数
    private SparseIntArray mFacesCountMap;
    private boolean isDetecting  = false;
    private HandlerThread backGroundThread1;
    private Handler backGroundHandler1;
    
    public SignInFragment(){}

    public static SignInFragment newInstance() {
        return new SignInFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_sign, container, false);
        textureView = (TextureView)v.findViewById(R.id.texture_view);
        headTextView = (TextView) v.findViewById(R.id.head_text_view);
        lottieAnimationView = (LottieAnimationView) v.findViewById(R.id.lottie_view);
        return v;
    }

    @Override
    public void onStart() {
        super.onStart();
        initView();

        mFaces = new FaceResult[MAX_FACE_COUNT];
        mPreviousFaces = new FaceResult[MAX_FACE_COUNT];
        mDetectedFaces = new FaceDetector.Face[MAX_FACE_COUNT];
        for (int i = 0; i < MAX_FACE_COUNT; i++) {
            mFaces[i] = new FaceResult();
            mPreviousFaces[i] = new FaceResult();
        }
        mFacesCountMap = new SparseIntArray();

        presenter = new SignInPresenter(this,getContext());
        presenter.start();
    }

    @Override
    public void initView() {
        headTextView.setText(TEXT_READY_TO_GO);

        faceOverlayView = new FaceOverlayView(getContext());
        RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.MATCH_PARENT);
        getActivity().addContentView(faceOverlayView,params );
//        faceOverlayView.setFront(true);
//        faceOverlayView.setDisplayOrientation(getActivity().getWindowManager().getDefaultDisplay().getRotation());
    }

    @Override
    public void onResume() {
        super.onResume();

        backGroundThread1 = new HandlerThread("handlerThread");
        backGroundThread1.start();
        backGroundHandler1 = new Handler(backGroundThread1.getLooper());
    }

    private void initCallbackAndListeners() {
        //为SurfaceView设置监听器
        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            //当把摄像头关闭后再次打开，这个方法并不会多次触发
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface1, int widthSurface, int heightSurface) {
                Log.i(TAG, "TextureView.SurfaceTextureListener -- onSurfaceTextureAvailable()");
                surfaceTexture = surface1;
                //这里表示当第一次开启摄像头时，开启预览并检测人脸
                startPreview();
                startTimerTask();

            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                Log.v(TAG, "TextureView.SurfaceTextureListener -- onSurfaceTextureSizeChanged()");
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                Log.v(TAG, "TextureView.SurfaceTextureListener -- onSurfaceTextureDestroyed()");
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
                Log.v(TAG, "TextureView -- onSurfaceTextureUpdated");
            }
        });

        cameraStateCallback = new CameraDevice.StateCallback() {
            @Override
            public void onOpened(@NonNull CameraDevice camera) {
                Log.i(TAG, "CameraDevice.StateCallback -- onOpened()");
                cameraDevice = camera;
                //这里表示，当再次开启摄像头时，开启预览模式并启动人脸检测线程
                if (cameraCaptureSession!=null) {
                    startPreview();
                    startTimerTask();
                }
            }

            @Override
            public void onDisconnected(@NonNull CameraDevice camera) {
                Log.i(TAG, "CameraDevice.StateCallback -- onDisconnected()");
                if (cameraDevice != null) {
                    cameraDevice.close();
                    cameraDevice = null;
                }
            }

            @Override
            public void onError(@NonNull CameraDevice camera, int error) {
                Log.i(TAG, "CameraDevice.StateCallback -- onError()");
            }
        };

        sessionStateCallback = new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(@NonNull CameraCaptureSession session) {
                Log.i(TAG, "CameraCaptureSession.StateCallback -- onConfigured()");
                if (cameraDevice == null) {
                    return;
                }
                cameraCaptureSession = session;
                //设置自动对焦
                builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                //设置自动曝光
                builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                //显示预览
                CaptureRequest captureRequest = builder.build();
                try {
                    //$ process in background
                    cameraCaptureSession.setRepeatingRequest(captureRequest, new CameraCaptureSession.CaptureCallback() {
                        @Override
                        public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request,
                                                     long timestamp, long frameNumber) {
                            Log.v(TAG, "CameraCaptureSession -- onCaptureStarted");
                            super.onCaptureStarted(session, request, timestamp, frameNumber);
                        }

                        @Override
                        public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request,
                                                        @NonNull CaptureResult partialResult) {
                            Log.v(TAG, "CameraCaptureSession -- onCaptureProgressed");
                            super.onCaptureProgressed(session, request, partialResult);
                        }

                        @Override
                        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request,
                                                       @NonNull TotalCaptureResult result) {
                            Log.v(TAG, "CameraCaptureSession -- onCaptureCompleted");
                            super.onCaptureCompleted(session, request, result);
                        }
                    }, backGroundHandler1);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                Log.v(TAG, "CameraCaptureSession.StateCallback -- onConfigureFailed()");
            }
        };


    }

    private void initCamera() {
        cameraID = "" + CameraCharacteristics.LENS_FACING_BACK;
        cameraManager = (CameraManager) getContext().getSystemService(CAMERA_SERVICE);
        try {
            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getContext(), "请授权使用照相机", Toast.LENGTH_SHORT).show();
                return;
            }
            //$ process in background
            cameraManager.openCamera(cameraID, cameraStateCallback, backGroundHandler1);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    public void startPreview(){
        try {
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraID);
            StreamConfigurationMap configMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            int width = textureView.getWidth();
            int height = textureView.getHeight();
//            faceOverlayView.setPreviewWidth(width);
//            faceOverlayView.setPreviewHeight(height);
            //设置一个合适的预览尺寸，防止图像拉伸
            previewSize = Util.getPreferredPreviewSize(configMap.getOutputSizes(ImageFormat.JPEG), width, height);
            surfaceTexture.setDefaultBufferSize(previewSize.getWidth(),previewSize.getHeight());
            Log.i(TAG, "previewSize info:" + previewSize.getWidth() + "x" + previewSize.getHeight());
            if (surface == null) {
                surface = new Surface(surfaceTexture);
            }
            builder =cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            builder.addTarget(surface);
            Log.i(TAG, "mTextureView info:" + textureView.getWidth() + "x" + textureView.getHeight());
            //process in background
            cameraDevice.createCaptureSession(Arrays.asList(surface),sessionStateCallback,backGroundHandler1);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void startCamera() {
        initCallbackAndListeners();
        initCamera();

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {

                textureView.setVisibility(View.VISIBLE);
                headTextView.setVisibility(View.INVISIBLE);
                lottieAnimationView.cancelAnimation();
                lottieAnimationView.setVisibility(View.INVISIBLE);
//                faceOverlayView.setVisibility(View.VISIBLE);
            }
        });


    }

    @TargetApi(23)
    @Override
    public void closeCamera() {

        textureView.setVisibility(View.GONE);

        if (mDetectTimer != null) {
            mDetectTimer.cancel();
        }
        if (cameraCaptureSession != null && isDetecting) {
            try {
                cameraCaptureSession.abortCaptures();
                cameraCaptureSession.stopRepeating();
                cameraCaptureSession.close();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
        if (cameraDevice != null) {
            cameraDevice.close();
        }
    }

    private void startTimerTask(){
        isDetecting = true;
        mDetectTimer = new Timer();
        //在这个定时任务中，不断的检测界面中的人脸
        mDetectFaceTask = new TimerTask() {
            @Override
            public void run() {
                Bitmap face = textureView.getBitmap();
                if (face != null&&isDetecting) {
                    Log.i(TAG, "Bitmap in mTextureView :" + face.getWidth() +
                            "x" + face.getHeight()+",Config:"+face.getConfig());

                    //原先的bitmap格式是ARGB_8888，以下的步骤是把格式转换为RGB_565
                    ByteArrayOutputStream bout = new ByteArrayOutputStream();
                    face.compress(Bitmap.CompressFormat.JPEG, 100, bout);
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inPreferredConfig = Bitmap.Config.RGB_565;
                    Bitmap RGBFace = BitmapFactory.decodeStream(new ByteArrayInputStream(bout.toByteArray()), null, options);

                    Bitmap smallRGBFace = Bitmap.createScaledBitmap(RGBFace, (int)(RGBFace.getWidth()*mScale)
                            , (int)(RGBFace.getHeight()*mScale), false);

                    //创建一个人脸检测器，MAX_FACE参数表示最多识别MAX_FACE张人脸
                    if (mFaceDetector == null) {
                        mFaceDetector = new FaceDetector(smallRGBFace.getWidth(), smallRGBFace.getHeight(), MAX_FACE_COUNT);
                    }
                    //findFaces中传入的bitmap格式必须为RGB_565
                    int found = mFaceDetector.findFaces(smallRGBFace, mDetectedFaces);

                    Log.i(TAG, "RGBFace:" + RGBFace.getWidth() + "x" + RGBFace.getHeight() + "," + RGBFace.getConfig());
                    Log.i(TAG, "smallRGBFace:" + smallRGBFace.getWidth() + "x" + smallRGBFace.getHeight() + ","
                            + smallRGBFace.getConfig());
                    Log.i(TAG, "found:" + found+" face(s)");

                    for(int i=0;i<MAX_FACE_COUNT;i++) {
                        if (mDetectedFaces[i] == null) {
                            mFaces[i].clear();
                        } else {
                            PointF mid = new PointF();
                            mDetectedFaces[i].getMidPoint(mid);

                            //前面为了方便检测人脸将图片缩小，现在按比例还原
                            mid.x *= 1.0/mScale;
                            mid.y *= 1.0/mScale;
                            Log.i(TAG, textureView.getWidth() + "x" + textureView.getHeight());
                            Log.i(TAG, "mid pointF:" + mid.x + "," + mid.y);
                            float eyeDistance = mDetectedFaces[i].eyesDistance()*(float)(1.0/mScale);
                            float confidence = mDetectedFaces[i].confidence();
                            float pose = mDetectedFaces[i].pose(FaceDetector.Face.EULER_Y);
                            //mPersonId一开始是0
                            int personId = mPersonId;

                            //预先创建一个人脸矩形区域
                            RectF rectF = ImageUtils.getPreviewFaceRectF(mid, eyeDistance);

                            //如果人脸矩形区域大于一定面积，才采集图像
                            if (rectF.width() * rectF.height() > 50 * 60) {
                                for(int j=0;j<MAX_FACE_COUNT;j++) {
                                    //获取之前的Faces数据
                                    float eyesDisPre = mPreviousFaces[j].eyesDistance();
                                    PointF midPre = new PointF();
                                    mPreviousFaces[j].getMidPoint(midPre);

                                    //在一定区域内检查人脸是否移动过大，超出这个区域。
                                    RectF rectCheck = ImageUtils.getCheckFaceRectF(midPre, eyesDisPre);

                                    //如果没有当前人脸没有超过这个检查区域，说明该ID对应的人脸晃动程度小，则适合采集
                                    if (rectCheck.contains(mid.x, mid.y) &&
                                            (System.currentTimeMillis() - mPreviousFaces[j].getTime()) < 1000) {
                                        personId = mPreviousFaces[j].getId();
                                        break;
                                    }
                                }

                                mFaces[i].setFace(personId, mid, eyeDistance, confidence, pose, System.currentTimeMillis());
                                mPreviousFaces[i].set(mFaces[i].getId(), mFaces[i].getMidEye(),
                                        mFaces[i].eyesDistance(), mFaces[i].getConfidence(), mFaces[i].getPose(), mFaces[i].getTime());

                                //将采集到的人脸的帧数以key-value的形式放在一个map中
                                int tmpFrameCount = mFacesCountMap.get(personId) + 1;
                                if (tmpFrameCount < 2) {
                                    mFacesCountMap.put(personId, tmpFrameCount);
                                }
                                if (tmpFrameCount == 2) {
                                    mFaceBitmap = ImageUtils.cropFace(mFaces[i], RGBFace,0);
                                    if (mFaceBitmap != null) {
                                        presenter.recognize(mFaceBitmap);
                                        mFacesCountMap.clear();
                                    }
                                }
                            }
                        }
                    }
                    try {
                        bout.close();
                        face.recycle();
                        RGBFace.recycle();
                        smallRGBFace.recycle();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        mDetectTimer.schedule(mDetectFaceTask, 1000, 200);
    }

    @Override
    public void setPresenter(SignInContract.Presenter presenter) {
        this.presenter = presenter;
    }

    public void setRosServiceBinder(RosConnectionService.ServiceBinder binder){
        presenter.setServiceProxy(binder);
    }

    public void changeUiState(int state){
        switch (state) {
            case SignInContract.UI_STATE_ON_THE_WAY:
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        headTextView.setVisibility(View.VISIBLE);
                        lottieAnimationView.setVisibility(View.VISIBLE);
                        headTextView.setText(TEXT_ON_THE_WAY);
                        lottieAnimationView.playAnimation();
                        //faceOverlayView.setVisibility(View.INVISIBLE);
                        isDetecting = false;
                    }
                });
                break;
            case SignInContract.UI_STATE_READY:
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        headTextView.setVisibility(View.VISIBLE);
                        lottieAnimationView.setVisibility(View.VISIBLE);
                        headTextView.setText(TEXT_READY_TO_GO);
                        lottieAnimationView.cancelAnimation();
                        lottieAnimationView.setProgress(0);
                        textureView.setVisibility(View.INVISIBLE);
                        //faceOverlayView.setVisibility(View.INVISIBLE);
                        isDetecting = false;
                    }
                });
                break;
            default:
                break;
        }
    }

    @Override
    public void displayInfo(String str) {
        Toast.makeText(getContext(),str, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroy() {
        closeCamera();
//        if(isDetecting||cameraCaptureSession!=null){
//            mDetectTimer.cancel();
//            cameraCaptureSession.close();
//            cameraDevice.close();
//        }
        presenter.releaseMemory();
        super.onDestroy();
    }

}
