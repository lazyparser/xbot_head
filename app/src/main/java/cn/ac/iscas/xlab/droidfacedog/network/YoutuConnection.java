package cn.ac.iscas.xlab.droidfacedog.network;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import cn.ac.iscas.xlab.droidfacedog.CameraActivity;
import cn.ac.iscas.xlab.droidfacedog.XBotFaceActivity;
import cn.ac.iscas.xlab.droidfacedog.config.Config;
import cn.ac.iscas.xlab.droidfacedog.entity.UserInfo;
import cn.ac.iscas.xlab.droidfacedog.util.ImageUtils;
import cn.ac.iscas.xlab.droidfacedog.util.Util;

import static cn.ac.iscas.xlab.droidfacedog.config.Config.RECOG_THRESHOLD;

/**
 * Created by lisongting on 2017/5/22.
 */

public class YoutuConnection {

    public static final String TAG = "YoutuConnection";
    private Context context;
    private Handler handler;
    private Bitmap userFace;
    private List<String> idList ;
    private List<UserInfo> userInfoList;
    private UserInfo info;

    public YoutuConnection(Context context) {
        this.context = context;
    }
    public YoutuConnection(Context context,Handler handler) {
        this.context = context;
        this.handler = handler;
    }

    public interface RecognitionCallback{

        void onResponse(String personId);

        void onFailure(String errorInfo);
    }

    public interface UserListCallback{

        void onUserInfoReady(UserInfo userInfo);

        void onBitmapReady(Bitmap bitmap);

        void onError();
    }

