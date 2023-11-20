package nl.kadaster.labs.unlocked.securedsparqlendpoint.endpoints;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

@RestController
public class GUIEndpoint {
    @GetMapping("/")
    public String index() throws IOException {
        try (var stream = GUIEndpoint.class.getResourceAsStream("/templates/gui.html")) {
            if (stream == null) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Existing file not found");
            } else {
                return new String(stream.readAllBytes());
            }
        }
    }
}
