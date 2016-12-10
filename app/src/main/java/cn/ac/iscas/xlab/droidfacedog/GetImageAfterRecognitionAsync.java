package cn.ac.iscas.xlab.droidfacedog;

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

public class GetImageAfterRecognitionAsync extends AsyncTask<String, Void, String> {
    private String serverAddress;
    // http://stackoverflow.com/questions/3698034/validating-ip-in-android
    private static Pattern IP_ADDRESS = Pattern.compile(
            "((25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\\.(25[0-5]|2[0-4]"
                    + "[0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1]"
                    + "[0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}"
                    + "|[1-9][0-9]|[0-9]))");
    private Context mContext;

    protected String doInBackground(String... idArray) {
        if (serverAddress == null) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
            serverAddress = prefs.getString("server_ip_address", "192.168.1.60");
        }
        if (serverAddress.equals("")) {
            return "";
        }
        // http://stackoverflow.com/questions/3698034/validating-ip-in-android
        Matcher matcher = IP_ADDRESS.matcher(serverAddress);
        if (matcher.matches()) {
            Log.d("xxlab", "GetImageAfterRecognitionAsync IP is validated: " + serverAddress);
            // ip is correct

            // http://www.wikihow.com/Execute-HTTP-POST-Requests-in-Android
            // http://stackoverflow.com/questions/6218143/how-to-send-post-request-in-json-using-httpclient
            // http://stackoverflow.com/questions/13911993/sending-a-json-http-post-request-from-android

            HttpURLConnection client = null;
            try {
                DataOutputStream outputStream;
//                DataInputStream inputStream;
                URL url = new URL("http://" + serverAddress + ":8000/face" + "?userid=yafen");
                client = (HttpURLConnection) url.openConnection();
                client.setRequestMethod("GET");
//                client.setRequestProperty("key","value");
//                client.setDoOutput(true);
                client.setDoInput(true);
                client.setUseCaches(false);
                //client.setRequestProperty("Content-Type","application/json");
                client.setChunkedStreamingMode(0);
                client.connect();
//                outputStream = new DataOutputStream(client.getOutputStream());
                Log.d("xxlab", "GetImageAfterRecognitionAsync HTTP ERROR CODE: " + client.getResponseCode());
                Log.d("xxlab", "GetImageAfterRecognitionAsync HTTP ERROR CODE: " + client.getResponseMessage());
                if (client.getResponseCode() >= 400) {
                    return "";
                }
//                inputStream = new DataInputStream(client.getInputStream());
                BufferedInputStream in = new BufferedInputStream(client.getInputStream());
                byte[] buf = new byte[1024];
                int l = in.read(buf);
                Log.d("xxlab", "GetImageAfterRecognitionAsync RESPONCE: " + new String(buf) + " with len " + l);
//                JsonReader jsonReader = new JsonReader(new InputStreamReader(client.getInputStream(), "UTF-8"));
//                Log.d("xxlab", "GetImageAfterRecognitionAsync RESPONSE: " + jsonReader.toString());

                // 3. build jsonObject
//                JSONObject jsonObject = new JSONObject();
                // jsonObject.accumulate("image", "base64 of image");
                //jsonObject.accumulate("image", encodeToBase64(idArray[0], Bitmap.CompressFormat.JPEG, 100));
//                jsonObject.put("userid", "fangyafen");

                // 4. convert JSONObject to JSON to String
//                String json = jsonObject.toString();
//                Log.d("xxlab", "GetImageAfterRecognitionAsync" + json);

//                outputStream.writeBytes(json);
//                outputStream.flush();


//                Log.d("xxlab", "GetImageAfterRecognitionAsync RESPONSE: " + jsonReader.toString());

//                outputStream.close();
//                jsonReader.close();
                //inputStream.close();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                Log.d("xxlab", "GetImageAfterRecognitionAsync FINALLY");
                if(client != null) // Make sure the connection is not null.
                    client.disconnect();
            }
        } else {
            Log.d("xxlab", "GetImageAfterRecognitionAsync IP validation failed: " + serverAddress);
        }

        return "";
    }

    protected void onPostExecute(String result) {
        Log.d("xxlab", "GetImageAfterRecognitionAsync onPostExecute [" + result + "]");
        Toast.makeText(mContext, result, Toast.LENGTH_SHORT).show();
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

