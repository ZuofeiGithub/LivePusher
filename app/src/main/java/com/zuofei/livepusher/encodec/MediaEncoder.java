package com.zuofei.livepusher.encodec;

import android.content.Context;

public class MediaEncoder extends BaseMediaEncoder {

    private EncoderRender encoderRender;
    public MediaEncoder(Context context,int textureId) {
        super(context);
        encoderRender = new EncoderRender(context,textureId);
        setRender(encoderRender);
        setRenderMode(BaseMediaEncoder.RENDERMODE_CONTINUOUSLY);
    }
}
