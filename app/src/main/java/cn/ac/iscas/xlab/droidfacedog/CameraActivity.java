package cn.ac.iscas.xlab.droidfacedog;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.util.Arrays;

import cn.ac.iscas.xlab.droidfacedog.network.YoutuConnection;
import cn.ac.iscas.xlab.droidfacedog.util.Util;

import static android.view.View.GONE;

/**
 * Created by lisongting on 2017/5/25.
 */
@TargetApi(21)
public class CameraActivity extends AppCompatActivity {

    private TextureView mTextureView;
    private SurfaceTexture mSurfaceTexture;
    private ImageView iv_show;
    private CameraManager mCameraManager;//摄像头管理器
    private String mCameraID;//摄像头Id 0 为后  1 为前
    private ImageReader mImageReader;
    private CameraCaptureSession mCameraCaptureSession;
    private CameraDevice mCameraDevice;
    private Button bt_ok,bt_reCapture,bt_home;
    private LinearLayout linearLayout;
    private TextView tv_shoot;
    private Handler handler;
    public static final String TAG = "CameraActivity";
    private Bitmap faceBitmap;
    private Size mPreviewSize;

    public static final int REGISTER_SUCCESS = 0x11;
    public static final int REGISTER_FAIL = 0x22;
    public static final int REGISTER_ALREADY_EXIST = 0x33;
    public static final int REGISTER_TIMEOUT = 0x44;
    public static final int REGISTER_PIC_TOO_LARGE = 0x55;
    public static final int REGISTER_HAS_NO_FACE = 0x66;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take_head_photo);

        linearLayout = (LinearLayout) findViewById(R.id.bottom_linear_layout);
        bt_ok = (Button) findViewById(R.id.id_bt_ok);
        bt_reCapture = (Button) findViewById(R.id.id_bt_again);
        bt_home = (Button) findViewById(R.id.id_bt_home);
        tv_shoot = (TextView) findViewById(R.id.id_tv_oval);
        iv_show = (ImageView) findViewById(R.id.id_iv_show_picture);
        mTextureView = (TextureView) findViewById(R.id.id_texture_view);

        initView();

        initCamera();

        initOnClickListener();
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                int result = msg.what;
                switch (result) {
                    case REGISTER_SUCCESS:
                        Toast.makeText(CameraActivity.this, "注册成功", Toast.LENGTH_LONG).show();
                        bt_ok.setVisibility(GONE);
                        bt_reCapture.setVisibility(GONE);
                        bt_home.setVisibility(View.VISIBLE);
                        break;
                    case REGISTER_FAIL:
                        Toast.makeText(CameraActivity.this, "注册失败,请检查服务端配置", Toast.LENGTH_SHORT).show();
                        break;
                    case REGISTER_ALREADY_EXIST:
                        Toast.makeText(CameraActivity.this, "注册失败，用户已存在", Toast.LENGTH_SHORT).show();
                        break;
                    case REGISTER_TIMEOUT:
                        Toast.makeText(CameraActivity.this, "连接超时，请确保优图服务端已开启", Toast.LENGTH_SHORT).show();
                        break;
                    case REGISTER_PIC_TOO_LARGE:
                        Toast.makeText(CameraActivity.this, "注册失败,图片尺寸过大", Toast.LENGTH_SHORT).show();
                        break;
                    case REGISTER_HAS_NO_FACE:
                        Toast.makeText(CameraActivity.this, "人脸不在图像中或人脸检测失败", Toast.LENGTH_SHORT).show();
                        break;
                    default:
                        Toast.makeText(CameraActivity.this, "注册失败,错误码:"+result, Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        };
    }

    public void initView() {

        //为SurfaceView设置监听器
        mTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                Log.i(TAG, "TextureView.SurfaceTextureListener -- onSurfaceTextureAvailable()");
                mSurfaceTexture = surface;
                takePreview();
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

            }
        });


    }

    public void initOnClickListener() {
        bt_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String userName = getIntent().getStringExtra("userName");
                //传入handler来处理优图服务端返回的注册结果
                YoutuConnection youtuConnection = new YoutuConnection(CameraActivity.this, handler);
                //进行注册
                youtuConnection.registerFace(userName,faceBitmap);
            }
        });

        bt_reCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reCapture();
            }
        });

        bt_home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(CameraActivity.this, MainActivity.class));
            }
        });

        tv_shoot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePicture();
            }
        });

    }

    private CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            Log.i(TAG, "CameraDevice.StateCallback -- onOpened()");
            mCameraDevice = camera;
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            Log.i(TAG, "CameraDevice.StateCallback -- onDisconnected()");
        }

        @Override
        public void onError(@NonNull CameraDevice camera,  int error) {
            Log.i(TAG, "CameraDevice.StateCallback -- onError()");
        }
    };

    //初始化摄像头
    private void initCamera() {
        mCameraID = ""+CameraCharacteristics.LENS_FACING_BACK;
        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        DisplayMetrics dm = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(dm);
        int width = dm.widthPixels;
        int height = dm.heightPixels;

        Log.i(TAG, "DisplayMetrics width:" + width + ",height:" + height);
        mImageReader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
        Log.i(TAG, "ImageReader width:" + mImageReader.getWidth() + ",height:" + mImageReader.getHeight());

        mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                mTextureView.setVisibility(GONE);
                tv_shoot.setVisibility(GONE);
                iv_show.setVisibility(View.VISIBLE);
                linearLayout.setVisibility(View.VISIBLE);

                Image image = reader.acquireLatestImage();
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                Bitmap tmpBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

                if (tmpBitmap != null) {
                    Log.i(TAG, "Bitmap info: [width:"+tmpBitmap.getWidth() +",height:"+ tmpBitmap.getHeight()+"]");

                    //调节bitmap的尺寸大小
                    int width = tmpBitmap.getWidth();
                    int height = tmpBitmap.getHeight();
                    Matrix matrix = new Matrix();
                    if (width >= 1500 || height >= 1500) {
                        matrix.postScale(0.3F, 0.3F);
                    } else {
                        matrix.postScale(0.5F, 0.5F);
                    }
                    faceBitmap = Bitmap.createBitmap(tmpBitmap, 0, 0, width, height, matrix, true);
                    iv_show.setImageBitmap(tmpBitmap);
                    image.close();
                }
            }
        },null);

        mCameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try{
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)!=
            PackageManager.PERMISSION_GRANTED) {
                return;
            }
            //打开摄像头
            mCameraManager.openCamera(mCameraID,stateCallback,null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }
    CaptureRequest.Builder requestBuilder;
    private void takePreview() {
        try{
            CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(mCameraID);
            StreamConfigurationMap configMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            int width = mTextureView.getWidth();
            int height = mTextureView.getHeight();
            //获得最合适的预览尺寸
            mPreviewSize = Util.getPreferredPreviewSize(configMap.getOutputSizes(ImageFormat.JPEG), width, height);
//            mPreviewSize = Util.getPreferredPreviewSize(configMap.getOutputSizes(SurfaceTexture.class), width, height);
            mSurfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(),mPreviewSize.getHeight());
            Log.i(TAG, "mPreviewSize:" + mPreviewSize.getWidth() + "x" + mPreviewSize.getHeight());

            final Surface surface = new Surface(mSurfaceTexture);
            //创建预览需要的CaptureRequest.Builder
            requestBuilder =
                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

            if (surface.isValid()) {
                requestBuilder.addTarget(surface);
            }
            Log.i(TAG, "mTextureView info:" + mTextureView.getWidth() + "x" + mTextureView.getHeight());

            //创建CameraSession,该对象负责管理处理预览请求和拍照请求
            mCameraDevice.createCaptureSession(Arrays.asList(surface,mImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    if (mCameraDevice == null) {
                        return;
                    }
                    mCameraCaptureSession = session;

                    //设置自动对焦点
                    requestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

                    //打开自动曝光
                    requestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

                    //显示预览
                    CaptureRequest previewRequest = requestBuilder.build();

                    try {
                        mCameraCaptureSession.setRepeatingRequest(previewRequest, null, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Log.i(TAG, "onConfigureFailed");

                }
            },null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }


    private void takePicture() {
        if (mCameraDevice == null) {
            return;
        }

        //创建Request.Builder()
        final CaptureRequest.Builder requestBuilder ;
        try{
            requestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);

            //将ImageReader的surface作为CaptureRequest.Builder的目标
            requestBuilder.addTarget(mImageReader.getSurface());
            // 自动对焦
            requestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            // 自动曝光
            requestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

            requestBuilder.set(CaptureRequest.JPEG_ORIENTATION, 270);

            //拍照
            CaptureRequest mCaptureRequest = requestBuilder.build();
            mCameraCaptureSession.capture(mCaptureRequest, null, null);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void reCapture() {
        iv_show.setVisibility(GONE);
        linearLayout.setVisibility(GONE);
        mTextureView.setVisibility(View.VISIBLE);
        tv_shoot.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "CameraActivity--onDestroy--");
        if (faceBitmap != null) {
            faceBitmap.recycle();
        }
        if (mSurfaceTexture != null) {
            mSurfaceTexture.release();
        }
        if (mCameraCaptureSession != null) {
            mCameraCaptureSession.close();
        }
        mCameraDevice.close();
        mImageReader.getSurface().release();
    }

}
