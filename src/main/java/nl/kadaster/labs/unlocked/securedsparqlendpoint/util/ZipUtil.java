package nl.kadaster.labs.unlocked.securedsparqlendpoint.util;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
public enum ZipUtil {
    ;
    public static Map<String, String> readFiles(ZipInputStream archive) throws IOException {
        Map<String, String> files = new HashMap<>();

        ZipEntry entry;
        while ((entry = archive.getNextEntry()) != null) {
            log.debug("Unzipping [{}]", entry.getName());
            files.put(entry.getName(), IOUtil.capture(archive));
        }

        return files;
    }
}
