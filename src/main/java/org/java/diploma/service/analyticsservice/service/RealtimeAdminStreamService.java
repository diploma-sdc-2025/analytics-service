package org.java.diploma.service.analyticsservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Owns the SSE serving layer for the admin realtime dashboard.
 *
 * <ul>
 *   <li>Keeps a thread-safe set of active emitters.</li>
 *   <li>Coalesces broadcast requests into the next {@link #BROADCAST_DEBOUNCE_MS}-second window so a burst of
 *       events fans out as one snapshot per client (caps work at events × clients).</li>
 *   <li>Sends a {@code :keepalive} comment heartbeat every {@link #HEARTBEAT_INTERVAL_MS} ms so reverse proxies
 *       and load balancers don't cut idle SSE connections.</li>
 * </ul>
 */
@Service
public class RealtimeAdminStreamService {

    private static final Logger log = LoggerFactory.getLogger(RealtimeAdminStreamService.class);

    /** Coalescing window. The doc target is ≤1–2s, this leaves plenty of margin. */
    public static final long BROADCAST_DEBOUNCE_MS = 250L;
    /** SSE keep-alive cadence. Most reverse-proxy idle timeouts are ≥30s; 15s is comfortable. */
    public static final long HEARTBEAT_INTERVAL_MS = 15_000L;

    private final Set<SseEmitter> emitters = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean broadcastPending = new AtomicBoolean(false);
    private volatile long lastBroadcastAtMs = 0L;
    private final AnalyticsService analyticsService;

    public RealtimeAdminStreamService(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(ex -> emitters.remove(emitter));
        return emitter;
    }

    public int activeClientCount() {
        return emitters.size();
    }

    /** Coalesced fan-out: marks a broadcast as pending; the scheduler flushes it within {@link #BROADCAST_DEBOUNCE_MS} ms. */
    public void scheduleBroadcast() {
        broadcastPending.set(true);
    }

    /** Direct fan-out used by the snapshot fallback path on initial subscribe. */
    public void broadcastSnapshot(Object snapshot) {
        if (snapshot == null) return;
        emitters.removeIf(emitter -> !sendSnapshot(emitter, snapshot));
        lastBroadcastAtMs = System.currentTimeMillis();
    }

    @Scheduled(fixedRate = BROADCAST_DEBOUNCE_MS)
    void flushPendingBroadcast() {
        if (emitters.isEmpty()) {
            broadcastPending.set(false);
            return;
        }
        if (!broadcastPending.compareAndSet(true, false)) {
            return;
        }
        Object snapshot;
        try {
            snapshot = analyticsService.getCurrentMetrics();
        } catch (RuntimeException e) {
            log.warn("Failed to assemble metrics snapshot", e);
            return;
        }
        emitters.removeIf(emitter -> !sendSnapshot(emitter, snapshot));
        lastBroadcastAtMs = System.currentTimeMillis();
    }

    @Scheduled(fixedRate = HEARTBEAT_INTERVAL_MS)
    void sendHeartbeat() {
        if (emitters.isEmpty()) return;
        emitters.removeIf(emitter -> !sendKeepAlive(emitter));
    }

    private boolean sendSnapshot(SseEmitter emitter, Object snapshot) {
        try {
            emitter.send(SseEmitter.event().name("metrics").data(snapshot));
            return true;
        } catch (IOException e) {
            try {
                emitter.complete();
            } catch (Exception ignore) {
                // emitter already torn down
            }
            return false;
        }
    }

    private boolean sendKeepAlive(SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event().comment("keepalive"));
            return true;
        } catch (IOException e) {
            try {
                emitter.complete();
            } catch (Exception ignore) {
                // no-op
            }
            return false;
        }
    }

    long getLastBroadcastAtMs() {
        return lastBroadcastAtMs;
    }
}
