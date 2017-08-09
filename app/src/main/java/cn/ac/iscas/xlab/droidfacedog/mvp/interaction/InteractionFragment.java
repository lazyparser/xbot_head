package cn.ac.iscas.xlab.droidfacedog.mvp.interaction;

import android.Manifest;
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
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import cn.ac.iscas.xlab.droidfacedog.R;
import cn.ac.iscas.xlab.droidfacedog.RosConnectionService;
import cn.ac.iscas.xlab.droidfacedog.custom_views.WaveView;
import cn.ac.iscas.xlab.droidfacedog.entity.FaceResult;
import cn.ac.iscas.xlab.droidfacedog.util.ImageUtils;
import cn.ac.iscas.xlab.droidfacedog.util.Util;

import static android.content.Context.CAMERA_SERVICE;
import static cn.ac.iscas.xlab.droidfacedog.XBotFaceActivity.MAX_FACE_COUNT;

/**
 * Created by lisongting on 2017/8/7.
 */

public class InteractionFragment extends Fragment implements InteractionContract.View {

    public static final String TAG = "InteractionFragment";
    private WaveView waveView;
    private ImageView imageView;
    private TextView textCommentary;
    private TextureView textureView;
    private SurfaceTexture surfaceTexture;
    private Surface surface;

    private InteractionContract.Presenter presenter;

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
    //这个boolean表示将人脸发送给服务器后，当前是否正在等待优图服务器返回识别结果
    private boolean isWaitingRecogResult = false;

    //识别到的人脸的id（不是注册在服务端的ID）,初始为0。
    private int mPersonId =0;
    private long mTotalFrameCount = 0;
    //比例因子，将检测到的原始人脸图像按此比例缩小，以此可以加快FaceDetect的检测速度
    private double mScale = 0.2;
    //用来标记每个人脸共有几张图像,key是人脸的id，value是当前采集到的图像张数
    private SparseIntArray mFacesCountMap;

    public InteractionFragment() {}

    public static InteractionFragment newInstance(){
        return new InteractionFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_interaction, container, false);
        waveView = (WaveView) view.findViewById(R.id.id_wave_view);
        textureView = (TextureView) view.findViewById(R.id.texture_view);
        imageView = (ImageView) view.findViewById(R.id.talker_img);
        textCommentary = (TextView) view.findViewById(R.id.text_commentary);

        return view;
    }

    @Override
    public void initView() {

    }

    @Override
    public void onStart() {
        super.onStart();

        mFaces = new FaceResult[MAX_FACE_COUNT];
        mPreviousFaces = new FaceResult[MAX_FACE_COUNT];
        mDetectedFaces = new FaceDetector.Face[MAX_FACE_COUNT];
        for (int i = 0; i < MAX_FACE_COUNT; i++) {
            mFaces[i] = new FaceResult();
            mPreviousFaces[i] = new FaceResult();
        }
        mFacesCountMap = new SparseIntArray();

    }
    @Override
    public void onResume() {
        super.onResume();

        presenter = new InteractionPresenter(this,getContext());
        presenter.start();

        initCallbackAndListeners();
        setWaveViewEnable(false);

        initCamera();
        //开启三个定时任务
        startTimerTask();

        mDetectTimer.schedule(mDetectFaceTask, 0, 200);

    }

    //创建回调接口，初始化监听器
    private void initCallbackAndListeners() {
        //每次setOnClickListener会再次将View的clickable设置为true!
        waveView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (waveView.isWorking()) {
                    presenter.stopAiTalk();
                    waveView.endAnimation();
                } else {
                    presenter.startAiTalk();
                    waveView.startAnimation();
                }
            }
        });
        textCommentary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                presenter.stopAiTalk();
                presenter.startCommentary();
                setWaveViewEnable(false);
            }
        });

        //为SurfaceView设置监听器
        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                Log.i(TAG, "TextureView.SurfaceTextureListener -- onSurfaceTextureAvailable()");
                surfaceTexture = surface;
                startCamera();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                Log.i(TAG, "TextureView.SurfaceTextureListener -- onSurfaceTextureSizeChanged()");
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                Log.i(TAG, "TextureView.SurfaceTextureListener -- onSurfaceTextureDestroyed()");
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
                Log.v(TAG, "TextureView -- onSurfaceTextureUpdated");
                mTotalFrameCount++;
                //每过400帧数，将isWaitingRecogResult置为false，这样可以避免频繁发送人脸给服务器
                if (mTotalFrameCount % 400 == 0) {
                    isWaitingRecogResult = false;
                }
