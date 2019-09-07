package com.zuofei.livepusher.camera;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.AttributeSet;

import com.zuofei.livepusher.egl.WLEGLSurfaceView;

public class CameraView extends WLEGLSurfaceView {
    private CameraRender cameraRender;
    private CameraOpt cameraOpt;
    private int cameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
    public CameraView(Context context) {
        super(context,null);
    }

    public CameraView(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public CameraView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        cameraRender = new CameraRender(context);
        cameraOpt = new CameraOpt();
        setRender(cameraRender);
        cameraRender.setOnSurfaceCreateListener(new CameraRender.OnSurfaceCreateListener() {
            @Override
            public void onSurfaceCreate(SurfaceTexture surfaceTexture) {
                cameraOpt.initCamera(surfaceTexture,cameraId);
            }
        });
    }

    public void onDestory() {
        if(cameraOpt != null){
            cameraOpt.stopPreview();
        }
    }
}
