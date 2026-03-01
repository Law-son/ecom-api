package com.eyarko.ecom.controller;

import com.eyarko.ecom.dto.ApiResponse;
import com.eyarko.ecom.service.JfrProfilingService;
import com.eyarko.ecom.util.ResponseUtil;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Runtime profiling endpoints for Java Flight Recorder operations.
 */
@RestController
@RequestMapping("/api/v1/profiling/jfr")
@PreAuthorize("hasRole('ADMIN')")
public class ProfilingController {
    private final JfrProfilingService jfrProfilingService;

    public ProfilingController(JfrProfilingService jfrProfilingService) {
        this.jfrProfilingService = jfrProfilingService;
    }

    @PostMapping("/start")
    public ApiResponse<Map<String, Object>> start(
        @RequestParam(defaultValue = "60") int durationSeconds,
        @RequestParam(defaultValue = "profile") String settings
    ) {
        return ResponseUtil.success(
            "JFR recording started",
            jfrProfilingService.startRecording(durationSeconds, settings)
        );
    }

    @PostMapping("/stop")
    public ApiResponse<Map<String, Object>> stop(
        @RequestParam(required = false) String fileName
    ) {
        return ResponseUtil.success(
            "JFR recording stopped",
            jfrProfilingService.stopRecording(fileName)
        );
    }

    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> status() {
        return ResponseUtil.success("JFR status", jfrProfilingService.getStatus());
    }
}

