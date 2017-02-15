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
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Lazyparser on 10/19/16.
 */

// https://developer.android.com/reference/android/os/AsyncTask.html

public class PostImageForRecognitionAsync extends AsyncTask<Bitmap, Void, String> {
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

    protected String doInBackground(Bitmap... faceImage) {
        if (serverAddress == null) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
            serverAddress = prefs.getString("server_ip_address", "192.168.1.60");
        }
        if (serverAddress.equals("")) {
            return "WARNING: IP Address is Empty.";
        }
        // http://stackoverflow.com/questions/3698034/validating-ip-in-android
        Matcher matcher = IP_ADDRESS.matcher(serverAddress);
        if (matcher.matches()) {
            Log.d("xxlab", "IP is validated: " + serverAddress);
            // ip is correct

            // http://www.wikihow.com/Execute-HTTP-POST-Requests-in-Android
            // http://stackoverflow.com/questions/6218143/how-to-send-post-request-in-json-using-httpclient
            // http://stackoverflow.com/questions/13911993/sending-a-json-http-post-request-from-android

            HttpURLConnection client = null;
            try {
                BufferedOutputStream outputStream;
//                DataInputStream inputStream;
//                URL url = new URL("http://" + serverAddress + ":8000/verification");
                URL url = new URL("http://" + serverAddress + ":8000/recognition");
                client = (HttpURLConnection) url.openConnection();
                client.setRequestMethod("POST");
//                client.setRequestProperty("key","value");
                client.setDoOutput(true);
                client.setDoInput(true);
                client.setUseCaches(false);

                //client.setChunkedStreamingMode(0);

                // 3. build jsonObject
                JSONObject jsonObject = new JSONObject();
                jsonObject.accumulate("Image", encodeToBase64(faceImage[0], Bitmap.CompressFormat.JPEG, 100));

                // 4. convert JSONObject to JSON to String
                String json = jsonObject.toString();
                Log.d("xxlab", json);

                client.setRequestProperty("Content-Length", Integer.toString(json.length()));
                client.setRequestProperty("Content-Type","application/json");
                client.setRequestProperty("Connection", "close");
                client.setRequestProperty("Accept-Encoding", "identity");
                client.setRequestProperty("Accept", "text/plain");


                outputStream = new BufferedOutputStream(client.getOutputStream());
                //outputStream.writeBytes(URLEncoder.encode(json, "UTF-8"));
                outputStream.write(json.getBytes());
                outputStream.flush();

//                client.connect();
                outputStream.close();
                int status = client.getResponseCode();
                Log.d("xxlab", "RESPONSE: " + status);
                Log.d("xxlab", "POST ERROR STRING: " + client.getResponseMessage());

                if (status >= 400) {
                    byte[] buf = new byte[1024];
                    BufferedInputStream errReader = new BufferedInputStream(client.getErrorStream());
                    int l = errReader.read(buf);
                    Log.d("xxlab", "RESPONSE ERROR: " + new String(buf, 0, l) + " len " + l);
                    return new String(buf, 0, l);

                } else {
                    JsonReader jsonReader = new JsonReader(new InputStreamReader(client.getInputStream(), "UTF-8"));
                    Log.d("xxlab", "RESPONSE: " + jsonReader.toString());
                    //String ret = jsonReader.toString();
//                //inputStream = new DataInputStream(client.getInputStream());
                    //jsonReader.close();
                    //return ret;
                    return "检测通过";

                }

                //inputStream.close();
            } catch (MalformedURLException e) {
                e.printStackTrace();
                Log.d("xxlab", "oh no, catch (MalformedURLException e)");
            } catch (IOException e) {
                e.printStackTrace();
                Log.d("xxlab", "oh no, catch (IOException e)");
            } catch (JSONException e) {
                e.printStackTrace();
                Log.d("xxlab", "oh no, catch (JSONException e)");
            } finally {
                Log.d("xxlab", "FINALLY");
                if(client != null) // Make sure the connection is not null.
                    client.disconnect();
            }
        } else {
            Log.d("xxlab", "IP validation failed: " + serverAddress);
        }

        return "Timeout";
    }

    protected void onPostExecute(String result) {
        Log.d("xxlab", "PostImageForRecognitionAsync onPostExecute [" + result + "]");
        Toast.makeText(mContext, result, Toast.LENGTH_LONG).show();
        //FIXME: very bad string magic. refactr it.
        if (result.equals("检测通过")) {
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

