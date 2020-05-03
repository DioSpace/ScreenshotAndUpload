package com.my.screenshotandupload;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    //获取权限
    public void get_permission(View view) {
        MyUtil.checkAndGet_permission(this);
    }

    //截屏并上传
    public void screenshot(View view) {
        MyUtil.screenShot(this);
    }

    //不断截屏并上传
    public void uploadImage_loop(View view) {
        MyUtil.screenShotInLoop(this);
    }
}
