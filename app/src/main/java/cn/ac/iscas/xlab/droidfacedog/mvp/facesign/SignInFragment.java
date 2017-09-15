package cn.ac.iscas.xlab.droidfacedog.mvp.facesign;

import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.FaceDetector;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

import com.airbnb.lottie.LottieAnimationView;

import java.util.Timer;
import java.util.TimerTask;

import cn.ac.iscas.xlab.droidfacedog.R;
import cn.ac.iscas.xlab.droidfacedog.RosConnectionService;
import cn.ac.iscas.xlab.droidfacedog.entity.FaceResult;

/**
 * Created by lisongting on 2017/9/12.
 */

public class SignInFragment extends Fragment  implements SignInContract.View{

    public static final String TAG = "SignInFragment";
    LottieAnimationView lottieAnimationView;

    private TextureView textureView;
    private SurfaceTexture surfaceTexture;
    private Surface surface;

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
    //这个boolean表示将人脸发送给服务器后，当前是否正在等待优图服务器返回识别结果
    private boolean isWaitingRecogResult = false;

    //识别到的人脸的id（不是注册在服务端的ID）,初始为0。
    private int mPersonId =0;
    private long mTotalFrameCount = 0;
    //比例因子，将检测到的原始人脸图像按此比例缩小，以此可以加快FaceDetect的检测速度
    private double mScale = 0.2;
    //用来标记每个人脸共有几张图像,key是人脸的id，value是当前采集到的图像张数
    private SparseIntArray mFacesCountMap;

    public SignInFragment(){}

    public static SignInFragment newInstance() {
        return new SignInFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_sign, container, false);
        textureView = (TextureView)v.findViewById(R.id.texture_view);
        lottieAnimationView = (LottieAnimationView) v.findViewById(R.id.lottie_view);
        return v;
    }

    @Override
    public void onStart() {
        super.onStart();
        lottieAnimationView.playAnimation();
    }

    @Override
    public void onResume() {
        super.onResume();
        presenter = new SignInPresenter(this,getContext());
        presenter.start();
    }

    @Override
    public void initView() {

    }

    @Override
    public void setPresenter(SignInContract.Presenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void startCamera() {

    }

    @Override
    public void closeCamera() {

    }

    public void setRosServiceBinder(RosConnectionService.ServiceBinder binder){
        presenter.setServiceProxy(binder);
    }

    @Override
    public void displayInfo(String str) {
        Snackbar snackbar;
    }
}