//                Log.i(TAG, "totalFrameCount:" + mTotalFrameCount);
            }
        });

        cameraStateCallback = new CameraDevice.StateCallback() {
            @Override
            public void onOpened(@NonNull CameraDevice camera) {
                Log.i(TAG, "CameraDevice.StateCallback -- onOpened()");
                cameraDevice = camera;

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
                    }, null);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                Log.i(TAG, "CameraCaptureSession.StateCallback -- onConfigureFailed()");
            }
        };
    }



    private void initCamera() {
        cameraID = "" + CameraCharacteristics.LENS_FACING_BACK;
        cameraManager = (CameraManager) getContext().getSystemService(CAMERA_SERVICE);
        try {
            if (ActivityCompat.checkSelfPermission(getContext(),Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getContext(), "请授权使用照相机", Toast.LENGTH_SHORT).show();
                return;
            }
            cameraManager.openCamera(cameraID, cameraStateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stopFaceDetectTask() {
        mDetectTimer.cancel();
    }

    private void startTimerTask(){

        mDetectTimer = new Timer();
        //在这个定时任务中，不断的检测界面中的人脸
        mDetectFaceTask = new TimerTask() {
            @Override
            public void run() {
                Bitmap face = textureView.getBitmap();
                if (face != null) {
                    Log.v(TAG, "Bitmap in mTextureView :" + face.getWidth() +
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
                    mFaceDetector = new FaceDetector(smallRGBFace.getWidth(), smallRGBFace.getHeight(), MAX_FACE_COUNT);
                    //findFaces中传入的bitmap格式必须为RGB_565
                    int found = mFaceDetector.findFaces(smallRGBFace, mDetectedFaces);

                    Log.v(TAG, "RGBFace:" + RGBFace.getWidth() + "x" + RGBFace.getHeight() + "," + RGBFace.getConfig());
                    Log.v(TAG, "smallRGBFace:" + smallRGBFace.getWidth() + "x" + smallRGBFace.getHeight() + ","
                            + smallRGBFace.getConfig());
                    Log.v(TAG, "found:" + found+" face(s)");

                    for(int i=0;i<MAX_FACE_COUNT;i++) {
                        if (mDetectedFaces[i] == null) {
                            mFaces[i].clear();
                        } else {
                            PointF mid = new PointF();
                            mDetectedFaces[i].getMidPoint(mid);

                            //前面为了方便检测人脸将图片缩小，现在按比例还原
                            mid.x *= 1.0/mScale;
                            mid.y *= 1.0/mScale;
                            Log.v(TAG, textureView.getWidth() + "x" + textureView.getHeight());
                            Log.v(TAG, "mid pointF:" + mid.x + "," + mid.y);
                            float eyeDistance = mDetectedFaces[i].eyesDistance()*(float)(1.0/mScale);
                            float confidence = mDetectedFaces[i].confidence();
                            float pose = mDetectedFaces[i].pose(FaceDetector.Face.EULER_Y);
                            //mPersonId一开始是0
                            int personId = mPersonId;

                            //预先创建一个人脸矩形区域
                            RectF rectF = ImageUtils.getPreviewFaceRectF(mid, eyeDistance);

                            //如果人脸矩形区域大于一定面积，才采集图像
                            if (rectF.width() * rectF.height() > 90 * 60) {
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

                                if (mPersonId == personId) {
                                    mPersonId++;
                                }
                                mFaces[i].setFace(personId, mid, eyeDistance, confidence, pose, System.currentTimeMillis());
                                mPreviousFaces[i].set(mFaces[i].getId(), mFaces[i].getMidEye(),
                                        mFaces[i].eyesDistance(), mFaces[i].getConfidence(), mFaces[i].getPose(), mFaces[i].getTime());
                                
                                //将采集到的人脸的帧数以key-value的形式放在一个map中
                                int tmpFrameCount = mFacesCountMap.get(personId) + 1;
                                if (tmpFrameCount < 5) {
                                    mFacesCountMap.put(personId, tmpFrameCount);
                                }
                                if (tmpFrameCount == 5) {
                                    mFaceBitmap = ImageUtils.cropFace(mFaces[i], RGBFace,0);
                                    if (mFaceBitmap != null) {
                                        if (!isWaitingRecogResult) {
                                            presenter.recognizeUserFace(mFaceBitmap);
                                            isWaitingRecogResult = true;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        };
    }


    @Override
    public void setPresenter(InteractionContract.Presenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void startAnimation() {
        waveView.startAnimation();
    }

    @Override
    public void stopAnimation() {
        waveView.endAnimation();

    }

    @Override
    public void startCamera() {
        try {
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraID);
            StreamConfigurationMap configMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            int width = textureView.getWidth();
            int height = textureView.getHeight();

            //设置一个合适的预览尺寸，防止图像拉伸
//            previewSize = getPreferredPreviewSize(configMap.getOutputSizes(SurfaceTexture.class), width, height);
            previewSize = Util.getPreferredPreviewSize(configMap.getOutputSizes(ImageFormat.JPEG), width, height);
            surfaceTexture.setDefaultBufferSize(previewSize.getWidth(),previewSize.getHeight());
            Log.v(TAG, "previewSize info:" + previewSize.getWidth() + "x" + previewSize.getHeight());

            surface = new Surface(surfaceTexture);

            builder =cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

            if (surface.isValid()) {
                builder.addTarget(surface);
            }
            Log.v(TAG, "mTextureView info:" + textureView.getWidth() + "x" + textureView.getHeight());

            cameraDevice.createCaptureSession(Arrays.asList(surface),sessionStateCallback,null);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stopCamera() {
        try {
            cameraCaptureSession.abortCaptures();
            cameraDevice.close();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        textureView.setVisibility(View.GONE);
    }

    @Override
    public void showRobotImg() {
        imageView.setVisibility(View.VISIBLE);

        Glide.with(getContext())
                .load(R.drawable.talker_img)
                .into(imageView);
    }

    //设置按钮是否可点击
    @Override
    public void setWaveViewEnable(boolean b) {
        log("WaveView -- setClickable:" + b);
        waveView.setEnable(b);
    }

    @Override
    public void showTip() {
        Toast.makeText(getContext(), "Tip:在解说模式，Ai对话功能将被关闭", Toast.LENGTH_SHORT).show();
//        final Snackbar snackbar = Snackbar.make(getView(), "在解说模式，Ai对话功能将被关闭", Snackbar.LENGTH_SHORT);
//        snackbar.setAction("知道了", new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                snackbar.dismiss();
//            }
//        });
    }

    public void setRosServiceBinder(RosConnectionService.ServiceBinder binder){
        presenter.setServiceProxy(binder);
    }


    @Override
    public void onDestroy() {
        stopCamera();
        presenter.releaseMemory();
        super.onDestroy();

    }

    public void log(String string) {
        Log.i(TAG, TAG + " -- " + string);
    }
}
