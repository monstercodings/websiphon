package top.codings.websiphon.util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

public final class ByteUtils {
    private ByteUtils() {
    }

    public static byte[] readAllBytes(InputStream inputStream) {
        try {
            if (!(inputStream instanceof BufferedInputStream)) {
                inputStream = new BufferedInputStream(inputStream);
            }
            byte[] bytes = new byte[inputStream.available()];
            inputStream.read(bytes);
            return bytes;
        } catch (Exception e) {
            return new byte[0];
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
            }
        }
    }
}
