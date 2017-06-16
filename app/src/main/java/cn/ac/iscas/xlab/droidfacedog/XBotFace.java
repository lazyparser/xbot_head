package cn.ac.iscas.xlab.droidfacedog;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.SynthesizerListener;
import com.iflytek.cloud.util.ResourceUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import cn.ac.iscas.xlab.droidfacedog.entity.FaceResult;
import cn.ac.iscas.xlab.droidfacedog.entity.RobotStatus;
import cn.ac.iscas.xlab.droidfacedog.entity.TtsStatus;
import cn.ac.iscas.xlab.droidfacedog.config.Config;
import cn.ac.iscas.xlab.droidfacedog.network.YoutuConnection;
import cn.ac.iscas.xlab.droidfacedog.util.ImageUtils;
import cn.ac.iscas.xlab.droidfacedog.util.Util;
import de.greenrobot.event.EventBus;



/**
 * Created by Nguyen on 5/20/2016.
 */

/**
 * FACE DETECT EVERY FRAME WIL CONVERT TO RGB BITMAP SO THIS HAS LOWER PERFORMANCE THAN GRAY BITMAP
 * COMPARE FPS (DETECT FRAME PER SECOND) OF 2 METHODs FOR MORE DETAIL
 */

public final class XBotFace extends AppCompatActivity implements SurfaceHolder.Callback, Camera.PreviewCallback {

    public static final String TAG = "XBotFace";
    public static final int IDLESTATE = 0;
    public static final int DETECTEDSTATE = 1;
    public static final int IDENTIFIEDSTATE = 2;
    public static final int CONN_ROS_SERVER_SUCCESS = 0x11;
    public static final int CONN_ROS_SERVER_ERROR = 0x12;
    public static final int HANDLER_UPDATE_FACE_STATE = 0x13;
    public static final int HANDLER_PLAY_TTS = 0x14;
    public static final String TTS_UNREGISTERED_USER = "0000000000";

    private int numberOfCameras;

    private Camera mCamera;
    private int cameraId = 0;

    // Let's keep track of the display rotation and orientation also:
    private int mDisplayRotation;
    private int mDisplayOrientation;

    private int previewWidth;
    private int previewHeight;

    // The surface view for the camera data
    private SurfaceView mView;

    // Draw rectangles and other fancy stuff:
    private FaceOverlayView mFaceView;

    // Log all errors:
    private final CameraErrorCallback mErrorCallback = new CameraErrorCallback();

    private static final int MAX_FACE = 3;
    private boolean isThreadWorking = false;
    private Handler handler;
    private FaceDetectThread detectThread = null;
    private int prevSettingWidth;
    private int prevSettingHeight;
    private android.media.FaceDetector fdet;

    private FaceResult faces[];
    private FaceResult faces_previous[];
    private int Id = 0;

    private String BUNDLE_CAMERA_ID = "camera";


    //RecylerView face image
    private HashMap<Integer, Integer> facesCount = new HashMap<>();
    private RecyclerView recyclerView;
    private ImagePreviewAdapter imagePreviewAdapter;
    private ArrayList<Bitmap> facesBitmap;
    private int m_state;
    private ImageView faceImageView;
    private long m_lastchangetime;

    private AssetManager mAssets;

    //用于定时发起与Ros服务端的连接
    private Timer timer;

    //用于定时发布topic
    private Timer publishTimer;

    private AlertDialog dialog;

    //科大讯飞的语音合成器[需要联网才能使用]
    private SpeechSynthesizer ttsSynthesizer;
    private SynthesizerListener synthesizerListener;

    private boolean isEnableRos;

    private RosConnectionService.ServiceBinder serviceProxy;

    //ServiceConnection
    private ServiceConnection serviceConnection;

