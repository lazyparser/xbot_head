package cn.ac.iscas.xlab.droidfacedog.network;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

/**
 * Created by lisongting on 2017/5/22.
 */

public class VolleySingleton {

    private static VolleySingleton volleySingleton;
    private RequestQueue requestQueue;
    private VolleySingleton(Context context){
        requestQueue = Volley.newRequestQueue(context);
    }
    public static synchronized VolleySingleton getVolleySingleton(Context context){
        if (volleySingleton == null) {
            volleySingleton = new VolleySingleton(context);
        }
        return volleySingleton;
    }
    public RequestQueue getRequestQueue(){
        return requestQueue;
    }
    public <T> void addToRequestQueue(Request<T> req){
        getRequestQueue().add(req);
    }
}
