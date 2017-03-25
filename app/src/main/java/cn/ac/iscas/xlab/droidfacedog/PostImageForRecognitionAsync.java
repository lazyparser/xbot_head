package cn.ac.iscas.xlab.droidfacedog;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.JsonReader;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.ac.iscas.xlab.droidfacedog.youtu.RecogResult;

/**
 * Created by Lazyparser on 10/19/16.
 */

// https://developer.android.com/reference/android/os/AsyncTask.html

public class PostImageForRecognitionAsync extends AsyncTask<Bitmap, Void, Integer> {
    public static final int RECOG_SUCCESS = 0;
    public static final int RECOG_REJECTED = 1;
    public static final int RECOG_TIMEOUT = 2;
    public static final int RECOG_INVALID_URL = 3;
    private static final int RECOG_SERVER_ERROR = 4;
    public static final String XLAB = "xxlab";
    public static final String SERVER_IP_ADDRESS = "server_ip_address";
    public static final String DEFAULT_IP = "192.168.1.60";
    public RecogResult mRecogResult;


    private String serverAddress;
    // http://stackoverflow.com/questions/3698034/validating-ip-in-android
    private static Pattern IP_ADDRESS = Pattern.compile(
            "((25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\\.(25[0-5]|2[0-4]"
                    + "[0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1]"
                    + "[0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}"
                    + "|[1-9][0-9]|[0-9]))");
    private Context mContext;

    public PostImageForRecognitionAsync() {
        super();
    }

    protected Integer doInBackground(Bitmap... faceImages) {
        if (serverAddress == null) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
            serverAddress = prefs.getString(SERVER_IP_ADDRESS, DEFAULT_IP);
        }
        if (serverAddress.equals("")) {
            return RECOG_INVALID_URL;
        }
        // http://stackoverflow.com/questions/3698034/validating-ip-in-android
        Matcher matcher = IP_ADDRESS.matcher(serverAddress);
        if (!matcher.matches()) {
            Log.d(XLAB, "IP validation failed: " + serverAddress);
            return RECOG_INVALID_URL;
        }

        Log.d(XLAB, "IP is validated: " + serverAddress);

        // http://www.wikihow.com/Execute-HTTP-POST-Requests-in-Android
        // http://stackoverflow.com/questions/6218143/how-to-send-post-request-in-json-using-httpclient
        // http://stackoverflow.com/questions/13911993/sending-a-json-http-post-request-from-android
        HttpURLConnection client = null;
        try {
            BufferedOutputStream outputStream;
            // TODO: extract the url to youtu package.
            URL url = new URL("http://" + serverAddress + ":8000/recognition");
            client = (HttpURLConnection) url.openConnection();
            client.setRequestMethod("POST");
            client.setDoOutput(true);
            client.setDoInput(true);
            client.setUseCaches(false);

            //client.setChunkedStreamingMode(0);

            // 3. build jsonObject
            JSONObject jsonObject = new JSONObject();
            jsonObject.accumulate("Image", encodeToBase64(faceImages[0], Bitmap.CompressFormat.JPEG, 100));

            // 4. convert JSONObject to JSON to String
            String jsonString = jsonObject.toString();
            Log.d(XLAB, jsonString);

            client.setRequestProperty("Content-Length", Integer.toString(jsonString.length()));
            client.setRequestProperty("Content-Type","application/json");
            client.setRequestProperty("Connection", "close");
            client.setRequestProperty("Accept-Encoding", "identity");
            client.setRequestProperty("Accept", "text/plain");

            outputStream = new BufferedOutputStream(client.getOutputStream());
            outputStream.write(jsonString.getBytes());
            outputStream.flush();
            outputStream.close();

            int status = client.getResponseCode();
            Log.d(XLAB, "RESPONSE CODE: " + Integer.toString(status));
            Log.d(XLAB, "POST ERROR STRING: " + client.getResponseMessage());

            // Ref: HTTP Return Code 200-399 is ok. return code above 400 means error.
            if (status < 400) {
                InputStream in = client.getInputStream();
                JsonReader reader = new JsonReader(new InputStreamReader(in, "UTF-8"));

                RecogResult recogResult = new RecogResult();
                if (!recogResult.parseFrom(reader))
                    return RECOG_SERVER_ERROR;

                mRecogResult = recogResult;
                return RECOG_SUCCESS;
            } else {
                // FIXME: 1024 has no means. It is just a buffer length.
                // this block read the errorStream and logcat it.
                byte[] buf = new byte[1024];
                BufferedInputStream errReader = new BufferedInputStream(client.getErrorStream());
                int l = errReader.read(buf);
                Log.d(XLAB, "RESPONSE ERROR: " + new String(buf, 0, l) + " len " + l);
                return RECOG_SERVER_ERROR;
            }

            //inputStream.close();
        } catch (MalformedURLException e) {
            e.printStackTrace();
            Log.d(XLAB, "oh no, catch (MalformedURLException e)");
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(XLAB, "oh no, catch (IOException e)");
        } catch (JSONException e) {
            e.printStackTrace();
            Log.d(XLAB, "oh no, catch (JSONException e)");
        } finally {
            Log.d(XLAB, "FINALLY");
            if(client != null) // Make sure the connection is not null.
                client.disconnect();
        }

        return RECOG_TIMEOUT;
    }

    protected void onPostExecute(Integer result) {
        Log.d(XLAB, "PostImageForRecognitionAsync onPostExecute [" + result + "]");
        Toast.makeText(mContext, Integer.toString(result), Toast.LENGTH_LONG).show();
        //FIXME: very bad string magic. refactr it.
        if (result == RECOG_SUCCESS) {
            if (mContext instanceof XBotFace) {
                XBotFace activity = (XBotFace) mContext;
                activity.updateFaceState(XBotFace.IDENTIFIEDSTATE);

                for (Sound s : activity.getSounds()) {
                    // XXX: short-circuit this function now for demo purpose.
                    if (s.getAssetPath().equals("tts/demo.mp3"))
                        activity.play(s);
                }
            }
        }
    }

    // http://stackoverflow.com/questions/16920942/getting-context-in-asynctask
    public void setContext(Context context) {
        mContext = context;
    }

    // http://stackoverflow.com/questions/9768611/encode-and-decode-bitmap-object-in-base64-string-in-android
    public static String encodeToBase64(Bitmap image, Bitmap.CompressFormat compressFormat, int quality)
    {
        ByteArrayOutputStream byteArrayOS = new ByteArrayOutputStream();
        image.compress(compressFormat, quality, byteArrayOS);
        return Base64.encodeToString(byteArrayOS.toByteArray(), Base64.DEFAULT);
    }

    // http://stackoverflow.com/questions/9768611/encode-and-decode-bitmap-object-in-base64-string-in-android
    public static Bitmap decodeBase64(String input)
    {
        byte[] decodedBytes = Base64.decode(input, 0);
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
    }

}

