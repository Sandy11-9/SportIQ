package com.sportiq.backend.media;

/** Exit code and captured (stdout+stderr) output of an external command. */
public record CommandResult(int exitCode, String output) {
}