package com.sportiq.backend.media;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Runs external commands (ffmpeg/ffprobe) with a hard timeout. */
@Component
public class ProcessRunner {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    public CommandResult run(List<String> command) {
        Process process;
        try {
            process = new ProcessBuilder(command).redirectErrorStream(true).start();
        } catch (IOException ex) {
            return new CommandResult(-1, "Failed to start process: " + ex.getMessage());
        }

        try {
            boolean finished = process.waitFor(DEFAULT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                return new CommandResult(-1, "Command timed out after " + DEFAULT_TIMEOUT.toSeconds() + "s");
            }
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            return new CommandResult(process.exitValue(), output);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            return new CommandResult(-1, "Command interrupted");
        } catch (IOException ex) {
            return new CommandResult(-1, "Failed to read command output: " + ex.getMessage());
        }
    }
}