package com.zuofei.livepusher.utils;

import android.content.Context;
import android.util.DisplayMetrics;

public class DisplayUtil {
    public static int getScreenWidth(Context context){
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return metrics.widthPixels;
    }

    public static int getScreenHeight(Context context){
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return metrics.heightPixels;
    }
}
