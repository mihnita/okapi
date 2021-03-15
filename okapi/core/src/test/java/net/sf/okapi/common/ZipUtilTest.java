package net.sf.okapi.common;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ZipUtilTest {

	private FileLocation root;

	@Before
	public void setUP() {
		root = FileLocation.fromClass(getClass());
	}

	@Test
	public void testSafeUnzip() throws Exception {
		Path p = Files.createTempDirectory("okapiZipUtilTest");
		try (ZipInputStream zis = new ZipInputStream(root.in("/testfiles.zip").asInputStream())) {
			ZipUtil.safeUnzip(zis, p);
		}
		assertTrue(Files.exists(p.resolve("files/1.txt")));
		assertTrue(Files.exists(p.resolve("files/2.txt")));
		FileUtil.deleteAllFilesInDirectory(p.resolve("files").toString());
		Files.delete(p.resolve("files"));
		Files.delete(p);
	}

	@Test
	public void testSafeUnzipDontAllowMaliciousFiles() throws Exception {
		boolean threwException = false;
		Path p = Files.createTempDirectory("okapiZipUtilTest");
		try (ZipInputStream zis = new ZipInputStream(root.in("/malicious.zip").asInputStream())) {
			ZipUtil.safeUnzip(zis, p);
		}
		catch (IllegalStateException e) {
			threwException = true; // Success!
		}
		if (!threwException) {
			fail("Allowed unsafe zip extraction");
		}
		assertFalse(Files.exists(p.getParent().resolve("malicious.txt")));
		FileUtil.deleteAllFilesInDirectory(p.toString());
	}

	@Test
	public void testZipFiles() throws Exception {
		String dir = FileLocation.fromClass(ZipUtilTest.class).in("/").toString();
		Path zipFile = Files.createTempFile("okapiZipUtilTest", ".zip");
		try {
			ZipUtil.zipFiles(zipFile.toString(), dir, "TestA.txt", "testB.tzt");
			try (ZipFile zf = new ZipFile(zipFile.toFile())) {
				assertNotNull(zf.getEntry("TestA.txt"));
				assertNotNull(zf.getEntry("testB.tzt"));
			}
		}
		finally {
			Files.deleteIfExists(zipFile);
		}
	}

	@Test
	public void testZipDirectory() throws Exception {
		String sourceDir = FileLocation.fromClass(ZipUtilTest.class).in("/zipDirTestFiles").toString();
		// Test with a non-standard extension
		ZipUtil.zipDirectory(sourceDir, ".foo");
		File out = FileLocation.fromClass(ZipUtilTest.class).in("/zipDirTestFiles.foo").asFile();
		try (ZipFile zf = new ZipFile(out)) {
			assertNotNull(zf.getEntry("1.txt"));
			assertNotNull(zf.getEntry("sub/1.txt"));
		}
		finally {
			Files.deleteIfExists(out.toPath());
		}
	}
}
