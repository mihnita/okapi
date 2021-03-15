package net.sf.okapi.common;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Helper class that gives access to the test data files (both in and out).
 * For output {@link #asFile()} and {@link #asOutputStream()} it creates the necessary folders and
 * marks everything (files and folders) as {@code deleteOnExit()}.
 */
public class FileLocation {
	public static final String ROOT_FOLDER = "/";
	public static final String CLASS_FOLDER = "";
	
	private final Class<?> clazz;

	private abstract static class FileLocationBase {
		protected final Class<?> clazz;
		protected final URL url;
		protected final String resourceName;
		protected final boolean isOutput;

		private FileLocationBase(Class<?> clazz, String resourceName, boolean isOutput) {
			this.clazz = clazz;
			this.resourceName = null == resourceName ? "" : resourceName;
			this.isOutput = isOutput;
			this.url = FileLocationImpl.makeUrlFromName(this.clazz, this.resourceName, isOutput);
		}

		public URL asUrl() {
			return url;
		}

		public File asFile() {
			try {
				File file = new File(url.toURI());
				if (isOutput) {
					if (resourceName.isEmpty()) { // folder
						file.mkdirs();
						file.deleteOnExit();
					} else { // file
						file.getParentFile().mkdirs();
						file.getParentFile().deleteOnExit();
						file.deleteOnExit();
					}
				}

				return file;
			} catch (URISyntaxException e) {
				e.printStackTrace();
				return null;
			}
		}

		public URI asUri() {
			try {
				return url.toURI();
			} catch (URISyntaxException e) {
				e.printStackTrace();
				return null;
			}
		}

		public Path asPath() {
			try {
				return Paths.get(url.toURI());
			} catch (URISyntaxException e) {
				e.printStackTrace();
				return null;
			}
		}

		@Override
		public String toString() {
			Path path = asPath();
			return path == null ? null : path.toString();
		}
	}  

	public static class In extends FileLocationBase {

		private In(Class<?> clazz, String resourceName) {
			super(clazz, resourceName, false);
		}

		public InputStream asInputStream() {
			if (resourceName.isEmpty()) {
				throw new IllegalArgumentException(
						"Can't get InputStream for a folder (" + url + ")");
			}
			try {
				return url.openStream();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}
	}  

	public static class Out extends FileLocationBase {

		private Out(Class<?> clazz, String resourceName) {
			super(clazz, resourceName, true);
		}

		public void makeOutputDir() {
			asFile();
		}

		public OutputStream asOutputStream() {
			if (resourceName.isEmpty()) {
				throw new IllegalArgumentException(
						"Can't get OutputStream for a folder (" + url + ")");
			}

			File file = asFile();
			if (file != null) {
				try {
					return new FileOutputStream(file);
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			}
			return null;
		}
	}

	private FileLocation(Class<?> clazz) {
		this.clazz = clazz;
	}

	public static FileLocation fromClass(Class<?> clazz) {
		return new FileLocation(clazz);
	}

	public Out out(String name) {
		return new Out(clazz, name);
	}

	public In in(String name) {
		return new In(clazz, name);
	}
}
