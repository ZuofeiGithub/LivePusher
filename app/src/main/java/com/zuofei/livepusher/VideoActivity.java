package com.zuofei.livepusher;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.ywl5320.libmusic.WlMusic;
import com.ywl5320.listener.OnCompleteListener;
import com.ywl5320.listener.OnPreparedListener;
import com.ywl5320.listener.OnShowPcmDataListener;
import com.zuofei.livepusher.camera.CameraView;
import com.zuofei.livepusher.encodec.BaseMediaEncoder;
import com.zuofei.livepusher.encodec.MediaEncoder;

public class VideoActivity extends AppCompatActivity {

    private CameraView cameraView;
    private Button recordBtn;

    private MediaEncoder mediaEncoder;

    private WlMusic wlMusic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);
        cameraView = findViewById(R.id.cameraview);
        recordBtn = findViewById(R.id.record);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {//版本判断
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA,Manifest.permission.INTERNET}, 1);
            }
        }
        wlMusic = WlMusic.getInstance();
        wlMusic.setCallBackPcmData(true);
        wlMusic.setOnPreparedListener(new OnPreparedListener() {
            @Override
            public void onPrepared() {
                wlMusic.playCutAudio(20,30);//录制多少秒之间
            }
        });
        wlMusic.setOnCompleteListener(new OnCompleteListener() {
            @Override
            public void onComplete() {
                if(mediaEncoder != null){
                    mediaEncoder.stopRecord();
                    mediaEncoder = null;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            recordBtn.setText("开始录制");
                        }
                    });
                }
            }
        });

        wlMusic.setOnShowPcmDataListener(new OnShowPcmDataListener() {
            @Override
            public void onPcmInfo(int samplerate, int bit, int channels) {
                mediaEncoder = new MediaEncoder(VideoActivity.this,cameraView.getTextureId());
                mediaEncoder.initEncodec(cameraView.getEglContext(),
                        Environment.getExternalStorageDirectory().getAbsolutePath()+"/wl_live_pusher.mp4",720,1280,samplerate,channels);
                mediaEncoder.setOnMediaInfoListener(new BaseMediaEncoder.OnMediaInfoListener() {
                    @Override
                    public void onMediaTime(long times) {
                        Log.d("zuofei","time is:"+times);
                    }
                });
                mediaEncoder.startRecord();
            }

            @Override
            public void onPcmData(byte[] pcmdata, int size, long clock) {
                if(mediaEncoder != null){
                    mediaEncoder.putPCMData(pcmdata,size);
                }
            }
        });
    }

    public void record(View view) {
        if(mediaEncoder == null){
            wlMusic.setSource(Environment.getExternalStorageDirectory().getAbsolutePath()+"/shenjingbing.mp3");
//            wlMusic.setSource("http://music.163.com/song/media/outer/url?id=452986458.mp3");
            wlMusic.prePared();
            recordBtn.setText("正在录制");
        }else{
            mediaEncoder.stopRecord();
            recordBtn.setText("开始录制");
            mediaEncoder = null;
            wlMusic.stop();
        }
    }
}
