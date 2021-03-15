package net.sf.okapi.common;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import net.sf.okapi.common.exceptions.OkapiIOException;

public class ZipUtil {

	/**
	 * Extract a ZipInputStream to the target destination directory. Does not allow
	 * malicious ZIP packages to escape from the prescribed directory tree.
	 * @param zis
	 * @param unzipDirectory
	 * @throws IOException
	 */
	public static void safeUnzip(ZipInputStream zis, Path unzipDirectory) throws IOException {
		Files.createDirectories(unzipDirectory);

		ZipEntry entry = null;
		while ((entry = zis.getNextEntry()) != null) {
			String outFilename = entry.getName();
			Path p = unzipDirectory.resolve(outFilename).normalize();
			if (!p.startsWith(unzipDirectory)) {
				throw new IllegalStateException("Zip entry points to path outside of target directory: " +
												entry.getName());
			}
			if (entry.isDirectory()) {
				Files.createDirectories(p);
			}
			else {
				// Create the parent directory of this entry, if it doesn't already exist
				if (!Files.exists(p.getParent())) {
					Files.createDirectories(p.getParent());
				}
				Files.copy(zis, p, StandardCopyOption.REPLACE_EXISTING);
			}
		}
	}

	/**
	 * Convenience method.
	 * @param zipFile
	 * @param unzipDirectory
	 * @throws IOException
	 */
	public static void safeUnzip(Path zipFile, Path unzipDirectory) throws IOException {
		try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile))) {
			safeUnzip(zis, unzipDirectory);
		}
	}

	/**
	 * Compresses a given directory. Creates in the same parent folder a ZIP
	 * file with the folder name as the file name and a given extension. The
	 * given directory is not deleted after compression.
	 * <p>
	 * This method uses the Java ZIP package and does not supports files to zip
	 * that have a path with extended characters.
	 * 
	 * @param sourceDir
	 *            the given directory to be compressed
	 * @param zipExtension
	 *            an extension for the output ZIP file (default is .zip if a
	 *            null or empty string is passed by the caller). The extension
	 *            is expected to contain the leading period.
	 */
	public static void zipDirectory(String sourceDir, String zipExtension) {
		zipDirectory(sourceDir, zipExtension, null);
	}

	/**
	 * Compresses a given directory. The given directory is not deleted after
	 * compression.
	 * <p>
	 * This method uses the Java ZIP package and does not supports files to zip
	 * that have a path with extended characters.
	 * 
	 * @param sourceDir
	 *            the given directory to be compressed
	 * @param zipExtension
	 *            an extension for the output ZIP file (default is .zip if a
	 *            null or empty string is passed by the caller). The extension
	 *            is expected to contain the leading period.
	 * @param destinationPathWithoutExtension
	 *            output path of the zip file, without extension. Use null to
	 *            use the source directory path.
	 */
	public static void zipDirectory(String sourceDir, String zipExtension,
			String destinationPathWithoutExtension) {
		String zipPath = null;

		if (Util.isEmpty(destinationPathWithoutExtension)) {
			// Use the directory as the destination path
			if (sourceDir.endsWith(File.separator) || sourceDir.endsWith("/")) {
				zipPath = sourceDir.substring(0, sourceDir.length() - 1);
			} else {
				zipPath = sourceDir;
			}
		} else { // destinationPathWithoutExtension is specified
			zipPath = destinationPathWithoutExtension;
		}
		// Set the extension
		if (Util.isEmpty(zipExtension)) {
			zipPath += ".zip";
		} else {
			zipPath += zipExtension;
		}

		// Compress the directory
		try (ZipOutputStream os = new ZipOutputStream(new FileOutputStream(zipPath))) {
			File dir = new File(sourceDir);
			if (!dir.isDirectory()) {
				return;
			}
			addDirectoryToZip(dir, os, null);
		} catch (IOException e) {
			throw new OkapiIOException("Error while zipping.", e);
		}
	}

	/**
	 * Creates a ZIP file and adds a list of files in it.
	 * 
	 * @param zipPath
	 *            the path of the ZIP file to create.
	 * @param sourceDir
	 *            the path of the directory where the source files are located.
	 * @param filenames
	 *            the list of files to zip.
	 */
	public static void zipFiles(String zipPath, String sourceDir, String... filenames) {
		try (ZipOutputStream os = new ZipOutputStream(new FileOutputStream(zipPath))) {
			// Creates the zip file
			Util.createDirectories(zipPath);
			Path sourceDirPath = Paths.get(sourceDir);
			for (String name : filenames) {
				Path file = sourceDirPath.resolve(name);
				os.putNextEntry(new ZipEntry(file.getFileName().toString()));
				Files.copy(file, os);
				os.closeEntry();
			}
		} catch (IOException e) {
			throw new OkapiIOException("Error while zipping.", e);
		}
	}

	/**
	 * Adds a directory to a ZIP output.
	 * 
	 * @param dir
	 *            the directory to add.
	 * @param os
	 *            the output stream where to add it.
	 * @param subDir
	 *            the sub-directory.
	 * @throws IOException
	 *             signals an I/O error.
	 */
	private static void addDirectoryToZip(File dir, ZipOutputStream os, String subDir) throws IOException {
		for (File file : dir.listFiles()) {
			// Go recursively if the entry is a sub-directory
			if (file.isDirectory()) {
				addDirectoryToZip(file, os, ((subDir == null) ? "" : subDir
						+ "/")
						+ file.getName());
				continue;
			}
			// Or add the file to the zip
			os.putNextEntry(new ZipEntry(((subDir == null) ? "" : subDir
					+ "/")
					+ file.getName()));
			Files.copy(file.toPath(), os);
			os.closeEntry();
		}
	}
}
