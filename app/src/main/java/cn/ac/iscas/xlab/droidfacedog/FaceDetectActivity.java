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
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.Size;
import android.view.MenuItem;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
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
import cn.ac.iscas.xlab.droidfacedog.network.YoutuConnection;
import cn.ac.iscas.xlab.droidfacedog.util.ImageUtils;
import cn.ac.iscas.xlab.droidfacedog.util.RegexCheckUtil;
import cn.ac.iscas.xlab.droidfacedog.util.Util;

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
    private CameraManager mCameraManager;
    private CameraDevice mCameraDevice;
    private Size mPreviewSize;
    private FaceOverlayView mFaceOverlayView;
    private RecyclerView mRecyclerView;
    private TextView mFpsTextView;
    private ImagePreviewAdapter mImagePreviewAdapter;
    private Handler mainHandler;
    private Timer mDetectTimer;

    //用来存放检测到的人脸
    private FaceDetector.Face[] mDetectedFaces;
    private FaceResult[] mFaces;
    private FaceResult[] mPreviousFaces;
    private FaceDetector mFaceDetector;

    //要发送给人脸识别服务器的人脸bitmap
    private Bitmap mFaceBitmap;
    private YoutuConnection connection;
    //这个boolean表示将人脸发送给服务器后，当前是否正在等待优图服务器返回识别结果
    private boolean isWaitingResult = false;

    //识别到的人脸的id（不是注册在服务端的ID）,初始为0。
    private int mPersonId =0;
    //用来标记每个人脸共有几张图像,key是人脸的id，value是当前采集到的图像张数
    private HashMap<Integer,Integer> mFacesCountMap;

    //RecyclerView中的人脸图像的List
    private ArrayList<Bitmap> mRecyclerViewBitmapList;

    //比例因子，将检测到的原始人脸图像按此比例缩小，以此可以加快FaceDetect的检测速度
    private double mScale = 0.2;

    long totalFrameCount = 0;
    long start,end;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.face_detect_activity_layout);
        mTextureView = (TextureView) findViewById(R.id.id_texture_view);
        mFpsTextView = (TextView) findViewById(R.id.id_tv_fps);
        mRecyclerView = (RecyclerView) findViewById(R.id.id_recycler_view);

        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

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
        mainHandler = new Handler(){
            public void handleMessage(Message msg) {
                Bundle data = msg.getData();
                String hexNameStr = (String) data.get("userId");
                String chineseName = Util.hexStringToString(hexNameStr);
                if (RegexCheckUtil.isRightPersonName(chineseName)) {
                    Toast.makeText(FaceDetectActivity.this, "用户：" + chineseName, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(FaceDetectActivity.this, "未知用户\n(请检查服务器配置或调低人脸检测阈值)" , Toast.LENGTH_LONG).show();
                }

            }
        };

        connection = new YoutuConnection(FaceDetectActivity.this,mainHandler);
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
                //每过400帧数，将isWaitingResult置为false，这样可以避免频繁发送人脸给服务器
                if (totalFrameCount % 400 == 0) {
                    isWaitingResult = false;
                }
                //显示FPS
                new Thread(){
                    public void run(){
                        end = System.currentTimeMillis();
                        totalFrameCount++;
                        double t = (end - start) / 1000.0;
                        final double fps = totalFrameCount / t;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                DecimalFormat df = new DecimalFormat("#.00");
                                mFpsTextView.setText("FPS:"+df.format(fps));
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
        mFaceOverlayView.setFront(true);
        mFaceOverlayView.setDisplayOrientation(getWindowManager().getDefaultDisplay().getRotation());
    }

    private void initCamera() {
        mCameraID = "" + CameraCharacteristics.LENS_FACING_BACK;
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
            @Override
            public void run() {
                int rotate = getWindowManager().getDefaultDisplay().getRotation();
                Bitmap face = mTextureView.getBitmap();
                if (face != null) {
                    Log.i(TAG, "Bitmap in mTextureView :" + face.getWidth() + "x" + face.getHeight()+",Config:"+face.getConfig());

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

                            Log.i(TAG, "mid pointF:" + mid.x + "," + mid.y);
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
                                    if (rectCheck.contains(mid.x, mid.y) && (System.currentTimeMillis() - mPreviousFaces[j].getTime()) < 1000) {
                                        personId = mPreviousFaces[j].getId();
                                        break;
                                    }
                                }

                                if (mPersonId == personId) {
                                    mPersonId++;
                                }
                                Log.i(TAG, "totalFrameCount:" + totalFrameCount);
                                mFaces[i].setFace(personId, mid, eyeDistance, confidence, pose, System.currentTimeMillis());
                                mPreviousFaces[i].set(mFaces[i].getId(), mFaces[i].getMidEye(), mFaces[i].eyesDistance(), mFaces[i].getConfidence(), mFaces[i].getPose(), mFaces[i].getTime());
                                if (mFacesCountMap.get(personId) == null) {
                                    mFacesCountMap.put(personId, 0);
                                }else {
                                    int tmpFrameCount = mFacesCountMap.get(personId) + 1;
                                    if (tmpFrameCount < 5) {
                                        mFacesCountMap.put(personId, tmpFrameCount);
                                    }
                                    if (tmpFrameCount == 5) {
                                        mFaceBitmap = ImageUtils.cropFace(mFaces[i], RGBFace,rotate);
                                        if (mFaceBitmap != null) {
                                            mainHandler.post(new Runnable() {
                                                public void run() {
                                                    mImagePreviewAdapter = new ImagePreviewAdapter(FaceDetectActivity.this, mRecyclerViewBitmapList, new ImagePreviewAdapter.ViewHolder.OnItemClickListener() {
                                                        @Override
                                                        public void onClick(View v, int position) {
                                                            mImagePreviewAdapter.setCheck(position);
                                                            mRecyclerView.setAdapter(mImagePreviewAdapter);
                                                            mImagePreviewAdapter.notifyDataSetChanged();
                                                        }
                                                    });
                                                    if (mImagePreviewAdapter.getItemCount() < 5) {
                                                        mImagePreviewAdapter.add(mFaceBitmap);
                                                        mRecyclerView.setAdapter(mImagePreviewAdapter);
                                                    }
                                                }
                                            });
                                            if(!isWaitingResult ){
                                                connection.sendBitmap(mFaceBitmap);
                                                isWaitingResult = true;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    mainHandler.post(new Runnable() {
                        public void run() {
                            //调用mFaceOverlayView的setFaces()方法，绘制人脸区域的矩形
                            mFaceOverlayView.setFaces(mFaces);
                            for (FaceResult f : mFaces) {
                                if (f.eyesDistance()>0) {
                                    Log.i(TAG, f.toString());
                                }
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
            }
            Log.i(TAG, "mTextureView info:" + mTextureView.getWidth() + "x" + mTextureView.getHeight());

            mFaceOverlayView.setPreviewWidth(mTextureView.getWidth());
            mFaceOverlayView.setPreviewHeight(mTextureView.getHeight());

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
                                        super.onCaptureStarted(session, request, timestamp, frameNumber);
                                    }

                                    @Override
                                    public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
                                        super.onCaptureProgressed(session, request, partialResult);
                                    }

                                    @Override
                                    public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
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
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
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
        if (mImagePreviewAdapter != null) {
            mImagePreviewAdapter.clearAll();
        }
        super.onDestroy();
    }
}
