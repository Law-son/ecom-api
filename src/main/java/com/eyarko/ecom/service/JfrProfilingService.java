package com.eyarko.ecom.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import jdk.jfr.Configuration;
import jdk.jfr.Recording;
import jdk.jfr.RecordingState;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/**
 * Manages Java Flight Recorder sessions for runtime profiling.
 */
@Service
public class JfrProfilingService {
    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final Object monitor = new Object();
    private final boolean enabled;
    private final Path outputDirectory;

    private Recording activeRecording;
    private Instant startedAt;

    public JfrProfilingService(
        @Value("${app.profiling.jfr.enabled:true}") boolean enabled,
        @Value("${app.profiling.jfr.output-dir:target/profiling}") String outputDir
    ) {
        this.enabled = enabled;
        this.outputDirectory = Paths.get(outputDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.outputDirectory);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to create JFR output directory: " + this.outputDirectory, ex);
        }
    }

    public Map<String, Object> startRecording(int durationSeconds, String settings) {
        if (!enabled) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "JFR profiling is disabled");
        }
        if (durationSeconds <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "durationSeconds must be greater than 0");
        }
        String resolvedSettings = StringUtils.hasText(settings) ? settings : "profile";

        synchronized (monitor) {
            cleanupClosedOrStoppedRecording();
            if (isRunning(activeRecording)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "A JFR recording is already running");
            }

            try {
                Configuration configuration = Configuration.getConfiguration(resolvedSettings);
                Recording recording = new Recording(configuration);
                recording.setName("ecom-jfr");
                recording.setToDisk(true);
                recording.setDuration(Duration.ofSeconds(durationSeconds));
                recording.start();

                this.activeRecording = recording;
                this.startedAt = Instant.now();
                return statusMap(recording, "JFR recording started");
            } catch (IOException | ParseException ex) {
                throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to start JFR recording: " + ex.getMessage(),
                    ex
                );
            }
        }
    }

    public Map<String, Object> stopRecording(String fileName) {
        if (!enabled) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "JFR profiling is disabled");
        }

        synchronized (monitor) {
            if (activeRecording == null || activeRecording.getState() == RecordingState.CLOSED) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "No active JFR recording to stop");
            }
            try {
                if (activeRecording.getState() == RecordingState.RUNNING) {
                    activeRecording.stop();
                }
                Path targetFile = resolveOutputFile(fileName);
                activeRecording.dump(targetFile);
                Map<String, Object> result = statusMap(activeRecording, "JFR recording stopped");
                result.put("filePath", targetFile.toString());
                activeRecording.close();
                activeRecording = null;
                startedAt = null;
                return result;
            } catch (IOException ex) {
                throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to stop/dump JFR recording: " + ex.getMessage(),
                    ex
                );
            }
        }
    }

    public Map<String, Object> getStatus() {
        synchronized (monitor) {
            if (activeRecording == null) {
                Map<String, Object> status = new LinkedHashMap<>();
                status.put("enabled", enabled);
                status.put("state", "NONE");
                status.put("outputDirectory", outputDirectory.toString());
                return status;
            }
            return statusMap(activeRecording, "JFR status");
        }
    }

    private void cleanupClosedOrStoppedRecording() {
        if (activeRecording == null) {
            return;
        }
        RecordingState state = activeRecording.getState();
        if (state == RecordingState.STOPPED || state == RecordingState.CLOSED) {
            try {
                activeRecording.close();
            } catch (Exception ex) {
                // Ignore close failures for stale recordings.
            }
            activeRecording = null;
            startedAt = null;
        }
    }

    private boolean isRunning(Recording recording) {
        return recording != null && recording.getState() == RecordingState.RUNNING;
    }

    private Path resolveOutputFile(String fileName) {
        String resolvedName = StringUtils.hasText(fileName)
            ? fileName
            : "ecom-jfr-" + TS_FORMAT.format(java.time.LocalDateTime.now()) + ".jfr";
        if (!resolvedName.endsWith(".jfr")) {
            resolvedName = resolvedName + ".jfr";
        }
        return outputDirectory.resolve(resolvedName).normalize();
    }

    private Map<String, Object> statusMap(Recording recording, String message) {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("message", message);
        status.put("enabled", enabled);
        status.put("recordingId", recording.getId());
        status.put("recordingName", recording.getName());
        status.put("state", recording.getState().name());
        status.put("settings", recording.getSettings());
        status.put("outputDirectory", outputDirectory.toString());
        status.put("startedAt", startedAt != null ? startedAt.toString() : null);
        return status;
    }
}

