package cn.ac.iscas.xlab.droidfacedog;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
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
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.WindowManager;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by lisongting on 2017/6/11.
 */

public class FaceDetectActivity extends AppCompatActivity {

    public static final String TAG = "FaceDetectActivity";
    private String mCameraID;//摄像头Id：0代表手机背面的摄像头  1代表朝向用户的摄像头
    private CameraCaptureSession mCameraCaptureSession;
    private TextureView mTextureView;
    private SurfaceTexture mSurfaceTexture;
    private ImageReader mImageReader;
    private CameraManager mCameraManager;
    private CameraDevice mCameraDevice;
    private Size mPreviewSize;

    private HandlerThread mHandlerThread;
    private Handler mHandler;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.face_detect_activity_layout);
        mTextureView = (TextureView) findViewById(R.id.id_texture_view);

        mHandlerThread = new HandlerThread("MyHandlerThread");
        mHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                Log.i(TAG, "handleMessage");
            }
        };

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

            }
        });


        initCamera2();

        Log.i(TAG, "ImageReader info:" + mImageReader.getWidth() + "x" + mImageReader.getHeight());
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



    }

    private void initCamera2() {

        mCameraID = "" + CameraCharacteristics.LENS_FACING_BACK;
        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        DisplayMetrics dm = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(dm);
        mPreviewSize = new Size(dm.widthPixels, dm.heightPixels);
        int width = mPreviewSize.getWidth();
        int height = mPreviewSize.getHeight();

        mImageReader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);

        mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                Log.i(TAG, "OnImageAvailableListener -- onImageAvailable------------------");

            }
        },null);

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


    private void startPreview() {

        try {

            SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
            assert surfaceTexture != null : Log.i(TAG,"surfaceTexture is null");

            CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(mCameraID);
            StreamConfigurationMap configMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            int width = mTextureView.getWidth();
            int height = mTextureView.getHeight();

            //设置一个合适的预览尺寸，防止图像拉伸
            mPreviewSize = getPreferredPreviewSize(configMap.getOutputSizes(SurfaceTexture.class), width, height);
            mSurfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(),mPreviewSize.getHeight());
            Log.i(TAG, "mPreviewSize info:" + mPreviewSize.getWidth() + "x" + mPreviewSize.getHeight());

            Surface surface = new Surface(mSurfaceTexture);

            final CaptureRequest.Builder builder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

            if (surface.isValid()) {
                builder.addTarget(surface);
//                builder.addTarget(mImageReader.getSurface());
            }
            Log.i(TAG, "surface valid:"+surface.isValid());
            Log.i(TAG, "mTextureView info:" + mTextureView.getWidth() + "x" + mTextureView.getHeight());

            mCameraDevice.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()),
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
        super.onDestroy();
    }

}
