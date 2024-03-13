package nl.kadaster.labs.unlocked.securedsparqlendpoint.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

public class IOUtilTest {
    @Test
    void captureOutputStream() {
        var result = IOUtil.capture(output -> {
            try {
                for (int i = 65; i < 91; i++) {
                    output.write(i);
                }
            } catch (IOException exception) {
                Assertions.fail("Writing shouldn't have thrown an exception", exception);
            }
        });

        Assertions.assertEquals("ABCDEFGHIJKLMNOPQRSTUVWXYZ", result);
    }

    @Test
    void captureInputStream() throws IOException {
        var result = IOUtil.capture(new InputStream() {
            private int val = 64;

            public int read() {
                this.val += 1;
                return this.val > 90 ? -1 : this.val;
            }
        });
        Assertions.assertEquals("ABCDEFGHIJKLMNOPQRSTUVWXYZ", result);
    }
}
