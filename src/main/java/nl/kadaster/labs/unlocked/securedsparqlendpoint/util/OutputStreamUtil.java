package nl.kadaster.labs.unlocked.securedsparqlendpoint.util;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.function.Consumer;

public enum OutputStreamUtil {
    ;

    public static String capture(Consumer<OutputStream> function) {
        var stream = new ByteArrayOutputStream();
        function.accept(stream);
        return stream.toString();
    }
}
