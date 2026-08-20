package at.pegelhub.monitoring.api;

import at.pegelhub.monitoring.application.MonitoringQueryService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

final class MonitoringControllerTest {

    @Test
    void rejectsLatestWindowOverOneYear() {
        var controller = new MonitoringController(mock(MonitoringQueryService.class));

        assertThrows(IllegalArgumentException.class, () -> controller.list("366d"));
    }
}
