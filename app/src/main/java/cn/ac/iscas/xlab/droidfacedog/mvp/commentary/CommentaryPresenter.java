package cn.ac.iscas.xlab.droidfacedog.mvp.commentary;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Binder;

import cn.ac.iscas.xlab.droidfacedog.RosConnectionService;
import cn.ac.iscas.xlab.droidfacedog.entity.AudioStatus;
import cn.ac.iscas.xlab.droidfacedog.entity.MuseumPosition;
import cn.ac.iscas.xlab.droidfacedog.model.AudioManager;
import cn.ac.iscas.xlab.droidfacedog.model.TTSModel;
import cn.ac.iscas.xlab.droidfacedog.network.YoutuConnection;
import cn.ac.iscas.xlab.droidfacedog.util.Util;
import de.greenrobot.event.EventBus;

/**
 * Created by lisongting on 2017/9/22.
 */

public class CommentaryPresenter implements CommentaryContract.Presenter{

    private RosConnectionService.ServiceBinder serviceBinder;
    private CommentaryContract.View view;
    private AudioManager audioManager;
    private TTSModel ttsModel;
    private YoutuConnection youtuConnection;
    private Context context;
    //表示机器人是否已经开始沿着指定路线运行
    private boolean isRobotStared;
    private int recogFailCount = 0;
    private int timeoutCount = 0;
    private boolean isWaitingRecogResult = false;

    public CommentaryPresenter(CommentaryContract.View view, Context context) {
        this.view = view;
        view.setPresenter(this);
        this.context = context;
    }

    @Override
    public void start() {
        youtuConnection = new YoutuConnection(context);
        ttsModel = new TTSModel(context, "xiaoyan");
        audioManager = new AudioManager(context);
        audioManager.loadTts();
        EventBus.getDefault().register(this);
    }

    @Override
    public void recognize(Bitmap bitmap) {
        if (isWaitingRecogResult) {
            return;
        }
        isWaitingRecogResult = true;
        youtuConnection.recognizeFace(bitmap, new YoutuConnection.RecognitionCallback() {
            @Override
            public void onResponse(String personId) {
                String chineseName = Util.hexStringToString(personId);
                if (personId.length() == 0) {
                    recogFailCount++;
                    if (recogFailCount == 3) {
                        view.changeUiState(CommentaryContract.STATE_DETECTED);
                        view.displayInfo("识别失败，请检查服务器配置或降低人脸检测阈值");
                        //表示没有识别出用户
                        audioManager.playAsync(11, new AudioManager.AudioCompletionCallback() {
                            @Override
                            public void onComplete(int id) {
                                serviceBinder.publishAudioStatus(new AudioStatus(0,true));
                            }
                        });
                        recogFailCount = 0;
                    }

                } else if (chineseName.equals("李松廷")) {
                    view.changeUiState(CommentaryContract.STATE_IDENTIFIED);
                    view.displayInfo("识别用户："+chineseName);
                    audioManager.playAsync(12, new AudioManager.AudioCompletionCallback() {
                        @Override
                        public void onComplete(int id) {
                            serviceBinder.publishAudioStatus(new AudioStatus(0,true));
                        }
                    });
                } else if (chineseName.equals("汪鹏")) {
                    view.changeUiState(CommentaryContract.STATE_IDENTIFIED);
                    view.displayInfo("识别用户："+chineseName);
                    audioManager.playAsync(13, new AudioManager.AudioCompletionCallback() {
                        @Override
                        public void onComplete(int id) {
                            serviceBinder.publishAudioStatus(new AudioStatus(0, true));
                        }
                    });
                } else {
                    view.changeUiState(CommentaryContract.STATE_IDENTIFIED);
                    view.displayInfo("识别用户："+chineseName);
                    //TODO:目前博物馆解说环境没有外网，如果有外网了之后将这里改为在线tts形式
                    audioManager.playAsync(11, new AudioManager.AudioCompletionCallback() {
                        @Override
                        public void onComplete(int id) {
                            serviceBinder.publishAudioStatus(new AudioStatus(0,true));
                        }
                    });
                }
                isWaitingRecogResult = false;
                view.closeCamera();
            }

            @Override
            public void onFailure(String errorInfo) {
                timeoutCount++;
                if (timeoutCount == 2) {
                    audioManager.playAsync(11, new AudioManager.AudioCompletionCallback() {
                        @Override
                        public void onComplete(int id) {
                            serviceBinder.publishAudioStatus(new AudioStatus(0,true));
                        }

                    });
                    view.displayInfo("人脸识别服务器连接超时");
                    view.closeCamera();
                    timeoutCount = 0;
                }
                isWaitingRecogResult = false;
            }
        });
    }

    @Override
    public void releaseMemory() {
        if (audioManager.isPlaying()) {
            audioManager.pause();
        }
        audioManager.releaseMemory();
        ttsModel.releaseMemory();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void setServiceProxy(Binder binder) {
        this.serviceBinder = (RosConnectionService.ServiceBinder) binder;
    }


    public void onEvent(MuseumPosition position) {
        int id = position.getLocationId();
        if (id == 0) {
            if (!isRobotStared) {
                view.startCamera();
                isRobotStared = true;
            } else {
                view.changeUiState(CommentaryContract.STATE_IDLE);
                //表示走完一圈又回到出发点
                isRobotStared = false;
            }
        } else if (id > 0) {
            audioManager.playAsync(id - 1, new AudioManager.AudioCompletionCallback() {
                @Override
                public void onComplete(int id) {
                    serviceBinder.publishAudioStatus(new AudioStatus(id, true));
                }
            });
        }
    }
}
