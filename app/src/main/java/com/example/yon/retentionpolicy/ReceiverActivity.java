package com.example.yon.retentionpolicy;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.example.retentionpolicy.clazz.BindInjector;
import com.example.runtime_annotation.BindClick;
import com.example.runtime_annotation.BindView;



/**
 * 使用编译时注解BindInjector（依赖clazz module库）进行View 成员变量绑定及设置点击事件
 */
public class ReceiverActivity extends AppCompatActivity {

    private static final String USER_NAME = "user_name";
    private static final String PASSWORD = "password";

    public static void start(Context context, String userName, String password) {
        Intent intent = new Intent();
        intent.setClass(context, ReceiverActivity.class);
        intent.putExtra(USER_NAME, userName);
        intent.putExtra(PASSWORD, password);
        context.startActivity(intent);
    }

    @BindView(R.id.tv_user_name)
    TextView mUserNameTextView;
    @BindView(R.id.tv_password)
    TextView mPasswordTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receiver);
        //使用自定义注入库
        BindInjector.inject(this);
        Intent intent = getIntent();
        if (intent.getExtras() != null) {
            mUserNameTextView.setText(getString(R.string.user_name_text, intent.getStringExtra(USER_NAME)));
            mPasswordTextView.setText(getString(R.string.password_text, intent.getStringExtra(PASSWORD)));
        }

    }

    @BindClick(R.id.btn_finish)
    public void onFinishClicked() {
        this.finish();
    }

}
