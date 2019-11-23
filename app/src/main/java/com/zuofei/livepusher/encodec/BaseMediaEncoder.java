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
    private int sampleRate;
    private int channelCount;


    private EGLMediaThread eglMediaThread;
    private VideoEncodecThread videoEncodecThread;
    private AudioEncodecThread audioEncodecThread;

    private MediaCodec videoEncoder;
    private MediaFormat videoFormat;
    private MediaCodec.BufferInfo videoBufferinfo;
    private WLEGLSurfaceView.WlGLRender wlGLRender;

    private MediaCodec audioEncodec;
    private MediaFormat audioFormat;
    private MediaCodec.BufferInfo audioBufferinfo;
    private long audioPts = 0;

    private MediaMuxer mediaMuxer;
    private boolean encodecStart;
    private boolean audioExit;
    private boolean videoExit;

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
            audioPts = 0;
            audioExit = false;
            videoExit = false;
            encodecStart = false;
            eglMediaThread = new EGLMediaThread(new WeakReference<BaseMediaEncoder>(this));
            videoEncodecThread = new VideoEncodecThread(new WeakReference<BaseMediaEncoder>(this));
            audioEncodecThread = new AudioEncodecThread(new WeakReference<BaseMediaEncoder>(this));
            eglMediaThread.isCreate = true;
            eglMediaThread.isChange = true;
            eglMediaThread.start();
            videoEncodecThread.start();
            audioEncodecThread.start();
        }
    }

    public void stopRecord(){
        if(eglMediaThread != null && videoEncodecThread != null&&audioEncodecThread != null){
            videoEncodecThread.exit();
            audioEncodecThread.exit();
            eglMediaThread.onDestory();
            videoEncodecThread = null;
            audioEncodecThread = null;
            eglMediaThread = null;
        }
    }

    public void initEncodec(EGLContext eglContext,String path,int width,int height,int sampleRate,int channelCount){
        this.width = width;
        this.height = height;
        this.sampleRate = sampleRate;
        this.channelCount = channelCount;
        this.eglContext = eglContext;
        initMediaEncodec(path,width,height,sampleRate,channelCount);
    }

    private void initMediaEncodec(String path,int width,int height,int sampleRate,int channelCount){
        try {
            mediaMuxer = new MediaMuxer(path,MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            initVideoEncoder(MediaFormat.MIMETYPE_VIDEO_AVC,width,height);
            initAudioEncoder(MediaFormat.MIMETYPE_AUDIO_AAC,sampleRate,channelCount);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void putPCMData(byte[] buffer,int size){
        if(audioEncodecThread != null && !audioEncodecThread.isExit&&buffer != null &&size > 0){
            int inputBufferindex = audioEncodec.dequeueInputBuffer(0);
            if(inputBufferindex >=0){
                ByteBuffer byteBuffer = audioEncodec.getInputBuffers()[inputBufferindex];
                byteBuffer.clear();
                byteBuffer.put(buffer);
                long pts  = getAudioPts(size,sampleRate);
                audioEncodec.queueInputBuffer(inputBufferindex,0,size,pts,0);
            }
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

    private void initAudioEncoder(String mimeType,int sampleRate,int channelCount){
        try {
            this.sampleRate = sampleRate;
            audioBufferinfo = new MediaCodec.BufferInfo();
            audioFormat = MediaFormat.createAudioFormat(mimeType,sampleRate,channelCount);
            audioFormat.setInteger(MediaFormat.KEY_BIT_RATE,96000);
            audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE,MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            audioFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE,4096);
            audioEncodec = MediaCodec.createDecoderByType(mimeType);
            audioEncodec.configure(audioFormat,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);
        } catch (IOException e) {
            e.printStackTrace();
            audioBufferinfo = null;
            audioFormat = null;
            audioEncodec = null;
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
                    encoder.get().videoExit = true;
                    if(encoder.get().audioExit){
                        mediaMuxer.stop();
                        mediaMuxer.release();
                        mediaMuxer = null;
                    }
                    break;
                }
                int outputBufferIndex = videoEncoder.dequeueOutputBuffer(videoBufferinfo,0);
                if(outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED)
                {
                    videoTrackIndex = mediaMuxer.addTrack(videoEncoder.getOutputFormat());
                    if(encoder.get().audioEncodecThread.audioTrackIndex != -1){
                        mediaMuxer.start();
                        encoder.get().encodecStart = true;
                    }

                }else{
                    while (outputBufferIndex >= 0){
                        if(encoder.get().encodecStart)
                        {
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

    static class AudioEncodecThread extends Thread{
        private WeakReference<BaseMediaEncoder> encoder;
        private boolean isExit;
        private MediaCodec audioEncodec;
        private MediaCodec.BufferInfo bufferInfo;
        private MediaMuxer mediaMuxer;
        private int audioTrackIndex;
        long pts;

        public AudioEncodecThread(WeakReference<BaseMediaEncoder> encoder) {
            this.encoder = encoder;
            audioEncodec = encoder.get().audioEncodec;
            bufferInfo = encoder.get().audioBufferinfo;
            mediaMuxer = encoder.get().mediaMuxer;
        }

        @Override
        public void run() {
            super.run();
            pts = 0;
            audioTrackIndex = -1;
            isExit = false;
            audioEncodec.start();
            while (true){
                if(isExit){

                    audioEncodec.stop();
                    audioEncodec.release();
                    audioEncodec = null;
                    encoder.get().audioExit = true;
                    if(encoder.get().videoExit){
                        mediaMuxer.stop();
                        mediaMuxer.release();
                        mediaMuxer = null;
                    }
                    break;
                }
                int outputBufferIndex = audioEncodec.dequeueOutputBuffer(bufferInfo,0);
                if(outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED)
                {
                    if(mediaMuxer != null){
                        audioTrackIndex = mediaMuxer.addTrack(audioEncodec.getOutputFormat());
                        if(encoder.get().videoEncodecThread.videoTrackIndex != -1)
                        {
                            mediaMuxer.start();
                            encoder.get().encodecStart = true;
                        }
                    }
                }else{
                    while (outputBufferIndex >= 0){
                        if(encoder.get().encodecStart){
                            ByteBuffer outputBuffer  = audioEncodec.getOutputBuffers()[outputBufferIndex];
                            outputBuffer.position(bufferInfo.offset);
                            outputBuffer.limit(bufferInfo.offset+bufferInfo.size);
                            if(pts == 0){
                                pts = bufferInfo.presentationTimeUs;
                            }
                            bufferInfo.presentationTimeUs = bufferInfo.presentationTimeUs - pts;
                            mediaMuxer.writeSampleData(audioTrackIndex,outputBuffer,bufferInfo);
                        }
                        audioEncodec.releaseOutputBuffer(outputBufferIndex,false);
                        audioTrackIndex = audioEncodec.dequeueOutputBuffer(bufferInfo,0);
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

    private long getAudioPts(int size,int sampleRate){
        audioPts += (long)(1.0*size / (sampleRate * 2 * 2) * 1000000.0);
        return audioPts;
    }
}