    private AudioManager audioManager;
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_xbot_face);

        mView = (SurfaceView) findViewById(R.id.xbotfaceview);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Now create the OverlayView:
        mFaceView = new FaceOverlayView(this);
        addContentView(mFaceView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        // Create and Start the OrientationListener:

        recyclerView = (RecyclerView) findViewById(R.id.xbotfacerecyclerview);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        EventBus.getDefault().register(this);

        audioManager = new AudioManager(this);
        audioManager.loadTts();

        isEnableRos = true;

        //双用途Handler，一用来接收TimerTask中发回来的Ros连接状态，二用来接收优图的识别结果
        handler = new Handler(){
            public void handleMessage(Message msg) {
                //如果连接成功
                if (msg.what == CONN_ROS_SERVER_SUCCESS) {
                    if (dialog.isShowing()) {
                        dialog.dismiss();
                        timer.cancel();
                        Toast.makeText(XBotFace.this, "连接成功", Toast.LENGTH_SHORT).show();
                        Toast.makeText(XBotFace.this, "正在进行人脸识别，请稍等", Toast.LENGTH_LONG).show();

                        startPreview();
                    }
                }else if(msg.what == CONN_ROS_SERVER_ERROR){
                    //Toast.makeText(XBotFace.this, "连接失败，正在重试", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "连接失败，正在重试");
                } else if (msg.what == HANDLER_UPDATE_FACE_STATE) {
                    updateFaceState(IDENTIFIEDSTATE);
                } else if (msg.what == HANDLER_PLAY_TTS) {
                    Bundle data = msg.getData();
                    String userName = (String) data.get("userId");
                    speakOutUser(userName);
                }
            }
        };
        faces = new FaceResult[MAX_FACE];
        faces_previous = new FaceResult[MAX_FACE];
        for (int i = 0; i < MAX_FACE; i++) {
            faces[i] = new FaceResult();
            faces_previous[i] = new FaceResult();
        }

        faceImageView = (ImageView) findViewById(R.id.faceimageview);
        updateFaceState(IDLESTATE);

        if (icicle != null)
            cameraId = icicle.getInt(BUNDLE_CAMERA_ID, 0);

        mAssets = getAssets();

        //启动一个对话框提示用户等待，然后连接至Ros服务器
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("请稍等")
                .setMessage("正在连接至Ros服务端")
                .setCancelable(false)
                .setPositiveButton("直接识别人脸", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        isEnableRos = false;
                        timer.cancel();
                        Toast.makeText(XBotFace.this, "正在进行人脸识别，请稍等", Toast.LENGTH_LONG).show();

                        if (serviceConnection != null) {
                            unbindService(serviceConnection);
                        }
                        startPreview();
                    }
                })
                .setNegativeButton("取消连接", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        XBotFace.this.onBackPressed();
                        timer.cancel();
                    }
                });
        dialog = builder.create();
        dialog.show();

        //创建定时任务
        initTimerTask();

        //初始化讯飞TTS引擎
        SpeechUtility.createUtility(this, SpeechConstant.APPID +"="+Config.APPID);

        //创建ServiceConnection对象
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                Log.i(TAG, "ServiceConnection--onServiceConnected()");
                serviceProxy = (RosConnectionService.ServiceBinder) service;
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.i(TAG, "ServiceConnection--onServiceDisconnected()");
            }
        };
        //绑定RosConnectionService
        Intent intent = new Intent(this, RosConnectionService.class);

        bindService(intent, serviceConnection, BIND_AUTO_CREATE);
    }

    public void onEvent(RobotStatus status) {
        int locationId = status.getLocationId();
        boolean isMoving = status.isMoving();
        //如果到达了新的位置，并且audioManager并没有在播放音频，则开始播放指定id的音频
        if (locationId != audioManager.getCurrentId() && !audioManager.isPlaying()) {
            audioManager.play(locationId);
        }
    }

    public void initTimerTask(){
        //启动定时任务，每3秒种发起一次连接
        //然后将结果发送给Handler，
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (serviceProxy != null) {
                    if (serviceProxy.connect()) {
                        handler.sendEmptyMessage(CONN_ROS_SERVER_SUCCESS);
                    } else {
                        handler.sendEmptyMessage(CONN_ROS_SERVER_ERROR);
                    }
                }

            }
        },0,3000);

        publishTimer = new Timer();
        publishTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (serviceProxy != null && audioManager !=null) {
                    int id = audioManager.getCurrentId();
                    boolean isPlaying = audioManager.isPlaying();
                    TtsStatus status = new TtsStatus(id,isPlaying);
                    serviceProxy.publishTtsStatus(status);
                }
            }
        },1000,200);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Check for the camera permission before accessing the camera.  If the
        // permission is not granted yet, request permission.
        SurfaceHolder holder = mView.getHolder();
        holder.addCallback(this);
        holder.setFormat(ImageFormat.NV21);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_camera, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                return true;

            case R.id.switchCam:

                if (numberOfCameras == 1) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Switch Camera").setMessage("Your device have one camera").setNeutralButton("Close", null);
                    AlertDialog alert = builder.create();
                    alert.show();
                    return true;
                }

                cameraId = (cameraId + 1) % numberOfCameras;
                recreate();

                return true;


            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Restarts the camera.
     */
    @Override
    protected void onResume() {
        super.onResume();
        initSynthesizer();
        Log.i(TAG, "onResume");

        //如果对话框消失，说明与Ros服务端连接成功
        if (!dialog.isShowing()) {
            startPreview();
        }

    }

    /**
     * Stops the camera.
     */
    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause");
        if (mCamera != null) {
            mCamera.stopPreview();
        }
    }


    @Override
    protected void onDestroy() {

        Log.i(TAG, "onDestroy");
//        unbindService(serviceConnection);
        resetData();

        audioManager.releaseMemory();

        ttsSynthesizer.stopSpeaking();
        ttsSynthesizer.destroy();
        EventBus.getDefault().unregister(this);

        timer.cancel();
        publishTimer.cancel();
        super.onDestroy();
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(BUNDLE_CAMERA_ID, cameraId);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        resetData();

        //Find the total number of cameras available
        numberOfCameras = Camera.getNumberOfCameras();
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
            Camera.getCameraInfo(i, cameraInfo);
            Log.d(TAG, "Camera i = " + i + " " + cameraInfo.toString());
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                if (cameraId == 0) cameraId = i;
            }
        }

        mCamera = Camera.open(cameraId);

        Camera.getCameraInfo(cameraId, cameraInfo);
        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            mFaceView.setFront(true);
        }

        try {
            mCamera.setPreviewDisplay(mView.getHolder());
        } catch (Exception e) {
            Log.e(TAG, "Could not preview the image.", e);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
        // We have no surface, return immediately:
        if (surfaceHolder.getSurface() == null) {
            return;
        }
        // Try to stop the current preview:
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
            // Ignore...
        }

        configureCamera(width, height);
        setDisplayOrientation();
        setErrorCallback();

        // Create media.FaceDetector
        float aspect = (float) previewHeight / (float) previewWidth;
        Log.e(TAG, "[[w, h]] = [" + Integer.toString(previewWidth) +
            ", " + Integer.toString((int) (prevSettingWidth * aspect)));
        fdet = new android.media.FaceDetector(prevSettingWidth, (int) (prevSettingWidth * aspect), MAX_FACE);


        // Everything is configured! Finally start the camera preview again:
        if (!dialog.isShowing()) {
            startPreview();
        }
    }

    private void setErrorCallback() {
        mCamera.setErrorCallback(mErrorCallback);
    }

    private void setDisplayOrientation() {
        // Now set the display orientation:
        mDisplayRotation = Util.getDisplayRotation(XBotFace.this);
        mDisplayOrientation = Util.getDisplayOrientation(mDisplayRotation, cameraId);

        mCamera.setDisplayOrientation(mDisplayOrientation);

        if (mFaceView != null) {
            mFaceView.setDisplayOrientation(mDisplayOrientation);
        }
    }

    private void configureCamera(int width, int height) {
        Camera.Parameters parameters = mCamera.getParameters();
        // Set the PreviewSize and AutoFocus:
        setOptimalPreviewSize(parameters, width, height);
        setAutoFocus(parameters);
        // And set the parameters:
        mCamera.setParameters(parameters);
    }

    private void setOptimalPreviewSize(Camera.Parameters cameraParameters, int width, int height) {
        List<Camera.Size> previewSizes = cameraParameters.getSupportedPreviewSizes();
        float targetRatio = (float) width / height;
        Camera.Size previewSize = Util.getOptimalPreviewSize(this, previewSizes, targetRatio);
        previewWidth = previewSize.width;
        previewHeight = previewSize.height;

        Log.e(TAG, "previewWidth " + previewWidth);
        Log.e(TAG, "previewHeight " + previewHeight);

        /**
         * Calculate size to scale full frame bitmap to smaller bitmap
         * Detect face in scaled bitmap have high performance than full bitmap.
         * The smaller image size -> detect faster, but distance to detect face shorter,
         * so calculate the size follow your purpose
         */
        if (previewWidth / 4 > 360) {
            prevSettingWidth = 360;
            prevSettingHeight = 270;
        } else if (previewWidth / 4 > 320) {
            prevSettingWidth = 320;
            prevSettingHeight = 240;
        } else if (previewWidth / 4 > 240) {
            prevSettingWidth = 240;
            prevSettingHeight = 160;
        } else {
            prevSettingWidth = 160;
            prevSettingHeight = 120;
        }

        cameraParameters.setPreviewSize(previewSize.width, previewSize.height);

        mFaceView.setPreviewWidth(previewWidth);
        mFaceView.setPreviewHeight(previewHeight);
    }

    private void setAutoFocus(Camera.Parameters cameraParameters) {
        List<String> focusModes = cameraParameters.getSupportedFocusModes();
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE))
            cameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
    }

    private void startPreview() {
        if (mCamera != null) {
            isThreadWorking = false;
            mCamera.startPreview();
            mCamera.setPreviewCallback(this);
            counter = 0;
        }
    }


    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        mCamera.setPreviewCallbackWithBuffer(null);
        mCamera.setErrorCallback(null);
        mCamera.release();
        mCamera = null;
    }


    @Override
    public void onPreviewFrame(byte[] _data, Camera _camera) {
        if (!isThreadWorking) {
            if (counter == 0)
                start = System.currentTimeMillis();

            isThreadWorking = true;
            waitForFdetThreadComplete();
            detectThread = new FaceDetectThread(handler, this);
            detectThread.setData(_data);
            detectThread.start();
        }
    }

    private void waitForFdetThreadComplete() {
        if (detectThread == null) {
            return;
        }

        if (detectThread.isAlive()) {
            try {
                detectThread.join();
                detectThread = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }


    // fps detect face (not FPS of camera)
    long start, end;
    int counter = 0;
    double fps;

    public void speakOutUser(String userId){
        if (audioManager.isPlaying())
            return;

        Log.i(TAG, "speakOutUser()");
        StringBuilder text = new StringBuilder();
        text.append("你好，");

        if (userId.equals(TTS_UNREGISTERED_USER)) {
            text.append("游客。");
        } else {
            String name = Util.hexStringToString(userId);
            text.append(name+"。");
        }

        //创建一个监听器
        synthesizerListener = new SynthesizerListener() {
            @Override
            public void onSpeakBegin() {
                Log.i(TAG, "--TTS--onSpeakBegin()--");
            }

            @Override
            public void onBufferProgress(int progress, int beginPos, int endPos,String info) {
                Log.i(TAG, "--TTS--onBufferProgress()--");
            }

            @Override
            public void onSpeakPaused() {
                Log.i(TAG, "--TTS--onSpeakPaused()--");
            }

            @Override
            public void onSpeakResumed() {
                Log.i(TAG, "--TTS--onSpeakResumed()--");
            }

            @Override
            public void onSpeakProgress(int progress,int beginPos,int endPos) {
                Log.i(TAG, "--TTS--onSpeakProgress()--");
            }

            @Override
            public void onCompleted(SpeechError speechError) {
                Log.i(TAG, "--TTS--onCompleted()--");

                //首先播放第0个音频
                audioManager.play(0);
            }

            //扩展用接口，由具体业务进行约定。
            @Override
            public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
                Log.i(TAG, "--TTS--onEvent()--");
            }
        };

        //设置声音文件的缓存。仅支持保存为 pcm 和 wav 格式
        String cacheFileName = getExternalCacheDir() + "/" + userId + ".pcm";
        //如果本地已经有离线缓存，则直接播放离线缓存文件
        if (isCacheExist(cacheFileName)) {
            ttsSynthesizer.setParameter(ResourceUtil.TTS_RES_PATH, cacheFileName);
            ttsSynthesizer.startSpeaking(text.toString(),synthesizerListener);
            Log.i(TAG, "播放离线缓存文件---");
        } else {
            //如果本地没有缓存，则播放在线数据的同时缓存到本地
            ttsSynthesizer.setParameter(SpeechConstant.TTS_AUDIO_PATH, cacheFileName);
            //开始播放
            ttsSynthesizer.startSpeaking(text.toString(),synthesizerListener );
            Log.i(TAG, "离线文件不存在,在线播放---");
        }

    }

    public boolean isCacheExist(String cacheFileName) {
        File f = new File(cacheFileName);
        if (f.exists()) {
            return true;
        } else {
            return false;
        }
    }
    //初始化语音合成器
    public void initSynthesizer() {

        //创建 SpeechSynthesizer 对象, 第二个参数：本地合成时传 InitListener，可以为Null
        ttsSynthesizer = SpeechSynthesizer.createSynthesizer(XBotFace.this, null);

        ttsSynthesizer.setParameter(SpeechConstant.VOICE_NAME, "xiaoyan"); //设置发音人
        ttsSynthesizer.setParameter(SpeechConstant.SPEED, "50");//设置语速
        ttsSynthesizer.setParameter(SpeechConstant.VOLUME, "80");//设置音量，范围 0~100
        ttsSynthesizer.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD); //设置云端

    }

    /**
     * Do face detect in thread
     */
    private class FaceDetectThread extends Thread {
        private Handler handler;
        private byte[] data = null;
        private Context ctx;
        private Bitmap faceCroped;

        public FaceDetectThread(Handler handler, Context ctx) {
            this.ctx = ctx;
            this.handler = handler;
        }


        public void setData(byte[] data) {
            this.data = data;
        }

        public void run() {
//            Log.i("FaceDetectThread", "running");

            float aspect = (float) previewHeight / (float) previewWidth;
            int w = prevSettingWidth;
            int h = (int) (prevSettingWidth * aspect);
            if (w % 2 == 1) w = w - 1;
            if (h % 2 == 1) h = h - 1;

            Bitmap bitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.RGB_565);
            // face detection: first convert the image from NV21 to RGB_565
            YuvImage yuv = new YuvImage(data, ImageFormat.NV21,
                    bitmap.getWidth(), bitmap.getHeight(), null);
            // TODO: make rect a member and use it for width and height values above
            Rect rectImage = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());

            // TODO: use a threaded option or a circular buffer for converting streams?
            //see http://ostermiller.org/convert_java_outputstream_inputstream.html
            ByteArrayOutputStream baout = new ByteArrayOutputStream();
            if (!yuv.compressToJpeg(rectImage, 100, baout)) {
                Log.e("CreateBitmap", "compressToJpeg failed");
            }

            BitmapFactory.Options bfo = new BitmapFactory.Options();
            bfo.inPreferredConfig = Bitmap.Config.RGB_565;
            bitmap = BitmapFactory.decodeStream(
                    new ByteArrayInputStream(baout.toByteArray()), null, bfo);

            Bitmap bmp = Bitmap.createScaledBitmap(bitmap, w, h, false);

            float xScale = (float) previewWidth / (float) prevSettingWidth;
            float yScale = (float) previewHeight / (float) h;

            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(cameraId, info);
            int rotate = mDisplayOrientation;
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT && mDisplayRotation % 180 == 0) {
                if (rotate + 180 > 360) {
                    rotate = rotate - 180;
                } else
                    rotate = rotate + 180;
            }

            switch (rotate) {
                case 90:
                    bmp = ImageUtils.rotate(bmp, 90);
                    xScale = (float) previewHeight / bmp.getWidth();
                    yScale = (float) previewWidth / bmp.getHeight();
                    break;
                case 180:
                    bmp = ImageUtils.rotate(bmp, 180);
                    break;
                case 270:
                    bmp = ImageUtils.rotate(bmp, 270);
                    xScale = (float) previewHeight / (float) h;
                    yScale = (float) previewWidth / (float) prevSettingWidth;
                    break;
            }

