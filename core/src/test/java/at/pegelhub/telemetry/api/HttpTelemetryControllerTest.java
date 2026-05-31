package at.pegelhub.telemetry.api;

import at.pegelhub.telemetry.application.TelemetryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static at.pegelhub.testsupport.ExampleData.ID;
import static at.pegelhub.testsupport.ExampleData.TELEMETRIES;
import static at.pegelhub.testsupport.ExampleData.TELEMETRY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HttpTelemetryController.class)
class HttpTelemetryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TelemetryService telemetryService;

    @Test
    void writeTelemetryDataReturnsTelemetryJson() throws Exception {
        when(telemetryService.saveTelemetry(any())).thenReturn(TELEMETRY);

        mockMvc.perform(post("/api/v1/telemetry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "measurement": "a93fdc3d-b71f-44ce-a826-fe1dc1f1f357",
                                  "stationIPAddressIntern": "172.0.0.0",
                                  "stationIPAddressExtern": "172.0.0.0",
                                  "timestamp": "2010-10-12T08:50:00Z",
                                  "cycleTime": 1,
                                  "temperatureWater": -2.0,
                                  "temperatureAir": -2.0,
                                  "performanceVoltageBattery": 2.0,
                                  "performanceVoltageSupply": 2.0,
                                  "performanceElectricityBattery": 2.0,
                                  "performanceElectricitySupply": 2.0,
                                  "fieldStrengthTransmission": 2.0
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.measurement").value(TELEMETRY.measurement()))
                .andExpect(jsonPath("$.cycleTime").value(TELEMETRY.cycleTime()));
    }

    @Test
    void findTelemetryInRangeReturnsTelemetryArrayJson() throws Exception {
        when(telemetryService.getByRange("72h")).thenReturn(TELEMETRIES);

        mockMvc.perform(get("/api/v1/telemetry/{range}", "72h"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].measurement").value(TELEMETRY.measurement()));
    }

    @Test
    void findTelemetryByIdReturnsLatestTelemetryJson() throws Exception {
        when(telemetryService.getLastData(ID)).thenReturn(TELEMETRY);

        mockMvc.perform(get("/api/v1/telemetry/last/{uuid}", ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.measurement").value(TELEMETRY.measurement()));
    }

    @Test
    void telemetryRuntimeExceptionIsMappedTo500() throws Exception {
        doThrow(new RuntimeException("influx down")).when(telemetryService).getByRange("72h");

        mockMvc.perform(get("/api/v1/telemetry/{range}", "72h"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("influx down"));

        verify(telemetryService).getByRange("72h");
    }
}
