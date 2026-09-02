package com.sportiq.backend.media;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Hourly sweep that removes stale uploads/frame dirs from the temp dir.
 * Safety net only: {@code AnalysisService} cleans up on every request.
 */
@Service
public class FileCleanupService {

    private static final Logger log = LoggerFactory.getLogger(FileCleanupService.class);

    private final MediaProperties properties;

    public FileCleanupService(MediaProperties properties) {
        this.properties = properties;
    }

    @Scheduled(cron = "0 0 * * * *")
    public void sweepExpiredFiles() {
        Path tempDir = Path.of(properties.tempDir());
        if (!Files.isDirectory(tempDir)) {
            return;
        }
        Instant cutoff = Instant.now().minusSeconds(properties.tempMaxAgeHours() * 3600);
        try (Stream<Path> stream = Files.walk(tempDir)) {
            stream.sorted(Comparator.reverseOrder())
                    .filter(p -> !p.equals(tempDir))
                    .filter(p -> p.toFile().isDirectory() || p.toFile().lastModified() < cutoff.toEpochMilli())
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                            log.info("Swept stale temp path {}", p);
                        } catch (IOException ex) {
                            log.warn("Could not sweep {}", p, ex);
                        }
                    });
        } catch (IOException ex) {
            log.warn("Temp dir sweep failed", ex);
        }
    }
}