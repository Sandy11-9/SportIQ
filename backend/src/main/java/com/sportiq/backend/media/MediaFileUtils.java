package com.sportiq.backend.media;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/** Best-effort deletion helpers for temp video/frame paths. */
public final class MediaFileUtils {

    private static final Logger log = LoggerFactory.getLogger(MediaFileUtils.class);

    private MediaFileUtils() {
    }

    public static void deleteQuietly(Path path) {
        if (path != null) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException ex) {
                log.warn("Could not delete temp file {}", path, ex);
            }
        }
    }

    public static void deleteTree(Path dir) {
        if (dir == null) {
            return;
        }
        try (Stream<Path> stream = Files.walk(dir)) {
            stream.sorted(Comparator.reverseOrder()).forEach(MediaFileUtils::deleteQuietly);
        } catch (IOException ex) {
            log.warn("Could not delete temp tree {}", dir, ex);
        }
    }
}