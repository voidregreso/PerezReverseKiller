package com.perez.revkiller.exifremover;

import java.io.File;
import java.io.IOException;

import com.perez.revkiller.exifremover.model.EXIFRemover;

import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.ImageWriteException;

/**
 * Command line interface for exifremover.
 * 
 * @author deque
 * 
 */
public class Interfaz {

	private static final String TEMP_DIR = "temp";
	private static final String FILE_SEPARATOR = System.getProperty("file.separator");

	public static String showExif(String pth) throws IOException,
			ImageReadException {
		File file = new File(pth);
		EXIFRemover remover = new EXIFRemover(file);
		return remover.getExifData();
	}


	public static void deleteEXIF(String pth, String pth0) throws IOException,
			ImageReadException, ImageWriteException {
		File source = new File(pth);
		// if no destination folder given, assign source folder to dest
		File dest = new File(pth0);

		if (source.isDirectory()) {
			deleteEXIFFromDir(source, dest);
		} else {
			deleteEXIFForSingleFile(source, dest);
		}
	}

	/**
	 * Removes EXIF data from all files in a given source directory and saves
	 * new images into the destination directory. Destination directory is
	 * created if it doesn't exist.
	 * 
	 * @param source
	 *            source folder that contains the image files
	 * @param dest
	 *            destination folder where the new images are saved to
	 */
	private static boolean deleteEXIFFromDir(File source, File dest) {
		assert source.isDirectory();
		if (!dest.isDirectory()) {
			if (dest.exists()) {
				System.err.println("destination file is no directory");
				return false;
			} else {
				dest.mkdir();
			}

		}
		// create temp folder if destination folder equals source folder
		if (dest.equals(source)) {
			return removeAllEXIFFromFolder(source, new File(dest.getAbsolutePath()
					+ FILE_SEPARATOR + TEMP_DIR));
		} else {
			return removeAllEXIFFromFolder(source, dest);
		}
	}

	/**
	 * Removes EXIF data from a single image file which is the source file.
	 * Resulting images is saved into the destination file which may be the same
	 * as the source file (in that case the image is overwritten).
	 * 
	 * @param source
	 *            source folder that contains the image files
	 * @param dest
	 *            destination folder where the new images are saved to
	 * @throws IOException
	 * @throws ImageReadException
	 * @throws ImageWriteException
	 */
	private static void deleteEXIFForSingleFile(File source, File dest)
			throws IOException, ImageReadException, ImageWriteException {
		EXIFRemover remover = new EXIFRemover(source);
		remover.removeEXIFData(dest);
		System.out.println("All EXIF data successfully removed from "
				+ source.getAbsolutePath());
		System.out.println("Result saved in " + dest.getAbsolutePath());
	}

	private static boolean removeAllEXIFFromFolder(File srcFolder, File destFolder) {
		assert srcFolder.isDirectory() && destFolder.isDirectory();
		File[] files = srcFolder.listFiles();

		if (!destFolder.exists()) {
			destFolder.mkdir();
		}
		for (File file : files) {
			if (file.isFile() && (file.getName().endsWith(".jpg")) || file.getName().endsWith(".jpeg")) { // make sure that file is only jpeg
				try {
					EXIFRemover remover = new EXIFRemover(file);
					File destFile = new File(destFolder.getAbsolutePath()
							+ FILE_SEPARATOR + file.getName());
					remover.removeEXIFData(destFile);
					System.out.println("created file "
							+ destFile.getAbsolutePath());
				} catch (IOException e) {
					System.err.println("Can not convert or read file "
							+ file.getAbsolutePath());
					return false;
				} catch (ImageReadException e) {
					System.err.println("Unable to remove EXIF data from "
							+ file.getAbsolutePath());
					return false;
				} catch (ImageWriteException e) {
					System.err.println("Unable to remove EXIF data from "
							+ file.getAbsolutePath());
					return false;
				}
			}
		}
		return true;
	}


}