    //发送人脸bitmap给服务端进行人脸识别
    public void sendBitmap(Bitmap faceBitmap) {

        final String RECOG_SERVER_URL = "http://" + Config.RECOGNITION_SERVER_IP + ":" +
                Config.RECOGNITION_SERVER_PORT + "/recognition";

        final String encodedBitmap = ImageUtils.encodeBitmapToBase64(faceBitmap, Bitmap.CompressFormat.JPEG, 100);

        new Thread(){
            public void run(){
                //请求成功的回调
                Response.Listener<JSONObject> rightListener = new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject jsonObject) {
                        Log.i(TAG,"Recognition Right Response:"+jsonObject);
                        try {
                            double confidence = jsonObject.getDouble("Confidence");
                            String userId = jsonObject.getString("Id");
                            int ret = jsonObject.getInt("Ret");
                            Message msg = handler.obtainMessage();
                            msg.what = XBotFaceActivity.HANDLER_PLAY_TTS;
                            Bundle user = new Bundle();
                            //判断Ret字段是否是0,如果是0表示识别成功
                            if (ret == 0 && confidence>=RECOG_THRESHOLD) {
                                Log.i(TAG, "识别成功");
                                user.putString("userId",userId);
                                msg.setData(user);
                                msg.arg1 = ret;
                                handler.sendMessage(msg);
                            }else {
                                Log.i(TAG, "人脸识别失败或阈值设置过高");
                                //识别失败
                                user.putString("userId", XBotFaceActivity.TTS_UNREGISTERED_USER);
                                msg.setData(user);
                                msg.arg1 = ret;
                                handler.sendMessage(msg);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                };

                //请求失败的回调
                Response.ErrorListener errorListener = new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        Log.i(TAG,"Recognition Error:"+volleyError.getMessage());
                        if (handler != null) {
                            Message msg = handler.obtainMessage();
                            msg.what = XBotFaceActivity.HANDLER_PLAY_TTS;
                            Bundle user = new Bundle();
                            user.putString("userId", XBotFaceActivity.TTS_UNREGISTERED_USER);
                            msg.setData(user);
                            msg.arg1 = -1;
                            handler.sendMessage(msg);
                        }

                    }
                };

                //post的参数
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.accumulate("Image",encodedBitmap);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                        Request.Method.POST,
                        RECOG_SERVER_URL,
                        jsonObject,
                        rightListener,
                        errorListener
                );

                VolleySingleton.getVolleySingleton(context).addToRequestQueue(jsonObjectRequest);

            }
        }.start();

    }

    //以回调的方式返回结果给调用者
    public void recognizeFace(Bitmap faceBitmap, final RecognitionCallback callback){

        final String RECOG_SERVER_URL = "http://" + Config.RECOGNITION_SERVER_IP + ":" +
                Config.RECOGNITION_SERVER_PORT + "/recognition";

        final String encodedBitmap = ImageUtils.encodeBitmapToBase64(faceBitmap, Bitmap.CompressFormat.JPEG, 100);

        new Thread(){
            public void run(){
                //请求成功的回调
                Response.Listener<JSONObject> rightListener = new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject jsonObject) {
                        Log.i(TAG,"Recognition Right Response:"+jsonObject);
                        try {
                            double confidence = jsonObject.getDouble("Confidence");
                            String userId = jsonObject.getString("Id");
                            int ret = jsonObject.getInt("Ret");
                            //判断Ret字段是否是0,如果是0表示识别成功
                            if (ret == 0 && confidence>=RECOG_THRESHOLD) {
                                callback.onResponse(userId);
                                Log.i(TAG, "识别成功");
                            }else {
                                Log.i(TAG, "人脸识别失败或阈值设置过高");
                                //如果没有识别成功，则返回空串
                                callback.onResponse("");
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                };

                //请求失败的回调
                Response.ErrorListener errorListener = new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        callback.onFailure(volleyError.getMessage());
                    }
                };

                //post的参数
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.accumulate("Image",encodedBitmap);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                        Request.Method.POST,
                        RECOG_SERVER_URL,
                        jsonObject,
                        rightListener,
                        errorListener
                );

                VolleySingleton.getVolleySingleton(context).addToRequestQueue(jsonObjectRequest);

            }
        }.start();

    }

    //注册用户
    public void registerFace(String userName,Bitmap face) {

        final String REGISTER_URL = "http://"+Config.RECOGNITION_SERVER_IP+":"+
                Config.RECOGNITION_SERVER_PORT+"/management/register?method=force";

        final String userNameHex = Util.makeUserNameToHex(userName);

        final String encodedBitmap = ImageUtils.encodeBitmapToBase64(face, Bitmap.CompressFormat.JPEG, 100);

        new Thread(){
            public void run() {
                //请求成功的回调
                Response.Listener<JSONObject> rightListener = new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject jsonObject) {
                        Message msg = handler.obtainMessage();
                        try {
                            int ret = jsonObject.getInt("Ret");
                            if (ret == 0) {
                                msg.what = CameraActivity.REGISTER_SUCCESS;
                            } else if (ret == 1) {
                                msg.what = CameraActivity.REGISTER_TIMEOUT;
                            } else if (ret == 14) {
                                msg.what =CameraActivity.REGISTER_ALREADY_EXIST;
                            } else if (ret == 11) {
                                msg.what = CameraActivity.REGISTER_PIC_TOO_LARGE;
                            } else if(ret == 9){
                                msg.what = CameraActivity.REGISTER_HAS_NO_FACE;
                            }else {
                                msg.what = CameraActivity.REGISTER_FAIL;
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        handler.sendMessage(msg);
                    }
                };

                //请求失败的回调
                Response.ErrorListener errorListener = new Response.ErrorListener(){

                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        Log.i(TAG,"Register  Error:"+volleyError.getMessage());
                        Message msg = handler.obtainMessage();
                        msg.what = CameraActivity.REGISTER_TIMEOUT;
                        handler.sendMessage(msg);
                    }
                };

                //post的参数
                JSONObject postParams = new JSONObject();
                try {
                    postParams.put("Userid", userNameHex);
                    postParams.put("Image", encodedBitmap);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                        Request.Method.POST,
                        REGISTER_URL,
                        postParams,
                        rightListener,
                        errorListener);

                VolleySingleton.getVolleySingleton(context).addToRequestQueue(jsonObjectRequest);
            }
        }.start();

    }

    //获取某个已注册用户的人脸图像
    public Bitmap getUserFaceBitmap(String userId, final UserListCallback callback) {
        final String GET_USER_FACE_URL = "http://" + Config.RECOGNITION_SERVER_IP +
                ":" + Config.RECOGNITION_SERVER_PORT +
                "/face?userid=" + userId;

                Response.Listener<JSONObject> rightListener = new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject object) {
                        try {
                            int ret = object.getInt("Ret");
                            String strFace = object.optString("Image");
                            if (ret == 0) {
                                //将base64的String 转换为合适大小的Bitmap
                                userFace = ImageUtils.decodeBase64ToBitmap(strFace);
                                callback.onBitmapReady(userFace);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                };
                Response.ErrorListener errorListener = new Response.ErrorListener(){
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        Log.i(TAG, "getUserFaceBitmap()--Error:"+volleyError.getMessage());
                    }
                };
                JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                        Request.Method.GET,
                        GET_USER_FACE_URL,
                        null,
                        rightListener,
                        errorListener);
                VolleySingleton.getVolleySingleton(context).addToRequestQueue(jsonObjectRequest);
        return userFace;
    }

    public void getUserInfoList(final UserListCallback callback) {
        //获取所有注册用户的id
        final String GET_USER_ID_URL = "http://" + Config.RECOGNITION_SERVER_IP +
                ":" + Config.RECOGNITION_SERVER_PORT +
                "/management/userids";

        idList = new ArrayList<>();
        userInfoList = new ArrayList<>();

        new Thread(new Runnable() {
            @Override
            public void run() {
                Response.Listener<JSONObject> rightListener = new Response.Listener<JSONObject>(){
                    @Override
                    public void onResponse(JSONObject jsonObject) {
                        int ret = jsonObject.optInt("Ret");
                        if (ret == 0) {
                            JSONArray array = jsonObject.optJSONArray("Userids");
                            for(int i=0;i<array.length();i++) {
                                final String id = array.optString(i);
                                //嵌套的网络请求，先拿到用户id，再去请求头像数据
                                getUserFaceBitmap(id, new UserListCallback() {
                                    @Override
                                    public void onUserInfoReady(UserInfo userInfo) {

                                    }
                                    @Override
                                    public void onBitmapReady(Bitmap bitmap) {
                                        info = new UserInfo(Util.hexStringToString(id),bitmap);
                                        callback.onUserInfoReady(info);
                                    }
                                    @Override
                                    public void onError() {

                                    }
                                });

                            }
                        }

                    }
                } ;

                Response.ErrorListener errorListener = new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        Log.i(TAG, "getUserInfoList()--Error"+volleyError.getMessage());
                        callback.onError();
                    }
                };

                JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                        Request.Method.GET,
                        GET_USER_ID_URL,
                        null,
                        rightListener,
                        errorListener
                );

                VolleySingleton.getVolleySingleton(context).addToRequestQueue(jsonObjectRequest);
            }
        }).start();

    }



}
