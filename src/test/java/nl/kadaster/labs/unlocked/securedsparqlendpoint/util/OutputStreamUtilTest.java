package nl.kadaster.labs.unlocked.securedsparqlendpoint.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class OutputStreamUtilTest {
    @Test
    void capture() {
        var result = OutputStreamUtil.capture(output -> {
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
}
