package nl.kadaster.labs.unlocked.securedsparqlendpoint.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Optional;
import java.util.function.BiFunction;

public enum GlobUtil {
    ;

    public static Collection<Path> findAll(Path root, String pattern) throws IOException {
        root = root.normalize().toAbsolutePath();
        var matcher = getMatcher(root, pattern);
        Collection<Path> paths = new LinkedList<>();

        walk(root, (file, attributes) -> {
            if (matcher.matches(file)) {
                paths.add(file);
            }
            return FileVisitResult.CONTINUE;
        });

        return paths;
    }

    public static Optional<Path> findFirst(Path root, String pattern) throws IOException {
        root = root.normalize().toAbsolutePath();
        var matcher = getMatcher(root, pattern);
        Path[] ref = {null};

        walk(root, (file, attributes) -> {
            if (matcher.matches(file)) {
                ref[0] = file;
                return FileVisitResult.TERMINATE;
            }
            return FileVisitResult.CONTINUE;
        });

        return Optional.ofNullable(ref[0]);
    }

    private static PathMatcher getMatcher(Path root, String pattern) {
        return root.getFileSystem().getPathMatcher("glob:" + root + File.separator + pattern);
    }

    private static void walk(Path root, BiFunction<Path, BasicFileAttributes, FileVisitResult> walker) throws IOException {
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            public FileVisitResult visitFile(Path path, BasicFileAttributes attributes) {
                return walker.apply(path, attributes);
            }
        });
    }
}

