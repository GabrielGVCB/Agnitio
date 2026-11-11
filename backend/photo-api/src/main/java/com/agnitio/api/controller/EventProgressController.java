package com.agnitio.api.controller;

import com.agnitio.application.dto.EventProgress;
import com.agnitio.application.ports.output.PhotoProgressTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/events/{id}/progress")
public class EventProgressController {

    private static final Logger log = LoggerFactory.getLogger(EventProgressController.class);
    private final PhotoProgressTracker photoProgressTracker;

    public EventProgressController(PhotoProgressTracker photoProgressTracker) {
        this.photoProgressTracker = Objects.requireNonNull(photoProgressTracker, "photoProgressTracker must not be null");
    }

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamProgress(@PathVariable("id") UUID eventId) {
        // 60-second SSE stream
        SseEmitter emitter = new SseEmitter(60_000L);
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

        ScheduledFuture<?> future = executor.scheduleAtFixedRate(() -> {
            try {
                EventProgress progress = photoProgressTracker.getProgress(eventId);
                emitter.send(SseEmitter.event()
                        .name("progress")
                        .data(new ProgressResponseDto(
                                progress.total(),
                                progress.processed(),
                                progress.failed(),
                                progress.isCompleted(),
                                progress.getPercentage()
                        )));

                if (progress.isCompleted()) {
                    emitter.complete();
                    executor.shutdown();
                }
            } catch (IOException e) {
                log.debug("SSE client disconnected for event {}", eventId);
                emitter.complete();
                executor.shutdown();
            } catch (Exception e) {
                log.error("Error streaming progress for event {}: {}", eventId, e.getMessage());
                emitter.completeWithError(e);
                executor.shutdown();
            }
        }, 0, 1000, TimeUnit.MILLISECONDS);

        emitter.onCompletion(() -> {
            future.cancel(true);
            executor.shutdown();
        });
        emitter.onTimeout(() -> {
            future.cancel(true);
            executor.shutdown();
        });
        emitter.onError(e -> {
            future.cancel(true);
            executor.shutdown();
        });

        return emitter;
    }

    @GetMapping("/status")
    public ResponseEntity<ProgressResponseDto> getProgressStatus(@PathVariable("id") UUID eventId) {
        EventProgress progress = photoProgressTracker.getProgress(eventId);
        return ResponseEntity.ok(new ProgressResponseDto(
                progress.total(),
                progress.processed(),
                progress.failed(),
                progress.isCompleted(),
                progress.getPercentage()
        ));
    }

    public record ProgressResponseDto(
            int total,
            int processed,
            int failed,
            boolean completed,
            double percentage
    ) {
    }
}
