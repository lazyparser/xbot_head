package cn.ac.iscas.xlab.droidfacedog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
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
import com.jilk.ros.ROSClient;
import com.jilk.ros.rosbridge.ROSBridgeClient;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

import cn.ac.iscas.xlab.droidfacedog.config.Config;
import cn.ac.iscas.xlab.droidfacedog.util.ImageUtils;
import cn.ac.iscas.xlab.droidfacedog.util.Util;
import de.greenrobot.event.EventBus;

import static cn.ac.iscas.xlab.droidfacedog.PostImageForRecognitionAsync.XLAB;


/**
 * Created by Nguyen on 5/20/2016.
 */

/**
 * FACE DETECT EVERY FRAME WIL CONVERT TO RGB BITMAP SO THIS HAS LOWER PERFORMANCE THAN GRAY BITMAP
 * COMPARE FPS (DETECT FRAME PER SECOND) OF 2 METHODs FOR MORE DETAIL
 */

public final class XBotFace extends AppCompatActivity implements SurfaceHolder.Callback, Camera.PreviewCallback {

    public static final int IDLESTATE = 0;
    public static final int DETECTEDSTATE = 1;
    public static final int IDENTIFIEDSTATE = 2;
    public static final int TTS_WELCOME = 7;
    //public static final int TTS_HELLO = 0;
    // public static final int TTS_UNREGISTERED_USER = 1;
    public static final int TTS_REGISTERED_USER = 2;
    public static final int TTS_USER_WANGPENG = 3;
    public static final int TTS_USER_CCK = 4;
    public static final int TTS_USER_XUZHIHAO = 5;
    public static final int TTS_USER_WUYANJUN = 6;
    public static final int CONN_ROS_SERVER_SUCCESS = 0x11;
    public static final int CONN_ROS_SERVER_ERROR = 0x12;
    public static final String TTS_UNREGISTERED_USER = "0000000000";
    // Number of Cameras in device.
    private int numberOfCameras;

    public static final String TAG = XBotFace.class.getSimpleName();

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

    private static final int MAX_SOUNDS=5;
    private SoundPool mSoundPool;
    private static final String SOUNDS_FOLDER = "tts";
    private AssetManager mAssets;
    private List<Sound> mSounds = new ArrayList<>();

    private Queue<MediaPlayer> ttsQueue;
    private List<MediaPlayer> ttsList;
    private int currentPlayId;
    private boolean isPlayingTTS;
    private MediaPlayer mCurrentPlayer;

    private Timer timer;
    private AlertDialog dialog;

    //科大讯飞的语音合成器[需要联网才能使用]
    private SpeechSynthesizer ttsSynthesizer;
    private SynthesizerListener synthesizerListener;

    /**
     * Initializes the UI and initiates the creation of a face detector.
     */
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

        //定义Handler，用来接收TimerTask中发回来的连接状态
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

        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.xbotface_title);
        getSupportActionBar().hide();

        if (icicle != null)
            cameraId = icicle.getInt(BUNDLE_CAMERA_ID, 0);

        mAssets = getAssets();

