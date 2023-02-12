package com.perez.revkiller.exifremover;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class FileIO {

	/**
	 * @param file
	 *            file to get the bytes from
	 * @return byte array that represents the given file
	 * @throws IOException
	 */
	public static byte[] getBytesFromFile(File file) throws IOException {
		byte[] data = null;
		FileInputStream fileInputStream = null;
		try {
			fileInputStream = new FileInputStream(file);
			data = new byte[(int) file.length()];
			fileInputStream.read(data);
		} finally {
			if (fileInputStream != null) {
				try {
					fileInputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return data;
	}

}
