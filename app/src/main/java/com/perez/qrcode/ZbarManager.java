package com.perez.qrcode;

public class ZbarManager {

	public native String decode(byte[] data, int width, int height, boolean isCrop, int x, int y, int cwidth, int cheight);
}
