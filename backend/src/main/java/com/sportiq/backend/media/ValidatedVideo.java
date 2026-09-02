package com.sportiq.backend.media;

import java.nio.file.Path;

/** Validated video stored on disk, ready for frame extraction. */
public record ValidatedVideo(Path path, double durationSeconds) {
}