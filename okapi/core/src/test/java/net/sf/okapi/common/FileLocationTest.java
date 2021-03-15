package net.sf.okapi.common;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class FileLocationTest {

	@Test
	public void getParentDir_ValidFile() {
		assertTrue("Incorrect path returned",
				FileLocation.fromClass(this.getClass())
				.in(FileLocation.ROOT_FOLDER)
				.asUrl().toString().endsWith("/target/test-classes/"));
	}

	@Test
	public void testInputAccess() {
		FileLocation location = FileLocation.fromClass(this.getClass());

		assertTrue("Incorrect path returned",
				location.in(FileLocation.ROOT_FOLDER).asUrl().toString()
				.endsWith("/okapi/core/target/test-classes/"));

		assertTrue("Incorrect path returned",
				location.in(FileLocation.CLASS_FOLDER).asUrl().toString()
				.endsWith("okapi/core/target/test-classes/net/sf/okapi/common/"));

		assertTrue("Incorrect path returned",
				location.in("/test.txt").asUrl().toString()
				.endsWith("okapi/core/target/test-classes/test.txt"));

		assertTrue("Incorrect path returned",
				location.in("test.txt").asUrl().toString()
				.endsWith("okapi/core/target/test-classes/net/sf/okapi/common/test.txt"));
	}

	@Test
	public void testOutputAccess() {
		FileLocation location = FileLocation.fromClass(this.getClass());

		assertTrue("Incorrect path returned",
				location.out(FileLocation.ROOT_FOLDER).asUrl().toString()
				.endsWith("/okapi/core/target/test-classes/out/"));

		assertTrue("Incorrect path returned",
				location.out(FileLocation.CLASS_FOLDER).asUrl().toString()
				.endsWith("okapi/core/target/test-classes/out/net/sf/okapi/common/"));

		assertTrue("Incorrect path returned",
				location.out("/test.txt").asUrl().toString()
				.endsWith("okapi/core/target/test-classes/out/test.txt"));

		assertTrue("Incorrect path returned",
				location.out("test.txt").asUrl().toString()
				.endsWith("okapi/core/target/test-classes/out/net/sf/okapi/common/test.txt"));
	}

	@Test
	public void testReuse() {
		FileLocation location = FileLocation.fromClass(this.getClass());

		assertTrue("Incorrect path returned",
				location.in(FileLocation.ROOT_FOLDER).asUrl().toString()
				.endsWith("/okapi/core/target/test-classes/"));

		assertTrue("Incorrect path returned",
				location.out(FileLocation.CLASS_FOLDER).asUrl().toString()
				.endsWith("okapi/core/target/test-classes/out/net/sf/okapi/common/"));

		assertTrue("Incorrect path returned",
				location.in("/test.txt").asUrl().toString()
				.endsWith("okapi/core/target/test-classes/test.txt"));

		assertTrue("Incorrect path returned",
				location.out("test.txt").asUrl().toString()
				.endsWith("okapi/core/target/test-classes/out/net/sf/okapi/common/test.txt"));
	}

	@Test
	public void testVariousOutputTypes() throws IOException {
		final String EXPECTEDU_END = "okapi/core/target/test-classes/TestUtilTestTestFile.txt";
		final String EXPECTED_END = EXPECTEDU_END.replace('/', File.separatorChar);
		FileLocation.In location = FileLocation.fromClass(this.getClass())
				.in("/TestUtilTestTestFile.txt");

		assertTrue("Incorrect file returned",
				location.asFile().getPath()
				.endsWith(EXPECTED_END));

		assertTrue("Incorrect path returned",
				location.asPath().toString()
				.endsWith(EXPECTED_END));

		String tmp = location.asUrl().toString();
		assertTrue("Incorrect url returned",
				tmp.endsWith(EXPECTEDU_END));
		assertTrue("Incorrect url returned",
				tmp.startsWith("file:/"));

		tmp = location.asUri().toString();
		assertTrue("Incorrect uri returned",
				tmp.endsWith(EXPECTEDU_END));
		assertTrue("Incorrect uri returned",
				tmp.startsWith("file:/"));

		assertTrue("Incorrect string returned",
				location.toString()
				.endsWith(EXPECTED_END));

		try (InputStream in = location.asInputStream()) {
			assertEquals(34, in.available());
			byte [] buffer = new byte[in.available()];
			in.read(buffer, 0, buffer.length);
			assertEquals("This file is for the TestUtil Yay!", new String(buffer, StandardCharsets.UTF_8));
		}
	}

	@Test
	public void testOutAndIn() throws FileNotFoundException, IOException {
		final byte [] buffer = { 0x12, 0x34, 0x56, 0x78 };

		FileLocation.Out location = FileLocation.fromClass(this.getClass()).out("/OutAndIn.txt");

		try (OutputStream os = location.asOutputStream()) {
			os.write(buffer);
		}

		byte [] bufferIn = Files.readAllBytes(location.asPath());
		assertArrayEquals(buffer, bufferIn);
	}
}
