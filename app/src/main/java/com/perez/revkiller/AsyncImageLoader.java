package com.perez.revkiller;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.widget.ImageView;

import com.perez.RealFuncUtil;

import java.lang.ref.SoftReference;
import java.util.HashMap;

public class AsyncImageLoader {

    private HashMap<String, SoftReference<Drawable>> imageCache;
    private Context ctx;

    public AsyncImageLoader(Context ctx) {
        imageCache = new HashMap<>();
        this.ctx = ctx;
    }

    public Drawable loadDrawable(final String imageUrl, final ImageView imageView,
                                 final PerezReverseKillerMain.ImageCallback imageCallback) {
        if(imageCache.containsKey(imageUrl)) {
            SoftReference<Drawable> softReference = imageCache.get(imageUrl);
            Drawable drawable = softReference.get();
            if(drawable != null)
                return drawable;
        }
        final Handler handler = new Handler() {
            public void handleMessage(Message message) {
                imageCallback.imageLoaded((Drawable) message.obj, imageView);
            }
        };
        new Thread() {
            public void run() {
                Drawable drawable = RealFuncUtil.showApkIcon(ctx, imageUrl);
                imageCache.put(imageUrl, new SoftReference<>(drawable));
                Message message = handler.obtainMessage(0, drawable);
                handler.sendMessage(message);
            }
        } .start();
        return ctx.getResources().getDrawable(R.drawable.android);
    }
}