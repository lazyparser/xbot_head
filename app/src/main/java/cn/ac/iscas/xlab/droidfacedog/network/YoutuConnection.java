package cn.ac.iscas.xlab.droidfacedog.network;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;

import cn.ac.iscas.xlab.droidfacedog.XBotFace;
import cn.ac.iscas.xlab.droidfacedog.config.Config;

import static cn.ac.iscas.xlab.droidfacedog.config.Config.RECOG_THRESHOLD;

/**
 * Created by lisongting on 2017/5/22.
 */

public class YoutuConnection {

    public static final String TAG = "YoutuConnection";
    private Context context;
    private Handler handler;
    public YoutuConnection(Context context,Handler handler) {
        this.context = context;
        this.handler = handler;
    }
    public void sendBitmap(Bitmap faceBitmap) {

        final String RECOG_SERVER_URL = "http://" + Config.RECOGNITION_SERVER_IP + ":" +
                Config.RECOGNITION_SERVER_PORT + "/recognition";

        final String encodedBitmap = encodeToBase64(faceBitmap, Bitmap.CompressFormat.JPEG, 100);

        new Thread(){
            public void run(){
                //请求成功的回调
                Response.Listener<JSONObject> rightListener = new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject jsonObject) {
                        Log.i(TAG,"Right Response:"+jsonObject);
                        Toast.makeText(context, jsonObject.toString(), Toast.LENGTH_SHORT).show();
                        try {
                            double confidence = jsonObject.getDouble("Confidence");
                            String userId = jsonObject.getString("Id");
                            int ret = jsonObject.getInt("Ret");
                            Message msg = handler.obtainMessage();
                            msg.what = XBotFace.HANDLER_PLAY_TTS;
                            Bundle user = new Bundle();
                            //判断Ret字段是否是0,如果是0表示识别成功
                            if (ret == 0 && confidence>=RECOG_THRESHOLD) {
                                Log.i(TAG, "识别成功");
                                user.putString("userId",userId);
                                msg.setData(user);
                                handler.sendMessage(msg);
                            }else {
                                Log.i(TAG, "人脸识别失败或阈值设置过高");
                                //识别失败
                                user.putString("userId", XBotFace.TTS_UNREGISTERED_USER);
                                msg.setData(user);
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
                        Log.i(TAG,volleyError.getMessage());
                        Message msg = handler.obtainMessage();
                        msg.what = XBotFace.HANDLER_PLAY_TTS;
                        Bundle user = new Bundle();
                        user.putString("userId", XBotFace.TTS_UNREGISTERED_USER);
                        msg.setData(user);
                        handler.sendMessage(msg);
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

    public static String encodeToBase64(Bitmap image, Bitmap.CompressFormat compressFormat, int quality)
    {
        ByteArrayOutputStream byteArrayOS = new ByteArrayOutputStream();
        image.compress(compressFormat, quality, byteArrayOS);
        return Base64.encodeToString(byteArrayOS.toByteArray(), Base64.DEFAULT);
    }
}
