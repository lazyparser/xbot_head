package cn.ac.iscas.xlab.droidfacedog;

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
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import cn.ac.iscas.xlab.droidfacedog.entity.FaceResult;
import cn.ac.iscas.xlab.droidfacedog.util.ImageUtils;

/**
 * Created by lisongting on 2017/6/11.
 */

public class FaceDetectActivity extends AppCompatActivity {

    public static final String TAG = "FaceDetectActivity";
    public static final int MAX_FACE_COUNT = 3;
    private String mCameraID;//摄像头Id：0代表手机背面的摄像头  1代表朝向用户的摄像头
    private CameraCaptureSession mCameraCaptureSession;
    private TextureView mTextureView;
    private SurfaceTexture mSurfaceTexture;
    private ImageReader mImageReader;
    private CameraManager mCameraManager;
    private CameraDevice mCameraDevice;
    private Size mPreviewSize;
    private FaceOverlayView mFaceOverlayView;
    private RecyclerView mRecyclerView;
    private TextView mFpsTextView;
    private int mPreviewWidth;
    private int mPreviewHeight;

    private ImagePreviewAdapter mImagePreviewAdapter;
    private Handler mainHandler;
    private Timer mDetectTimer;
    private FaceDetector mFaceDetector;

    //用来存放检测到的人脸
    private FaceDetector.Face[] mDetectedFaces;
    private FaceResult[] mFaces;
    private FaceResult[] mPreviousFaces;

    //要发送给人脸识别服务器的人脸bitmap
    private Bitmap faceBitmap;

    //识别到的人脸的id,初始为0。如果镜头中只有一个人，那么这里始终只为0
    private int mPersonId =0;
    //用来标记每个人脸共有几张图像,key是人脸的id，value是当前采集到的图像张数。当图像大于
    private HashMap<Integer,Integer> mFacesCountMap;

    //RecyclerView中的人脸图像的List
    private ArrayList<Bitmap> mRecyclerViewBitmapList;

