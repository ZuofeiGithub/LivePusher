package com.zuofei.livepusher;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.zuofei.livepusher.camera.CameraView;

public class CameraActivity extends AppCompatActivity {
    private CameraView cameraView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_camera);
        cameraView = findViewById(R.id.cameraview);

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        cameraView.previewAngle(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraView.onDestory();
    }
}
