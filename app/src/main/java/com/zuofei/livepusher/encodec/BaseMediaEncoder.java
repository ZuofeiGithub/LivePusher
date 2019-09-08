package com.zuofei.livepusher.encodec;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.view.Surface;

import com.zuofei.livepusher.egl.EglHelper;
import com.zuofei.livepusher.egl.WLEGLSurfaceView;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

import javax.microedition.khronos.egl.EGLContext;

public abstract class BaseMediaEncoder {
    private Surface surface;
    private EGLContext eglContext;

    private int width;
    private int height;

    private EGLMediaThread eglMediaThread;
    private VideoEncodecThread videoEncodecThread;

    private MediaCodec videoEncoder;
    private MediaFormat videoFormat;
    private MediaCodec.BufferInfo videoBufferinfo;
    private WLEGLSurfaceView.WlGLRender wlGLRender;

    private MediaMuxer mediaMuxer;

    public final static int RENDERMODE_WHEN_DIRTY = 0;
    public final static int RENDERMODE_CONTINUOUSLY = 1;
    private int mRenderMode = RENDERMODE_CONTINUOUSLY;

    private OnMediaInfoListener onMediaInfoListener;

    public BaseMediaEncoder(Context context) {

    }

    public void setRender(WLEGLSurfaceView.WlGLRender wlGLRender) {
        this.wlGLRender = wlGLRender;
    }

    public void setRenderMode(int mRenderMode) {
        if(wlGLRender == null){
            throw new RuntimeException("must set render before");
        }
        this.mRenderMode = mRenderMode;
    }

    public void startRecord(){
        if(surface != null && eglContext != null){
            eglMediaThread = new EGLMediaThread(new WeakReference<BaseMediaEncoder>(this));
            videoEncodecThread = new VideoEncodecThread(new WeakReference<BaseMediaEncoder>(this));
            eglMediaThread.isCreate = true;
            eglMediaThread.isChange = true;
            eglMediaThread.start();
            videoEncodecThread.start();
        }
    }

    public void stopRecord(){
        if(eglMediaThread != null && videoEncodecThread != null){
            videoEncodecThread.exit();
            eglMediaThread.onDestory();
            videoEncodecThread = null;
            eglMediaThread = null;
        }
    }

    public void initEncodec(EGLContext eglContext,String path,String mimeType,int width,int height){
        this.width = width;
        this.height = height;
        this.eglContext = eglContext;
        initMediaEncodec(path,mimeType,width,height);
    }