    long frameCount = 0;
    long start,end;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.face_detect_activity_layout);
        mTextureView = (TextureView) findViewById(R.id.id_texture_view);
        mFpsTextView = (TextView) findViewById(R.id.id_tv_fps);
        mRecyclerView = (RecyclerView) findViewById(R.id.id_recycler_view);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(layoutManager);

        mFaces = new FaceResult[MAX_FACE_COUNT];
        mPreviousFaces = new FaceResult[MAX_FACE_COUNT];
        mDetectedFaces = new FaceDetector.Face[MAX_FACE_COUNT];
        for (int i = 0; i < MAX_FACE_COUNT; i++) {
            mFaces[i] = new FaceResult();
            mPreviousFaces[i] = new FaceResult();
        }

        mRecyclerViewBitmapList = new ArrayList<>();
        mFacesCountMap = new HashMap<>();

        mDetectTimer = new Timer();
        mainHandler = new Handler();

        initView();
        

    }

    @Override
    public void onResume() {
        super.onResume();

        //为SurfaceView设置监听器
        mTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                Log.i(TAG, "TextureView.SurfaceTextureListener -- onSurfaceTextureAvailable()");
                mSurfaceTexture = surface;
                mPreviewWidth = mTextureView.getWidth();
                mPreviewHeight = mTextureView.getHeight();
                //当TextureView可用后，调用startPreview开启预览
                startPreview();
                start = System.currentTimeMillis();
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
//                Log.i(TAG, "TextureView.SurfaceTextureListener -- onSurfaceTextureUpdated()");
                new Thread(){
                    public void run(){
                        end = System.currentTimeMillis();
                        frameCount++;
                        double t = (end - start) / 1000;
                        final double fps = frameCount / t;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                DecimalFormat df = new DecimalFormat("#.00");

                                mFpsTextView.setText("Frame Per Second:"+df.format(fps));
                            }
                        });

                    }
                }.start();

            }
        });

        initCamera();

        startDetectTask();

    }


    private CameraDevice.StateCallback cameraStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            Log.i(TAG, "CameraDevice.StateCallback -- onOpened()");
            mCameraDevice = camera;

        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            Log.i(TAG, "CameraDevice.StateCallback -- onDisconnected()");
            if (mCameraDevice != null) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            Log.i(TAG, "CameraDevice.StateCallback -- onError()");

        }
    };


    private void initView() {

        mFaceOverlayView = new FaceOverlayView(this);

        RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.MATCH_PARENT);
        addContentView(mFaceOverlayView,params );

    }

    private void initCamera() {

        mCameraID = "" + CameraCharacteristics.LENS_FACING_BACK;
        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        DisplayMetrics dm = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(dm);
        mPreviewSize = new Size(dm.widthPixels, dm.heightPixels);
        int width = mPreviewSize.getWidth();
        int height = mPreviewSize.getHeight();

//        mImageReader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
//
//        mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
//            @Override
//            public void onImageAvailable(ImageReader reader) {
//                Log.i(TAG, "OnImageAvailableListener -- onImageAvailable------------------");
//
//            }
//        },null);

        mCameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) !=
                    PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "没有权限", Toast.LENGTH_SHORT).show();
                return;
            }
            mCameraManager.openCamera(mCameraID, cameraStateCallback, null);
        }catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    private void startDetectTask() {
        //在这个定时任务中，不断的检测界面中的人脸
        final TimerTask detectTask = new TimerTask() {
            long start;
            @Override
            public void run() {

                //计算适合的尺寸
                int prevSettingWidth, prevSettingHeight;

                if (mPreviewWidth / 4 > 360) {
                    prevSettingWidth = 360;
                    prevSettingHeight = 270;
                } else if (mPreviewWidth / 4 > 320) {
                    prevSettingWidth = 320;
                    prevSettingHeight = 240;
                } else if (mPreviewWidth / 4 > 240) {
                    prevSettingWidth = 240;
                    prevSettingHeight = 160;
                } else {
                    prevSettingWidth = 160;
                    prevSettingHeight = 120;
                }

                float aspect = (float) mPreviewHeight / (float) mPreviewWidth;
                int w = prevSettingWidth;
                int h = (int) (prevSettingWidth * aspect);
                float xScale = (float) mPreviewWidth / (float) prevSettingWidth;
                float yScale = (float) mPreviewHeight / (float) h;


                Bitmap face = mTextureView.getBitmap();
                if (face != null) {
                    start  = System.currentTimeMillis();

                    Log.i(TAG, "faceBitmap info:" + face.getWidth() + "x" + face.getHeight()+",Config:"+face.getConfig());
                    //创建一个人脸检测器，MAX_FACE参数表示最多识别MAX_FACE张人脸
//                    mFaceDetector = new FaceDetector(face.getWidth(), face.getHeight(), MAX_FACE_COUNT);

                    //原先的bitmap格式是ARGB_8888，以下的步骤是把格式转换为RGB_565
                    ByteArrayOutputStream bout = new ByteArrayOutputStream();
                    face.compress(Bitmap.CompressFormat.JPEG, 100, bout);
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inPreferredConfig = Bitmap.Config.RGB_565;
                    Bitmap RGBFace = BitmapFactory.decodeStream(new ByteArrayInputStream(bout.toByteArray()), null, options);

                    Bitmap smallRGBFace = Bitmap.createScaledBitmap(RGBFace, w, h, false);


                    Log.i(TAG, "RGBFace:" + RGBFace.getWidth() + "x" + RGBFace.getHeight() + "," + RGBFace.getConfig());
                    Log.i(TAG, "smallRGBFace:" + smallRGBFace.getWidth() + "x" + smallRGBFace.getHeight() + "," + smallRGBFace.getConfig());

                    //test
                    mFaceDetector = new FaceDetector(smallRGBFace.getWidth(), smallRGBFace.getHeight(), MAX_FACE_COUNT);
    //                Log.i(TAG, "tmp bitmap:" + tmp.getWidth() + "x" + tmp.getHeight());
                    //findFaces中传入的bitmap格式必须为RGB_565
                    int found = mFaceDetector.findFaces(smallRGBFace, mDetectedFaces);

                    Log.i(TAG, "found:" + found+" face(s)");

                    for(int i=0;i<MAX_FACE_COUNT;i++) {
                        if (mDetectedFaces[i] == null) {
                            mFaces[i].clear();
                        } else {


                            PointF mid = new PointF();
                            mDetectedFaces[i].getMidPoint(mid);
                            mid.x *= xScale;
                            mid.y *= yScale;

                            Log.i(TAG, "mid pointF:" + mid.x + "," + mid.y);
                            float eyeDistance = mDetectedFaces[i].eyesDistance();
                            float confidence = mDetectedFaces[i].confidence();
                            float pose = mDetectedFaces[i].pose(FaceDetector.Face.EULER_Y);
                            //mPersonId一开始是0
                            int personId = mPersonId;

                            //预先创建一个人脸矩形区域
                            RectF rectF = ImageUtils.getPreviewFaceRectF(mid, eyeDistance);

                            //如果人脸矩形区域大于一定区域，才采集图像
                            if (rectF.width() * rectF.height() > 90 * 60) {
                                for(int j=0;j<MAX_FACE_COUNT;j++) {
                                    //获取之前的Faces数据
                                    float eyesDisPre = mPreviousFaces[j].eyesDistance();
                                    PointF midPre = new PointF();
                                    mPreviousFaces[j].getMidPoint(midPre);

                                    //在一定区域内检查人脸是否移动过大，超出这个区域。
                                    RectF rectCheck = ImageUtils.getCheckFaceRectF(midPre, eyesDisPre);

                                    //如果没有当前人脸没有超过这个检查区域，说明该ID对应的人脸晃动程度小，则适合采集
                                    if (rectCheck.contains(mid.x, mid.y) && (System.currentTimeMillis() - mPreviousFaces[j].getTime()) < 1000) {
                                        personId = mPreviousFaces[j].getId();
                                        break;
                                    }
                                }

                                mFaces[i].setFace(personId, mid, eyeDistance, confidence, pose, System.currentTimeMillis());

                                mPreviousFaces[i].set(mFaces[i].getId(), mFaces[i].getMidEye(), mFaces[i].eyesDistance(), mFaces[i].getConfidence(), mFaces[i].getPose(), mFaces[i].getTime());

                                if (mFacesCountMap.get(personId) == null) {
                                    mFacesCountMap.put(personId, 0);
                                }else {

                                    int frameCount = mFacesCountMap.get(personId) + 1;
                                    if (frameCount < 5) {
                                        mFacesCountMap.put(personId, frameCount);
                                    }
                                    if (frameCount == 5) {
                                        int rotate = getWindowManager().getDefaultDisplay().getRotation();
                                        faceBitmap = ImageUtils.cropFace(mFaces[i], face,rotate);
                                        if (faceBitmap != null) {
                                            mainHandler.post(new Runnable() {
                                                public void run() {
                                                    mImagePreviewAdapter = new ImagePreviewAdapter(FaceDetectActivity.this, mRecyclerViewBitmapList, new ImagePreviewAdapter.ViewHolder.OnItemClickListener() {
                                                        @Override
                                                        public void onClick(View v, int position) {
                                                            mImagePreviewAdapter.setCheck(position);
                                                            mImagePreviewAdapter.notifyDataSetChanged();
                                                        }
                                                    });
                                                    if (mImagePreviewAdapter.getItemCount() < 5) {
                                                        mImagePreviewAdapter.add(faceBitmap);
                                                        mRecyclerView.setAdapter(mImagePreviewAdapter);
                                                    }

                                                }
                                            });
                                        }
                                    }
                                }
                            }

                        }
                    }
                    mainHandler.post(new Runnable() {
                        public void run() {
                            //send face to FaceView to draw rect
                            mFaceOverlayView.setFaces(mFaces);
                            for (FaceResult f : mFaces) {
                                Log.i(TAG, f.toString());
                            }
                        }
                    });



                }
            }
        };

        mDetectTimer.schedule(detectTask, 0, 200);
    }

    private void startPreview() {

        try {

            SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
            assert surfaceTexture != null : Log.i(TAG,"surfaceTexture is null");

            CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(mCameraID);
            StreamConfigurationMap configMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            int width = mTextureView.getWidth();
            int height = mTextureView.getHeight();

            //设置一个合适的预览尺寸，防止图像拉伸
//            mPreviewSize = getPreferredPreviewSize(configMap.getOutputSizes(SurfaceTexture.class), width, height);
            mPreviewSize = getPreferredPreviewSize(configMap.getOutputSizes(ImageFormat.JPEG), width, height);
            mSurfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(),mPreviewSize.getHeight());
            Log.i(TAG, "mPreviewSize info:" + mPreviewSize.getWidth() + "x" + mPreviewSize.getHeight());

            Surface surface = new Surface(mSurfaceTexture);

            final CaptureRequest.Builder builder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

            if (surface.isValid()) {
                builder.addTarget(surface);
//                builder.addTarget(mImageReader.getSurface());
            }
            Log.i(TAG, "mTextureView info:" + mTextureView.getWidth() + "x" + mTextureView.getHeight());

            mCameraDevice.createCaptureSession(Arrays.asList(surface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            Log.i(TAG, "CameraCaptureSession.StateCallback -- onConfigured()");
                            if (mCameraDevice == null) {
                                return;
                            }
                            mCameraCaptureSession = session;

                            //设置自动对焦
                            builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

                            //设置自动曝光
                            builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

                            //显示预览
                            CaptureRequest captureRequest = builder.build();

                            try {
                                mCameraCaptureSession.setRepeatingRequest(captureRequest, new CameraCaptureSession.CaptureCallback() {
                                    @Override
                                    public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
//                                        Log.i(TAG, "CameraCaptureSession.CaptureCallback -- onCaptureStarted()");
                                        super.onCaptureStarted(session, request, timestamp, frameNumber);

                                    }

                                    @Override
                                    public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
//                                        Log.i(TAG, "CameraCaptureSession.CaptureCallback -- onCaptureProgressed()");
                                        super.onCaptureProgressed(session, request, partialResult);
                                    }

                                    @Override
                                    public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
//                                        Log.i(TAG, "CameraCaptureSession.CaptureCallback -- onCaptureCompleted()");

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
                    },null);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private Size getPreferredPreviewSize(Size[] sizes, int width, int height) {
        List<Size> collectorSizes = new ArrayList<>();
        for (Size option : sizes) {
            //找到长宽都大于指定宽高的size，把这些size放在List中
            if (width > height) {
                if (option.getWidth() > width && option.getHeight() > height) {
                    collectorSizes.add(option);
                }
            } else {
                if (option.getHeight() > width && option.getWidth() > height) {
                    collectorSizes.add(option);
                }
            }
        }
        if (collectorSizes.size() > 0) {
            return Collections.min(collectorSizes, new Comparator<Size>() {
                @Override
                public int compare(Size s1, Size s2) {
                    return Long.signum(s1.getWidth() * s1.getHeight() - s2.getWidth() * s2.getHeight());
                }
            });
        }
        return sizes[0];
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mCameraCaptureSession != null) {
            mCameraCaptureSession.close();
        }
        mDetectTimer.cancel();
    }

    @Override
    public void onDestroy(){
        if (mCameraDevice != null) {
            mCameraDevice.close();
        }

        if (mSurfaceTexture != null) {
            mSurfaceTexture.release();
        }
        if (mImageReader != null) {
            mImageReader.close();
        }
        if (mImagePreviewAdapter != null) {
            mImagePreviewAdapter.clearAll();
        }
        super.onDestroy();
    }
}
