package com.zuofei.livepusher.camera;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.WindowManager;

import com.zuofei.livepusher.egl.WLEGLSurfaceView;

public class CameraView extends WLEGLSurfaceView {
    private CameraRender cameraRender;
    private CameraOpt cameraOpt;
    private int cameraId = Camera.CameraInfo.CAMERA_FACING_BACK;

    private int textureId = -1;

    public CameraView(Context context) {
        super(context, null);
    }

    public CameraView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        cameraRender = new CameraRender(context);
        cameraOpt = new CameraOpt(context);
        setRender(cameraRender);
        previewAngle(context); //预览角度
        cameraRender.setOnSurfaceCreateListener(new CameraRender.OnSurfaceCreateListener() {
            @Override
            public void onSurfaceCreate(SurfaceTexture surfaceTexture,int tid) {
                cameraOpt.initCamera(surfaceTexture, cameraId);
                textureId = tid;
            }
        });
    }

    public void previewAngle(Context context) {
        int angle = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
       cameraRender.resetMatrix();
        switch (angle) {
            case Surface.ROTATION_0:
                if(cameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    cameraRender.setAngle(90, 0, 0, 1);
                    cameraRender.setAngle(180, 1, 0, 0);
                }else{
                    cameraRender.setAngle(90, 0, 0, 1);
                }
                break;
            case Surface.ROTATION_90:
                if(cameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    cameraRender.setAngle(180, 0, 0, 1);
                    cameraRender.setAngle(180, 0, 1, 0);
                }else{
                    cameraRender.setAngle(90, 0, 0, 1);
                }
                break;
            case Surface.ROTATION_180:
                if(cameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    cameraRender.setAngle(90, 0, 0, 1);
                    cameraRender.setAngle(180, 0, 1, 0);
                }else{
                    cameraRender.setAngle(-90, 0, 0, 1);
                }
                break;
            case Surface.ROTATION_270:
                if(cameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    cameraRender.setAngle(180, 0, 1, 0);
                }else{
                    cameraRender.setAngle(0, 0, 0, 1);
                }
                break;
        }
    }

    public void onDestory() {
        if (cameraOpt != null) {
            cameraOpt.stopPreview();
        }
    }

    public int getTextureId(){
      return  textureId;
    }
}
