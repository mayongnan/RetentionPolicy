package com.example.retentionpolicy.source;

/**
 * 登录进展
 * Created by YON on 2017/1/17.
 */

public interface SignInProgress {

    /**
     * 登录进行中
     */
    int START = 0;

    /**
     * 登录成功
     */
    int SUCCESS = 1;

    /**
     * 登录失败
     */
    int FAILURE = -1;

}
