package com.sportiq.backend.media;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileCleanupServiceTest {

    private Path root;

    private Path tempDir() throws IOException {
        root = Files.createTempDirectory("sportiq-cleanup-test");
        return root;
    }

    @AfterEach
    void cleanup() throws IOException {
        if (root != null) {
            try (var stream = Files.walk(root)) {
                stream.sorted(java.util.Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
            }
        }
    }

    @Test
    void removesOnlyExpiredFilesAndEmptyDirs() throws IOException {
        Path tempDir = tempDir();
        MediaProperties properties = new MediaProperties(tempDir.toString(), 1, 5, 20, 10,
                50L * 1024 * 1024, null);
        FileCleanupService service = new FileCleanupService(properties);

        Path stale = tempDir.resolve("stale.mp4");
        Path fresh = tempDir.resolve("fresh.mp4");
        Files.write(stale, new byte[]{1});
        Files.write(fresh, new byte[]{2});
        stale.toFile().setLastModified(System.currentTimeMillis() - 2 * 3600 * 1000);

        service.sweepExpiredFiles();

        assertFalse(Files.exists(stale), "stale file should be swept");
        assertTrue(Files.exists(fresh), "fresh file should survive");
    }

    @Test
    void emptyRootInnerDirectoryIsRemoved() throws IOException {
        Path tempDir = tempDir();
        Files.createDirectories(tempDir.resolve("abandoned/frames"));
        Path stale = tempDir.resolve("abandoned/frames/.placeholder");
        Files.write(stale, new byte[]{1});
        stale.toFile().setLastModified(System.currentTimeMillis() - 2 * 3600 * 1000);

        new FileCleanupService(new MediaProperties(tempDir.toString(), 1, 5, 20, 10,
                50L * 1024 * 1024, null)).sweepExpiredFiles();

        assertFalse(Files.exists(tempDir.resolve("abandoned")), "expired empty dirs should be swept");
    }

    @Test
    void sweepIsSafeWhenTempDirMissing() {
        Path missing = Path.of("/tmp/sportiq-does-not-exist-" + UUID.randomUUID());
        new FileCleanupService(new MediaProperties(missing.toString(), 1, 5, 20, 10,
                50L * 1024 * 1024, null)).sweepExpiredFiles();
    }
}