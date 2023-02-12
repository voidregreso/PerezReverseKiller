package com.perez.qrcode.decode;

import java.util.concurrent.CountDownLatch;

import com.perez.qrcode.CaptureActivity;

import android.os.Handler;
import android.os.Looper;

/**
 * : ()
 * 
 * : 201459 12:24:34
 *
 * : V_1.0.0
 *
 * : 
 */
final class DecodeThread extends Thread {

	CaptureActivity activity;
	private Handler handler;
	private final CountDownLatch handlerInitLatch;

	DecodeThread(CaptureActivity activity) {
		this.activity = activity;
		handlerInitLatch = new CountDownLatch(1);
	}

	Handler getHandler() {
		try {
			handlerInitLatch.await();
		} catch (InterruptedException ie) {
			// continue?
		}
		return handler;
	}

	@Override
	public void run() {
		Looper.prepare();
		handler = new DecodeHandler(activity);
		handlerInitLatch.countDown();
		Looper.loop();
	}

}