//        mSoundPool = new SoundPool(MAX_SOUNDS, AudioManager.STREAM_MUSIC, 0);
//        loadSounds();

        //在这里Activity的生命周期里直接加载TTS,这样会比较耗时大概会导致Activity延迟1秒启动
        //loadTTS(this);
        //所以新开线程去加载TTS
        new Thread(){
            public void run() {
                loadTTS(XBotFace.this);
            }
        }.start();

        isPlayingTTS = false;
        mCurrentPlayer = null;

        //启动一个对话框提示用户等待，然后连接至Ros服务器
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("请稍等")
                .setMessage("正在连接至Ros服务端")
                .setCancelable(false)
                .setNegativeButton("取消连接", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        XBotFace.this.onBackPressed();
                        timer.cancel();
                    }
                });
        dialog = builder.create();
        dialog.show();


        //启动定时任务，每3秒种发起一次连接
        //然后将结果发送给Handler，
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                boolean result = connectToRosServer();
                if(result){
                    handler.sendEmptyMessage(CONN_ROS_SERVER_SUCCESS);
                }else{
                    handler.sendEmptyMessage(CONN_ROS_SERVER_ERROR);
                }
            }
        },0,3000);

        SpeechUtility.createUtility(this, SpeechConstant.APPID +"="+Config.APPID);
    }

    private boolean connectToRosServer(){
        String rosIP = "192.168.1.151";
        String rosPort = "9090";
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        rosIP = prefs.getString("rosserver_ip_address", "192.168.1.151");
        String rosURI = "ws://" + rosIP + ":" + rosPort;
        Log.d(TAG, "Connecting ROS " + rosURI);
        final ROSBridgeClient client = new ROSBridgeClient(rosURI);
        // TODO check return value of client.connect()
        boolean conneSucc = client.connect(new ROSClient.ConnectionStatusListener() {
            @Override
            public void onConnect() {
                client.setDebug(true);
                ((XbotApplication)getApplication()).setRosClient(client);
                Log.d(TAG,"Connect ROS success");
            }

            @Override
            public void onDisconnect(boolean normal, String reason, int code) {
                Log.d(TAG,"ROS disconnect");
            }

            @Override
            public void onError(Exception ex) {
                ex.printStackTrace();
                Log.d(TAG,"ROS communication error");
            }
        });
        if (conneSucc) {
            RosBridgeCommunicateThread thread = new RosBridgeCommunicateThread<PublishEvent>(client);
            thread.start();
            thread.getLooper();
            ((XbotApplication)getApplication()).setRosThread(thread);
            Log.i(TAG, "Background RosBridgeCommunicateThread thread started");
            thread.beginPublishTopicSpeakerDone();
        }
        return conneSucc;
    }

    public void startPlayTTS() {
        if (isPlayingTTS)
            return;
        isPlayingTTS = true;
        RosBridgeCommunicateThread rosCommunicationThread = ((XbotApplication)getApplication()).getRosThread();
        if (rosCommunicationThread != null) {
            rosCommunicationThread.updateSpeakerState(true);
            playNext();
        }else{
            Log.d(TAG,"RosBridgeCommunicateThread is null");
        }
    }
    private void loadTTS(final XBotFace xbotface) {
        ttsList = new ArrayList<>();
        ttsQueue = new ArrayDeque<>();
        currentPlayId = 0;
        String[] ttsFileList = {
//                "tts/hello.mp3",
//                "tts/guest.mp3",
//                "tts/recognized_user.mp3",
//                "tts/name_wangpeng.mp3",
//                "tts/name_chaichangkun.mp3",
//                "tts/name_xuzhihao.mp3",
//                "tts/name_wuyanjun.mp3",
                "tts/welcome.mp3",
                "tts/HISTORY01.mp3",
                "tts/HISTORY02.mp3",
                "tts/HISTORY03.mp3",
                "tts/HISTORY04.mp3",
                "tts/HISTORY05.mp3",
                "tts/HISTORY06.mp3",
                "tts/HISTORY07.mp3",
                "tts/HISTORY08.mp3",
                "tts/HISTORY09.mp3",
                "tts/HISTORY10.mp3",
                "tts/HISTORY11.mp3",
                "tts/HISTORY12.mp3",
                "tts/HISTORY13.mp3",
                "tts/HISTORY14.mp3",
                "tts/HISTORY15.mp3",
                "tts/HISTORY16.mp3",
                "tts/HISTORY17.mp3",
                "tts/HISTORY18.mp3"};
        for (int i = 0; i < ttsFileList.length; i++) {
            try {
                AssetFileDescriptor afd = getAssets().openFd(ttsFileList[i]);
                MediaPlayer mp = new MediaPlayer();
                mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                mp.prepare();
                // Toast.makeText(xbotface, "Loading ttsList[" + Integer.toString(i) + "]",
                //         Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Loading ttsList[" + Integer.toString(i) + "]");
                mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        xbotface.playNext();
                    }
                });
                ttsList.add(mp);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        EventBus.getDefault().register(this);
    }

    public void onEvent(final NarrateStatusChangeRequest req) {
        if (req.getRequest() == NarrateStatusChangeRequest.PlayStatus.PAUSE)
            pauseNarrating();
        else if (req.getRequest() == NarrateStatusChangeRequest.PlayStatus.STOP)
            stopNarrating();
        else if (req.getRequest() == NarrateStatusChangeRequest.PlayStatus.START)
            startPlayTTS();
        else //(req.getRequest() == NarrateStatusChangeRequest.PlayStatus.RESUME)
            resumeNarrating();
    }

    private void pauseNarrating() {
        // TODO stub
        if (mCurrentPlayer != null)
            mCurrentPlayer.pause();
    }

    private void stopNarrating() {
        // TODO stub
        if(mCurrentPlayer != null)
            mCurrentPlayer.stop();
    }

    private void resumeNarrating() {
        //TODO stub
        if (mCurrentPlayer != null)
            mCurrentPlayer.start();
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

    /**
     * Releases the resources associated with the camera source, the associated detector, and the
     * rest of the processing pipeline.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
//        releaseSounds();
        resetData();
        isPlayingTTS = false;
        RosBridgeCommunicateThread rosCommunicationThread = ((XbotApplication)getApplication()).getRosThread();
        if (rosCommunicationThread != null) {
            rosCommunicationThread.updateSpeakerState(false);
            rosCommunicationThread.stopPublishTopicSpeakerDone();
        }else{
            Log.d(TAG, "RosBridgeCommunicateThread is null");
        }

        for (int i = 0; i < ttsList.size(); ++i) {
            MediaPlayer mp = ttsList.get(i);
            mp.stop();
            mp.release();
        }
        ttsSynthesizer.stopSpeaking();
        ttsSynthesizer.destroy();
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

    private void playNext() {
        if (ttsQueue.isEmpty()) {
            isPlayingTTS = false;
            RosBridgeCommunicateThread rosCommunicationThread = ((XbotApplication)getApplication()).getRosThread();
            if (rosCommunicationThread != null) {
                rosCommunicationThread.updateSpeakerState(false);
            }else{
                Log.d(TAG, "RosBridgeCommunicateThread is null");
            }
            return;
        }
        mCurrentPlayer = ttsQueue.poll();
        mCurrentPlayer.start();
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

    //        1977976464 汪鹏
    //        2782058378 柴长坤
    //        3321435094 徐志浩
    //        0051424595 屈晟 (NOT INPLEMENTED YET)
    //        3831542170 武延军
    //根据不同的用户有不同的问候语
    public void speekOutUser(String userId){
        if (isPlayingTTS)
            return;
        StringBuilder text = new StringBuilder();
        text.append("你好，");
        if (userId.equalsIgnoreCase("1977976464")
                || userId.equalsIgnoreCase("wangpeng")){
            text.append("汪鹏。");
        }else if (userId.equalsIgnoreCase("2782058378")){
            text.append("柴长昆。");
        }else if (userId.equalsIgnoreCase("3321435094")){
            text.append("徐志浩。");
        }else if (userId.equalsIgnoreCase("3831542170")){
            text.append("武延军。");
        }else if (userId.equalsIgnoreCase("0051424595")) {
            text.append("注册用户。");
        }else if(userId.equals(TTS_UNREGISTERED_USER)){
            text.append("游客。");
        }

        //创建一个监听器
        synthesizerListener = new SynthesizerListener() {
            @Override
            public void onSpeakBegin() {
                Log.i(TAG, "--TTS--onSpeakBegin()--");
            }

            @Override
            public void onBufferProgress(int i, int i1, int i2, String s) {
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
            public void onSpeakProgress(int i, int i1, int i2) {
                Log.i(TAG, "--TTS--onSpeakProgress()--");
            }

            @Override
            public void onCompleted(SpeechError speechError) {
                Log.i(TAG, "--TTS--onCompleted()--");

                enqueueSpeekingResources();
                Log.i(XLAB, "activity.startPlayTTS();");
                startPlayTTS();
            }

            @Override
            public void onEvent(int i, int i1, int i2, Bundle bundle) {
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

        ttsSynthesizer.setParameter(SpeechConstant.VOICE_NAME, "xiaoyu"); //设置发音人
        ttsSynthesizer.setParameter(SpeechConstant.SPEED, "50");//设置语速
        ttsSynthesizer.setParameter(SpeechConstant.VOLUME, "80");//设置音量，范围 0~100
        ttsSynthesizer.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD); //设置云端

    }
    //在该方法中将所有要播放的音频同一加入到一个队列中。然后调用startPlayTTS()播放
    public void enqueueSpeekingResources() {
        // If is playing. do nothing.
        if (isPlayingTTS)
            return;
        for (int i = 0; i < ttsList.size(); ++i)
            ttsQueue.add(ttsList.get(i));
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
