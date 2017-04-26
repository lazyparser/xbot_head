package cn.ac.iscas.xlab.droidfacedog;

import android.app.AlertDialog;
import android.content.Context;
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Queue;

import de.greenrobot.event.EventBus;


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
    public static final int TTS_HELLO = 0;
    public static final int TTS_UNREGISTERED_USER = 1;
    public static final int TTS_REGISTERED_USER = 2;
    public static final int TTS_USER_WANGPENG = 3;
    public static final int TTS_USER_CCK = 4;
    public static final int TTS_USER_XUZHIHAO = 5;
    public static final int TTS_USER_WUYANJUN = 6;
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

//    private void loadSounds() {
//        String[] soundNames;
//        try {
//            soundNames = mAssets.list(SOUNDS_FOLDER);
//            Log.d(TAG, "xxlab Found " + soundNames.length + " sounds.");
//
//        } catch (IOException ioe){
//            Log.e(TAG, "ERROR Could not list assets", ioe);
//            return;
//        }
//
//        for (String filename : soundNames) {
//            String assetPath = SOUNDS_FOLDER + "/" + filename;
//            Sound sound = new Sound(assetPath);
//            try {
//                load(sound);
//                mSounds.add(sound);
//                Log.d(TAG, "filename = " + filename);
//            } catch (IOException ioe) {
//                Log.e(TAG, "ERROR Could not load sound file '" + filename + "': ", ioe);
//            }
//        }
//    }

//    private void load(Sound sound) throws IOException {
//        AssetFileDescriptor afd = mAssets.openFd(sound.getAssetPath());
//        int soundId = mSoundPool.load(afd, 1);
//        sound.setSoundId(soundId);
//    }

//    public void play(Sound sound) {
//        Integer soundId = sound.getSoundId();
//        if (soundId == null) {
//            return;
//        }
//        mSoundPool.play(soundId, 1.0f, 1.0f, 1, 0, 1.0f);
//    }

//    public List<Sound> getSounds() {
//        return mSounds;
//    }

//    public void releaseSounds() {
//        mSoundPool.release();
//    }

    //==============================================================================================
    // Activity Methods
    //==============================================================================================

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


        handler = new Handler();
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

        loadTTS(this);
        isPlayingTTS = false;
        mCurrentPlayer = null;

        // TODO: make rosPort configurable.
        // TODO: make rosIP configurable.
        String rosIP = "192.168.1.111";
        String rosPort = "9090";
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        rosIP = prefs.getString("rosserver_ip_address", "192.168.1.111");
        String rosURI = "ws://" + rosIP + ":" + rosPort;
        Log.d(TAG, "Connecting ROS " + rosURI);
//        final ROSBridgeClient client = new ROSBridgeClient(rosURI);
//        // TODO check return value of client.connect()
//        boolean conneSucc = client.connect(new ROSClient.ConnectionStatusListener() {
//            @Override
//            public void onConnect() {
//                client.setDebug(true);
//                ((XbotApplication)getApplication()).setRosClient(client);
//                Log.d(TAG,"Connect ROS success");
//            }
//
//            @Override
//            public void onDisconnect(boolean normal, String reason, int code) {
//                Log.d(TAG,"ROS disconnect");
//            }
//
//            @Override
//            public void onError(Exception ex) {
//                ex.printStackTrace();
//                Log.d(TAG,"ROS communication error");
//            }
//        });
//
//        checkConnectionResult(conneSucc);
//
//        RosBridgeCommunicateThread thread = new RosBridgeCommunicateThread<PublishEvent>(client);
//        thread.start();
//        thread.getLooper();
//        ((XbotApplication)getApplication()).setRosThread(thread);
//        Log.i(TAG, "Background RosBridgeCommunicateThread thread started");
//        thread.beginPublishTopicSpeakerDone();
    }

    private void checkConnectionResult(boolean conneSucc) {
        if (!conneSucc) {
            this.onBackPressed();
            Toast.makeText(getApplicationContext(), "连接Ros服务端失败,请开启服务端后重试", Toast.LENGTH_SHORT).show();
        }
    }

    public void startPlayTTS() {
        if (isPlayingTTS)
            return;
        isPlayingTTS = true;
        RosBridgeCommunicateThread rosCommunicationThread = ((XbotApplication)getApplication()).getRosThread();
        if (rosCommunicationThread != null) {
            rosCommunicationThread.updateSpeakerState(true);
        }else{
            Log.d(TAG,"RosBridgeCommunicateThread is null");
        }
        playNext();
    }
    private void loadTTS(final XBotFace xbotface) {
        ttsList = new ArrayList<>();
        ttsQueue = new ArrayDeque<>();
        currentPlayId = 0;
        String[] ttsFileList = {
                "tts/hello.mp3",
                "tts/guest.mp3",
                "tts/recognized_user.mp3",
                "tts/name_wangpeng.mp3",
                "tts/name_chaichangkun.mp3",
                "tts/name_xuzhihao.mp3",
                "tts/name_wuyanjun.mp3",
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

        Log.i(TAG, "onResume");
        startPreview();
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
        startPreview();
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

    public MediaPlayer lookupNames(String id) {
        Log.w(TAG, "lookupNames(String '" + id + "')");

        // TODO: lookup wangpeng and others here.
//        1977976464 汪鹏
//        2782058378 柴长坤
//        3321435094 徐志浩
//        0051424595 屈晟 (NOT INPLEMENTED YET)
//        3831542170 武延军
//        "tts/hello.mp3",
//                "tts/guest.mp3",
//                "tts/recognized_user.mp3",
//        3        "tts/name_wangpeng.mp3",
//        4        "tts/name_chaichangkun.mp3",
//        5        "tts/name_xuzhihao.mp3",
//        6        "tts/name_wuyanjun.mp3",
//                "tts/welcome.mp3",
        if (id.equalsIgnoreCase("1977976464")
                || id.equalsIgnoreCase("wangpeng"))
            return ttsList.get(TTS_USER_WANGPENG);
        if (id.equalsIgnoreCase("2782058378"))
            return ttsList.get(TTS_USER_CCK);
        if (id.equalsIgnoreCase("3321435094"))
            return ttsList.get(TTS_USER_XUZHIHAO);
        if (id.equalsIgnoreCase("0051424595"))
            return ttsList.get(TTS_REGISTERED_USER);
        if (id.equalsIgnoreCase("3831542170"))
            return ttsList.get(TTS_USER_WUYANJUN);

        // if not found, return a generic name.
        return ttsList.get(TTS_REGISTERED_USER);
    }

    public void prepareGreetingTTS(MediaPlayer ttsUserId) {
        // If is playing. do nothing.
        if (isPlayingTTS)
            return;
        ttsQueue.add(ttsList.get(TTS_HELLO));
        ttsQueue.add(ttsUserId);
        for (int i = TTS_WELCOME; i < ttsList.size(); ++i)
            ttsQueue.add(ttsList.get(i));
    }
    public void prepareGreetingTTS() {
        prepareGreetingTTS(ttsList.get(TTS_UNREGISTERED_USER));
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
