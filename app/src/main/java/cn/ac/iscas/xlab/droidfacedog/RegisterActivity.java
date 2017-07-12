package cn.ac.iscas.xlab.droidfacedog;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import cn.ac.iscas.xlab.droidfacedog.mvp.userlist.UserListActivity;
import cn.ac.iscas.xlab.droidfacedog.util.RegexCheckUtil;

/**
 * Created by lisongting on 2017/5/23.
 */

public class RegisterActivity extends AppCompatActivity {
    private TextInputLayout textInputLayout;
    private EditText editText ;
    private Button btStartCamera;
    private Button btUserList;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_user);

        textInputLayout = (TextInputLayout) findViewById(R.id.id_text_input_layout);
        editText = (EditText) findViewById(R.id.id_textView);
        btStartCamera = (Button) findViewById(R.id.id_bt_camera);
        btUserList = (Button) findViewById(R.id.id_bt_user_list);

        btStartCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String userName = editText.getText().toString();
                if (RegexCheckUtil.isRightPersonName(userName)) {
                    Intent intent = new Intent(RegisterActivity.this, CameraActivity.class);
                    intent.putExtra("userName",userName);
                    startActivity(intent);
                } else {
                    Toast.makeText(RegisterActivity.this, "请输入正确的姓名", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btUserList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(RegisterActivity.this, UserListActivity.class));
            }
        });
    }
}
