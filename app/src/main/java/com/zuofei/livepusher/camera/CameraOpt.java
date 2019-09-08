package com.zuofei.livepusher.camera;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;

import com.zuofei.livepusher.utils.DisplayUtil;

import java.io.IOException;
import java.util.List;

public class CameraOpt {
    private SurfaceTexture surfaceTexture;
    private Camera camera;
    private int screenWidth;
    private int screenHeight;
    public CameraOpt(Context context) {
        this.screenWidth = DisplayUtil.getScreenWidth(context);
        this.screenHeight = DisplayUtil.getScreenHeight(context);
    }
    public void initCamera(SurfaceTexture surfaceTexture,int cameraId)
    {
        this.surfaceTexture = surfaceTexture;
        setCameraParams(cameraId);
    }

    private void setCameraParams(int cameraId){
        try {
            camera = Camera.open(cameraId);
            camera.setPreviewTexture(surfaceTexture);
            Camera.Parameters parameters = camera.getParameters();
            parameters.setFlashMode("off");

            Camera.Size size = getFitSize(parameters.getSupportedPictureSizes());
            parameters.setPreviewFormat(ImageFormat.NV21);
            parameters.setPictureSize(size.width,size.height);
            size = getFitSize(parameters.getSupportedPreviewSizes());
            parameters.setPreviewSize(size.width, size.height);
            camera.setParameters(parameters);
            camera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopPreview(){
        if(camera != null){
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }
    public void changeCamera(int cameraId){
        if(camera != null){
            stopPreview();
        }
        setCameraParams(cameraId);
    }

    private Camera.Size getFitSize(List<Camera.Size> sizes){
        if(screenWidth  < screenHeight){
            int t = screenHeight;
            screenHeight = screenWidth;
            screenWidth = t;
        }
        for(Camera.Size size:sizes){
            if(1.0f*size.width/size.height == 1.0f*screenWidth / screenHeight ){
                return size;
            }
        }
        return sizes.get(0);
    }
}
