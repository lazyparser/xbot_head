package cn.ac.iscas.xlab.droidfacedog;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
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
import android.view.SurfaceHolder;
import android.view.SurfaceView;
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

import static android.view.View.GONE;

/**
 * Created by lisongting on 2017/5/25.
 */
@TargetApi(21)
public class CameraActivity extends AppCompatActivity {
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
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
    public static final int REGISTER_SUCCESS = 0x11;
    public static final int REGISTER_FAIL = 0x22;
    public static final int REGISTER_ALREADY_EXIST = 0x33;
    public static final int REGISTER_TIMEOUT = 0x44;
    public static final int REGISTER_PIC_TOO_LARGE = 0x55;
    public static final int REGISTER_HAS_NO_FACE = 0x66;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_layout);

        linearLayout = (LinearLayout) findViewById(R.id.bottom_linear_layout);
        bt_ok = (Button) findViewById(R.id.id_bt_ok);
        bt_reCapture = (Button) findViewById(R.id.id_bt_again);
        bt_home = (Button) findViewById(R.id.id_bt_home);
        tv_shoot = (TextView) findViewById(R.id.id_tv_oval);
        iv_show = (ImageView) findViewById(R.id.id_iv_show_picture);
        mSurfaceView = (SurfaceView) findViewById(R.id.id_surface_view);

        initView();

        initClickListener();

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
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.setKeepScreenOn(true);

        mSurfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                Log.i(TAG, "---surfaceCreated---");
                initCamera2();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                Log.i(TAG, "---surfaceChanged---");
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                Log.i(TAG, "---surfaceDestroyed---");
                if (mCameraDevice != null) {
                    mCameraDevice.close();
                    mCameraDevice = null;
                }
            }
        });

    }

    public void initClickListener() {
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
    private void initCamera2() {
        mCameraID = ""+CameraCharacteristics.LENS_FACING_BACK;
        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        DisplayMetrics dm = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(dm);
        int width = dm.widthPixels;
        int height = dm.heightPixels;
        mImageReader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);

        mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                mCameraDevice.close();
                mSurfaceView.setVisibility(GONE);
                tv_shoot.setVisibility(GONE);
                iv_show.setVisibility(View.VISIBLE);
                linearLayout.setVisibility(View.VISIBLE);
                Image image = reader.acquireNextImage();
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                Bitmap tmpBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

                if (tmpBitmap != null) {
                    Log.i("tag", "width:"+tmpBitmap.getWidth() +"height:"+ tmpBitmap.getHeight());

                    //调节bitmap的尺寸大小
                    int width = tmpBitmap.getWidth();
                    int height = tmpBitmap.getHeight();
                    Matrix matrix = new Matrix();
                    if (width >= 1500 || height >= 1500) {
                        matrix.postScale(0.3F, 0.3F);
                    }
                    matrix.postScale(0.5F, 0.5F);
                    faceBitmap = Bitmap.createBitmap(tmpBitmap, 0, 0, width, height, matrix, true);
                    iv_show.setImageBitmap(tmpBitmap);
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

    private CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            mCameraDevice = camera;
            takePreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            if (mCameraDevice != null) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
        }

        @Override
        public void onError(@NonNull CameraDevice camera,  int error) {
            Toast.makeText(CameraActivity.this, "摄像头开启失败", Toast.LENGTH_SHORT).show();
        }
    };

    private void takePreview() {
        try{
            //创建预览需要的CaptureRequest.Builder
            final CaptureRequest.Builder requestbuilder =
                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

            requestbuilder.addTarget(mSurfaceHolder.getSurface());

            //创建CameraSession,该对象负责管理处理预览请求和拍照请求
            mCameraDevice.createCaptureSession(Arrays.asList(mSurfaceHolder.getSurface(),mImageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    if (mCameraDevice == null) {
                        return;
                    }
                    mCameraCaptureSession = session;

                    //设置自动对焦点
                    requestbuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

                    //打开自动曝光
                    requestbuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

                    //显示预览
                    CaptureRequest previewRequest = requestbuilder.build();

                    try {
                        mCameraCaptureSession.setRepeatingRequest(previewRequest, null, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Toast.makeText(CameraActivity.this, "配置失败", Toast.LENGTH_SHORT).show();

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
        mSurfaceView.setVisibility(View.VISIBLE);
        tv_shoot.setVisibility(View.VISIBLE);
    }

}
