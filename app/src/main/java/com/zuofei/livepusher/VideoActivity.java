package com.zuofei.livepusher;

import android.media.MediaFormat;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.zuofei.livepusher.camera.CameraView;
import com.zuofei.livepusher.encodec.BaseMediaEncoder;
import com.zuofei.livepusher.encodec.MediaEncoder;

public class VideoActivity extends AppCompatActivity {

    private CameraView cameraView;
    private Button recordBtn;

    private MediaEncoder mediaEncoder;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);
        cameraView = findViewById(R.id.cameraview);
        recordBtn = findViewById(R.id.record);
    }

    public void record(View view) {
        if(mediaEncoder == null){
            mediaEncoder = new MediaEncoder(this,cameraView.getTextureId());
            mediaEncoder.initEncodec(cameraView.getEglContext(),
                    Environment.getExternalStorageDirectory().getAbsolutePath()+"/wl_live_pusher.mp4", MediaFormat.MIMETYPE_VIDEO_AVC,720,1280);
           mediaEncoder.setOnMediaInfoListener(new BaseMediaEncoder.OnMediaInfoListener() {
               @Override
               public void onMediaTime(long times) {
                   Log.d("zuofei","time is:"+times);
               }
           });
            mediaEncoder.startRecord();
            recordBtn.setText("正在录制");
        }else{
            mediaEncoder.stopRecord();
            recordBtn.setText("开始录制");
            mediaEncoder = null;
        }
    }
}
