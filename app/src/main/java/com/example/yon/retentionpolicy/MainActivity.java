package com.example.yon.retentionpolicy;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.EditText;

import com.dd.CircularProgressButton;
import com.example.retentionpolicy.source.SignInProgress;
import com.example.retentionpolicy.source.SignInState;
import com.example.retentionpolicy.runtime.FindViewById;
import com.example.retentionpolicy.runtime.SetOnClickById;
import com.example.retentionpolicy.runtime.ViewInjector;

import java.util.Random;


/**
 * 使用运行时注解ViewInjector（runtime module）进行View 成员变量绑定及设置点击事件
 */
public class MainActivity extends AppCompatActivity {

    @FindViewById(R.id.btn_sign_in)
    private CircularProgressButton mSubmitBtn;
    @FindViewById(R.id.et_user_name)
    private EditText mUserNameText;
    @FindViewById(R.id.et_password)
    private EditText mPasswordText;

    private int mSignInProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ViewInjector.inject(this);
        mSubmitBtn.setIndeterminateProgressMode(true);
    }

    @SetOnClickById(R.id.btn_sign_in)
    void onSubmitClicked() {
        //模拟登录请求过程，这里定时更新进度
        mSignInProgress = 0;
        updateSignInState(SignInProgress.START);
        final Random random = new Random();
        mSubmitBtn.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mSignInProgress < 100) {
                    mSignInProgress += 10;
                    mSubmitBtn.postDelayed(this, random.nextInt(300));
                } else {
                    updateSignInState(SignInProgress.SUCCESS);
                }
            }
        }, random.nextInt(300));
    }

    /**
     * 使用源码级注解（source）代替enum枚举类使用，限定登录的几种视图状态
     */
    void updateSignInState(@SignInState int state) {
        switch (state) {
            case SignInProgress.START:
                //因为submitBtn.setIndeterminateProgressMode(true);，因此这里随便给个进度值（0~100）即可让进度条转动
                mSubmitBtn.setProgress(1);
                break;
            case SignInProgress.SUCCESS:
                mSubmitBtn.setProgress(100);
                mSubmitBtn.setEnabled(true);
                ReceiverActivity.start(this, mUserNameText.getText().toString(), mPasswordText.getText().toString());
                finish();
                break;
            case SignInProgress.FAILURE:
                break;
            default:
                break;
        }
    }
}
