package nl.kadaster.labs.unlocked.securedsparqlendpoint.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Consumer;

public enum IOUtil {
    ;

    public static String capture(Consumer<OutputStream> function) {
        var stream = new ByteArrayOutputStream();
        function.accept(stream);
        return stream.toString();
    }

    public static String capture(InputStream inStream) throws IOException {
        var outStream = new ByteArrayOutputStream();

        int len;
        byte[] buffer = new byte[4096];
        while ((len = inStream.read(buffer)) > 0) {
            outStream.write(buffer, 0, len);
        }

        return outStream.toString();
    }
}