    private void initMediaEncodec(String path,String mimeType,int width,int height){
        try {
            mediaMuxer = new MediaMuxer(path,MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            initVideoEncoder(mimeType,width,height);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void setOnMediaInfoListener(OnMediaInfoListener onMediaInfoListener) {
        this.onMediaInfoListener = onMediaInfoListener;
    }

    private void initVideoEncoder(String mimeType, int width, int height){
        try {
            videoBufferinfo = new MediaCodec.BufferInfo();
            videoFormat = MediaFormat.createVideoFormat(mimeType,width,height);
            videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            videoFormat.setInteger(MediaFormat.KEY_BIT_RATE,width*height*4);
            videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE,30);
            videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL,1);
            videoEncoder = MediaCodec.createEncoderByType(mimeType);
            videoEncoder.configure(videoFormat,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);
            surface = videoEncoder.createInputSurface();
        } catch (IOException e) {
            e.printStackTrace();
            videoEncoder = null;
            videoFormat = null;
            videoBufferinfo = null;
        }
    }

    static class EGLMediaThread extends Thread{
        private WeakReference<BaseMediaEncoder> encoder;
        private EglHelper eglHelper;
        private Object object;

        private boolean isExit = false;
        private boolean isCreate = false;
        private boolean isChange = false;
        private boolean isStart = false;


        public EGLMediaThread(WeakReference<BaseMediaEncoder> encoder) {
            this.encoder = encoder;
        }

        @Override
        public void run() {
            super.run();
            isExit = false;
            isStart = false;
            object = new Object();
            eglHelper = new EglHelper();
            eglHelper.initEgl(encoder.get().surface,encoder.get().eglContext);
            while (true){
                if(isExit){
                    release();
                    break;
                }
                if(isStart)
                {
                    if(encoder.get().mRenderMode == RENDERMODE_WHEN_DIRTY)
                    {
                        synchronized (object)
                        {
                            try {
                                object.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    else if(encoder.get().mRenderMode == RENDERMODE_CONTINUOUSLY)
                    {
                        try {
                            Thread.sleep(1000 / 60);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    else
                    {
                        throw  new RuntimeException("mRenderMode is wrong value");
                    }
                }

                onCreate();
                onChange(encoder.get().width, encoder.get().height);
                onDraw();

                isStart = true;
            }
        }

        private void onCreate()
        {
            if(isCreate && encoder.get().wlGLRender != null)
            {
                isCreate = false;
                encoder.get().wlGLRender.onSurfaceCreated();
            }
        }

        private void onChange(int width, int height)
        {
            if(isChange && encoder.get().wlGLRender != null)
            {
                isChange = false;
                encoder.get().wlGLRender.onSurfaceChanged(width, height);
            }
        }

        private void onDraw()
        {
            if(encoder.get().wlGLRender != null && eglHelper != null)
            {
                encoder.get().wlGLRender.onDrawFrame();
                if(!isStart)
                {
                    encoder.get().wlGLRender.onDrawFrame();
                }
                eglHelper.swapBuffers();

            }
        }

        private void requestRender()
        {
            if(object != null)
            {
                synchronized (object)
                {
                    object.notifyAll();
                }
            }
        }

        public void onDestory()
        {
            isExit = true;
            requestRender();
        }


        public void release(){
            if(eglHelper != null){
                eglHelper.destoryEgl();
                eglHelper = null;
                object = null;
                encoder = null;
            }
        }
    }

    static class VideoEncodecThread extends Thread{
        private WeakReference<BaseMediaEncoder> encoder;
        private boolean isExit;
        private MediaCodec videoEncoder;
        private MediaFormat videoFormat;
        private MediaMuxer mediaMuxer;
        private int videoTrackIndex;
        private long pts;
        private MediaCodec.BufferInfo videoBufferinfo;
        public VideoEncodecThread(WeakReference<BaseMediaEncoder> encoder) {
            this.encoder = encoder;
            videoEncoder = encoder.get().videoEncoder;
            videoFormat = encoder.get().videoFormat;
            videoBufferinfo = encoder.get().videoBufferinfo;
            mediaMuxer = encoder.get().mediaMuxer;
        }

        @Override
        public void run() {
            super.run();
            isExit = false;
            pts = 0;
            videoTrackIndex = -1;
            videoEncoder.start();
            while (true){
                if(isExit){

                    videoEncoder.stop();
                    videoEncoder.release();
                    videoEncoder = null;

                    mediaMuxer.stop();
                    mediaMuxer.release();
                    mediaMuxer = null;
                    break;
                }
                int outputBufferIndex = videoEncoder.dequeueOutputBuffer(videoBufferinfo,0);
                if(outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED)
                {
                    videoTrackIndex = mediaMuxer.addTrack(videoEncoder.getOutputFormat());
                    mediaMuxer.start();
                }else{
                    while (outputBufferIndex > 0){
                        ByteBuffer outputBuffer  = videoEncoder.getOutputBuffers()[outputBufferIndex];
                        outputBuffer.position(videoBufferinfo.offset);
                        outputBuffer.limit(videoBufferinfo.offset+videoBufferinfo.size);
                        if(pts == 0){
                            pts = videoBufferinfo.presentationTimeUs;
                        }
                        videoBufferinfo.presentationTimeUs = videoBufferinfo.presentationTimeUs - pts;
                        mediaMuxer.writeSampleData(videoTrackIndex,outputBuffer,videoBufferinfo);
                        if(encoder.get().onMediaInfoListener != null){
                            encoder.get().onMediaInfoListener.onMediaTime(videoBufferinfo.presentationTimeUs/1000000);
                        }

                        videoEncoder.releaseOutputBuffer(outputBufferIndex,false);
                        outputBufferIndex = videoEncoder.dequeueOutputBuffer(videoBufferinfo,0);
                    }
                }
            }
        }

        public void exit(){
            isExit = true;
        }
    }

    public  interface OnMediaInfoListener
    {
        void onMediaTime(long times);
    }
}
