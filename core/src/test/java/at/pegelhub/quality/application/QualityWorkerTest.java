package at.pegelhub.quality.application;

import at.pegelhub.measurement.application.InternalMeasurements;
import at.pegelhub.notifications.application.Notifications;
import at.pegelhub.quality.domain.ProfileConfig;
import at.pegelhub.quality.domain.QualityRun;
import at.pegelhub.quality.persistence.QualityRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class QualityWorkerTest {
    private final QualityRepository repository = mock(QualityRepository.class);
    private final Quality quality = mock(Quality.class);
    private final InternalMeasurements measurements = mock(InternalMeasurements.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-13T10:00:00Z"), ZoneOffset.UTC);
    private final QualityWorker worker = new QualityWorker(new QualityProperties(true, 100000, 120, 90),
            repository, quality, measurements, mock(Notifications.class), clock);

    @Test
    void recoversAfterAnUncertainClaimWithoutRestarting() {
        when(repository.claim(any())).thenThrow(new IllegalStateException("database unavailable")).thenReturn(null);

        worker.tick();
        worker.tick();

        verify(repository, times(2)).recover(clock.instant());
    }

    @Test
    void recoversAfterReleaseFails() {
        var run = run();
        when(repository.claim(any())).thenReturn(run).thenReturn(null);
        when(measurements.readWindow(any(), any(), anyInt(), any())).thenReturn(Map.of());
        doThrow(new IllegalStateException("database unavailable")).when(repository).release(any(), any());

        worker.tick();
        worker.tick();

        verify(repository, times(2)).recover(clock.instant());
    }

    @Test
    void uncertainResultCommitDoesNotOverwriteFindingsWithAnError() {
        var run = run();
        when(repository.claim(any())).thenReturn(run).thenReturn(null);
        when(measurements.readWindow(any(), any(), anyInt(), any())).thenReturn(Map.of());
        doThrow(new IllegalStateException("uncertain commit")).when(quality).record(any(), any());

        worker.tick();
        worker.tick();

        verify(quality, never()).fail(any(), any(), any());
        verify(repository).release(run, clock.instant());
        verify(repository, times(2)).recover(clock.instant());
    }

    @Test
    void failedErrorRecordingTriggersRecovery() {
        when(repository.claim(any())).thenReturn(run()).thenReturn(null);
        when(measurements.readWindow(any(), any(), anyInt(), any())).thenThrow(new IllegalStateException("read failed"));
        doThrow(new IllegalStateException("database unavailable")).when(quality).fail(any(), any(), any());

        worker.tick();
        worker.tick();

        verify(repository, times(2)).recover(clock.instant());
    }

    private QualityRun run() {
        return new QualityRun(UUID.randomUUID(), UUID.randomUUID(),
                new ProfileConfig("Test", true, List.of(UUID.randomUUID()), null, 1200, 60, List.of(), null),
                QualityRun.State.RUNNING, clock.instant(), null, 0, QualityRun.OutputState.NOT_CONFIGURED, null);
    }
}