//            Log.e(TAG, "[w, h] = [" + Integer.toString(bmp.getWidth()) +
//                    ", " + Integer.toString(bmp.getHeight()));
            fdet = new android.media.FaceDetector(bmp.getWidth(), bmp.getHeight(), MAX_FACE);

            android.media.FaceDetector.Face[] fullResults = new android.media.FaceDetector.Face[MAX_FACE];
            fdet.findFaces(bmp, fullResults);

            for (int i = 0; i < MAX_FACE; i++) {
                if (fullResults[i] == null) {
                    faces[i].clear();
                } else {
                    PointF mid = new PointF();
                    fullResults[i].getMidPoint(mid);

                    mid.x *= xScale;
                    mid.y *= yScale;

                    float eyesDis = fullResults[i].eyesDistance() * xScale;
                    float confidence = fullResults[i].confidence();
                    float pose = fullResults[i].pose(android.media.FaceDetector.Face.EULER_Y);
                    int idFace = Id;

                    Rect rect = new Rect(
                            (int) (mid.x - eyesDis * 1.20f),
                            (int) (mid.y - eyesDis * 1.7f),
                            (int) (mid.x + eyesDis * 1.20f),
                            (int) (mid.y + eyesDis * 1.9f));

                    /**
                     * Only detect face size > 100x100
                     */
                    if (rect.height() * rect.width() > 90 * 60) {
                        for (int j = 0; j < MAX_FACE; j++) {
                            float eyesDisPre = faces_previous[j].eyesDistance();
                            PointF midPre = new PointF();
                            faces_previous[j].getMidPoint(midPre);

                            RectF rectCheck = new RectF(
                                    (midPre.x - eyesDisPre * 1.5f),
                                    (midPre.y - eyesDisPre * 1.9f),
                                    (midPre.x + eyesDisPre * 1.5f),
                                    (midPre.y + eyesDisPre * 2.2f));

                            if (rectCheck.contains(mid.x, mid.y)
                                    && (System.currentTimeMillis() - faces_previous[j].getTime()) < 1000) {
                                idFace = faces_previous[j].getId();
                                break;
                            }
                        }

                        if (idFace == Id) Id++;

                        faces[i].setFace(idFace, mid, eyesDis, confidence,
                                pose, System.currentTimeMillis());

                        faces_previous[i].set(faces[i].getId(), faces[i].getMidEye(),
                                faces[i].eyesDistance(), faces[i].getConfidence(),
                                faces[i].getPose(), faces[i].getTime());

                        //
                        // if focus in a face 5 frame -> take picture face display in RecyclerView
                        // because of some first frame have low quality
                        //
                        if (facesCount.get(idFace) == null) {
                            facesCount.put(idFace, 0);
                        } else {
                            int count = facesCount.get(idFace) + 1;
                            if (count <= 5)
                                facesCount.put(idFace, count);

                            //
                            // Crop Face to display in RecylerView
                            //
                            if (count == 5) {
                                faceCroped = ImageUtils.cropFace(faces[i], bitmap, rotate);
                                if (faceCroped != null) {
                                    handler.post(new Runnable() {
                                        public void run() {
                                            imagePreviewAdapter.add(faceCroped);

                                            YoutuConnection connection = new YoutuConnection(getApplicationContext(),handler);
                                            connection.sendBitmap(faceCroped);

                                            updateFaceState(DETECTEDSTATE);
                                        }
                                    });
                                }
                            }
                        }
                    }
                }
            }

            handler.post(new Runnable() {
                public void run() {
                    //send face to FaceView to draw rect
                    mFaceView.setFaces(faces);

                    //calculate FPS
                    end = System.currentTimeMillis();
                    counter++;
                    double time = (double) (end - start) / 1000;
                    if (time != 0)
                        fps = counter / time;

                    mFaceView.setFPS(fps);

                    if (counter == (Integer.MAX_VALUE - 1000))
                        counter = 0;
                    updateFaceState();

                    isThreadWorking = false;
                }
            });
        }
    }

    /**
     * Release Memory
     */
    private void resetData() {
        if (imagePreviewAdapter == null) {
            facesBitmap = new ArrayList<>();
            imagePreviewAdapter = new ImagePreviewAdapter(XBotFace.this, facesBitmap, new ImagePreviewAdapter.ViewHolder.OnItemClickListener() {
                @Override
                public void onClick(View v, int position) {
                    imagePreviewAdapter.setCheck(position);
                    imagePreviewAdapter.notifyDataSetChanged();
                }
            });
            recyclerView.setAdapter(imagePreviewAdapter);
        } else {
            imagePreviewAdapter.clearAll();
        }
    }

    public void updateFaceState(int state) {
        m_state = state;
        m_lastchangetime = System.currentTimeMillis();
        updateFace();
    }

    void updateFaceState() {
        long curr = System.currentTimeMillis();
        Log.d(TAG, "Curr(" + curr + " - last(" + m_lastchangetime+ ")=" + (curr - m_lastchangetime));
        if (curr - m_lastchangetime > 10000)
            updateFaceState(IDLESTATE);
    }
    void updateFace() {
        if (m_state == IDLESTATE)
            faceImageView.setImageResource(R.drawable.idleface);
        else if (m_state == DETECTEDSTATE)
            faceImageView.setImageResource(R.drawable.detectedface);
        else if (m_state == IDENTIFIEDSTATE)
            faceImageView.setImageResource(R.drawable.identifiedface);
        else
            Log.e(TAG, "updateFace: STATE ERROR");
    }


}
