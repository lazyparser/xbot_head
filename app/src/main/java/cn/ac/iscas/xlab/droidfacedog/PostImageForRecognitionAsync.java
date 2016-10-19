package cn.ac.iscas.xlab.droidfacedog;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.JsonReader;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

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

    protected String doInBackground(Bitmap... faceImage) {
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
            Log.d("xxlab", "IP is validated: " + serverAddress);
            // ip is correct

            // http://www.wikihow.com/Execute-HTTP-POST-Requests-in-Android
            // http://stackoverflow.com/questions/6218143/how-to-send-post-request-in-json-using-httpclient
            // http://stackoverflow.com/questions/13911993/sending-a-json-http-post-request-from-android

            HttpURLConnection client = null;
            try {
                DataOutputStream outputStream;
                DataInputStream inputStream;
                URL url = new URL("http://" + serverAddress + ":8000/recognition");
                client = (HttpURLConnection) url.openConnection();
                client.setRequestMethod("POST");
//                client.setRequestProperty("key","value");
                client.setDoOutput(true);
                client.setDoInput(true);
                client.setUseCaches(false);
                client.setRequestProperty("Content-Type","application/json");
                client.setChunkedStreamingMode(0);

                // 3. build jsonObject
                JSONObject jsonObject = new JSONObject();
                jsonObject.accumulate("image", "TODO base64 of image");

                // 4. convert JSONObject to JSON to String
                String json = jsonObject.toString();
                Log.d("xxlab", json);

                outputStream = new DataOutputStream(client.getOutputStream());
                outputStream.writeBytes(URLEncoder.encode(json, "UTF-8"));
                outputStream.flush();
                outputStream.close();

//                client.connect();

                inputStream = new DataInputStream(client.getInputStream());
                JsonReader jsonReader = new JsonReader(new InputStreamReader(inputStream, "UTF-8"));
                Log.d("xxlab", "RESPONSE: " + jsonReader.toString());

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            } finally {
                if(client != null) // Make sure the connection is not null.
                    client.disconnect();
            }
        } else {
            Log.d("xxlab", "IP validation failed: " + serverAddress);
        }

        return "";
    }

    protected void onPostExecute(String result) {
        Log.d("xxlab", "PostImageForRecognitionAsync onPostExecute [" + result + "]");
        Toast.makeText(mContext, result, Toast.LENGTH_SHORT).show();
    }

    // http://stackoverflow.com/questions/16920942/getting-context-in-asynctask
    public void setCentext(Context context) {
        mContext = context;
    }
}

