package cn.ac.iscas.xlab.droidfacedog.custom_preference;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import cn.ac.iscas.xlab.droidfacedog.R;
import cn.ac.iscas.xlab.droidfacedog.config.Config;
import cn.ac.iscas.xlab.droidfacedog.util.RegexCheckUtil;


/**
 * Created by lisongting on 2017/7/19.
 */

public class RosIpPreference extends DialogPreference {
    
    public static final String TAG = "RosIpPreference";
    //初始值：添加10.0.0.0和192.168.1.0
    public static final String INIT_HISTORY = "10.0.0.0_192.168.1.0";
    public static final String KEY_ROS_HISTORY = "ros_history";
    public String KEY_ROS_SERVER_SP ;
    
    private TextView textViewIpType;
    private TextView textViewIp;
    private EditText dialogEditText;
    private ListView listView;
    private ArrayList<Map<String,String>> historyList;
    private SimpleAdapter simpleAdapter;
    private String input_history;
    private String[] historyArr;
    private ImageView ok;
    private ImageView cancel;

    private SharedPreferences.Editor spEdtor;

    public RosIpPreference(Context context, AttributeSet attrs) {
        super(context, attrs,0);
        KEY_ROS_SERVER_SP = context.getResources().getString(R.string.key_ros_server_ip);
        spEdtor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        historyArr = new String[5];
        Log.i(TAG, "RosIpPreference(Context context, AttributeSet attrs)");
    }

    public RosIpPreference(Context context) {
        super(context,null);
        Log.i(TAG, "RosIpPreference(Context context)");


    }

    @Override
    public View onCreateView(ViewGroup parent) {
        super.onCreateView(parent);
        Log.i(TAG, TAG + " -- onCreateView");
        View v = LayoutInflater.from(getContext()).inflate(R.layout.ip_preference_layout,parent,false);
        textViewIpType = (TextView) v.findViewById(R.id.id_ip_type);
        textViewIp = (TextView) v.findViewById(R.id.id_ip);
        historyList = new ArrayList<>();
        return v;
    }

    @Override
    protected void onBindView(View view) {
        Log.i(TAG, TAG + " -- onBindView");
        textViewIpType.setText("ROS服务器IP地址:");
        String ipInSp = PreferenceManager.getDefaultSharedPreferences(getContext())
                .getString(KEY_ROS_SERVER_SP, Config.ROS_SERVER_IP);
        textViewIp.setText(ipInSp);

        input_history = PreferenceManager.getDefaultSharedPreferences(getContext()).
                getString(KEY_ROS_HISTORY,INIT_HISTORY);
        historyArr = input_history.split("_");
        historyArr = Arrays.copyOf(historyArr, 5);
        super.onBindView(view);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        Log.i(TAG, TAG + " -- onBindDialogView");

        dialogEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showHistoryList();
            }
        });
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                dialogEditText.setText(historyArr[position]);
                listView.setVisibility(View.GONE);
            }
        });
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String strIp = dialogEditText.getText().toString();
                if (RegexCheckUtil.isRightIP(strIp)) {
                    textViewIp.setText(strIp);
                    writeToSharedPreference(strIp);
                    getDialog().dismiss();
                } else {
                    Toast.makeText(getContext(), "请输入正确的IP地址", Toast.LENGTH_SHORT).show();
                }

            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getDialog().dismiss();
            }
        });

    }


    @Override
    protected View onCreateDialogView() {
        Log.i(TAG, TAG + " -- onCreateDialogView");
        View v = LayoutInflater.from(getContext()).inflate(R.layout.pref_dialog_view,null);

        dialogEditText = (EditText) v.findViewById(R.id.id_edittext_ip);
        listView = (ListView) v.findViewById(R.id.history_listview);
        ok = (ImageView) v.findViewById(R.id.id_iv_ok);
        cancel = (ImageView) v.findViewById(R.id.id_iv_cacel);
        dialogEditText.setText(textViewIp.getText());
        return v;

    }

    public void showHistoryList() {

        for (String str : historyArr) {
            if (str != null) {
                Map<String, String> map = new HashMap<>();
                map.put("textViewIp", str);
                historyList.add(map);
            }

        }
        simpleAdapter = new SimpleAdapter(getContext(), historyList,
                android.R.layout.simple_dropdown_item_1line,
                new String[]{"textViewIp"}, new int[]{android.R.id.text1});

        listView.setAdapter(simpleAdapter);
    }

    //将每次输入的记录保存下来，存放在SharedPreference中
    public void writeToSharedPreference(String str) {
        //如果历史记录中已经有了相同的ip，则不添加，否则才添加到历史记录
        if (!input_history.contains(str)){
            for(int i=historyArr.length-1;i>=3;i--) {
                historyArr[i] = historyArr[i-1];
            }
            historyArr[2] = str;
            StringBuilder sb = new StringBuilder(INIT_HISTORY);
            int num = INIT_HISTORY.split("_").length;
            for(int i=num;i<historyArr.length;i++) {
                if (historyArr[i] != null) {
                    sb.append("_").append(historyArr[i]);
                }
            }
            spEdtor.putString(KEY_ROS_HISTORY, sb.toString());
//            Log.i(TAG, "写入SP的输入记录:"+sb.toString());
        }
        spEdtor.putString(KEY_ROS_SERVER_SP, str);
        spEdtor.apply();
        Config.ROS_SERVER_IP = str;

    }
}
