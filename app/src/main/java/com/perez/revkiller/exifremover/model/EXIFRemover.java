package com.perez.revkiller.exifremover.model;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.ImageWriteException;
import org.apache.sanselan.Sanselan;
import org.apache.sanselan.common.IImageMetadata;
import org.apache.sanselan.formats.jpeg.exifRewrite.ExifRewriter;

import com.perez.revkiller.exifremover.FileIO;

/**
 * A class that utilizes the sanselan library to show or remove EXIF data
 * 
 * @author deque
 * 
 */
public class EXIFRemover {

	private final byte[] imageBytes;

	/**
	 * Constructor. Retrieves image bytes from given image file.
	 *
	 * @param image
	 *            file that shall be treated
	 * @throws IOException
	 */
	public EXIFRemover(File image) throws IOException {
		imageBytes = FileIO.getBytesFromFile(image);
	}

	/**
	 * @return String that describes the meta data of the given image file.
	 * @throws ImageReadException
	 * @throws IOException
	 */
	public String getExifData() throws ImageReadException, IOException {
		IImageMetadata metdata = Sanselan.getMetadata(imageBytes);
		if(metdata != null) {
			return metdata.toString();
		}
		return "no metadata found";
	}

	/**
	 * Completely deletes EXIF data from the extracted image bytes. Saves the
	 * resulting images bytes into the destination file.
	 * 
	 * @param dest
	 *            the destination file
	 * @throws ImageReadException
	 * @throws ImageWriteException
	 * @throws IOException
	 */
	public void removeEXIFData(File dest) throws ImageReadException,
			ImageWriteException, IOException {
		OutputStream os = null;
		try {
			os = new FileOutputStream(dest);
			ExifRewriter exifWriter = new ExifRewriter();
			exifWriter.removeExifMetadata(imageBytes, os);
		} finally {
			if (os != null) {
				try {
					os.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

}
