package net.sf.okapi.common;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

/**
 * Class to hold test utility methods
 * @author Christian Hargraves
 */
public class TestUtil {
    public static String getFileAsString(final File file) throws IOException {
        try (final BOMAwareInputStream bis = new BOMAwareInputStream(new FileInputStream(file), "UTF-8")) {
            return StreamUtil.streamAsString(bis, bis.detectEncoding());
        }
    }

    public static void writeString(String str, String filePath, String encoding) throws java.io.IOException{
        if (Util.isEmpty(encoding))
            encoding = Charset.defaultCharset().name();
        try (FileOutputStream fos = new FileOutputStream(filePath);
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fos, encoding))) {
            writer.write(str);
        }
    }

    public static String inputStreamAsString(InputStream is) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] b = new byte[4096];
        for (int n; (n = is.read(b)) != -1;) {
            out.write(b, 0, n);
        }
        return out.toString("UTF-8");
    }
}
