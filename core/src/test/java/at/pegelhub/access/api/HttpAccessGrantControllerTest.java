package at.pegelhub.access.api;

import at.pegelhub.access.application.AccessGrantService;
import at.pegelhub.access.application.CreateAccessGrantCommand;
import at.pegelhub.access.domain.AccessGrant;
import at.pegelhub.access.domain.AccessGrantId;
import at.pegelhub.access.domain.AccessPermission;
import at.pegelhub.access.domain.AccessResourceRef;
import at.pegelhub.connector.domain.ConnectorId;
import at.pegelhub.station.domain.StationId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HttpAccessGrantController.class)
class HttpAccessGrantControllerTest {

    private static final UUID GRANT_ID = UUID.fromString("770f06e5-6293-4ca4-86e2-91f95db99d37");
    private static final UUID CONNECTOR_ID = UUID.fromString("512c03d7-8db1-4725-a02b-6fb01d34fe86");
    private static final UUID STATION_ID = UUID.fromString("f4cf3697-47c5-45d1-9f7e-9d2bdb84dc8d");
    private static final AccessGrant GRANT = new AccessGrant(
            new AccessGrantId(GRANT_ID),
            new ConnectorId(CONNECTOR_ID),
            AccessResourceRef.station(new StationId(STATION_ID)),
            AccessPermission.READ);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccessGrantService accessGrants;

    @Test
    void createsAccessGrant() throws Exception {
        when(accessGrants.create(any())).thenReturn(GRANT);

        mockMvc.perform(post("/api/v1/access-grants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "connectorId": "%s",
                                  "resourceType": "STATION",
                                  "resourceId": "%s",
                                  "permission": "READ"
                                }
                                """.formatted(CONNECTOR_ID, STATION_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(GRANT_ID.toString()))
                .andExpect(jsonPath("$.connectorId").value(CONNECTOR_ID.toString()))
                .andExpect(jsonPath("$.resourceType").value("STATION"))
                .andExpect(jsonPath("$.resourceId").value(STATION_ID.toString()))
                .andExpect(jsonPath("$.permission").value("READ"));

        verify(accessGrants).create(eq(new CreateAccessGrantCommand(
                new ConnectorId(CONNECTOR_ID),
                AccessResourceRef.station(new StationId(STATION_ID)),
                AccessPermission.READ)));
    }

    @Test
    void rejectsCreateWithoutConnectorId() throws Exception {
        mockMvc.perform(post("/api/v1/access-grants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "resourceType": "STATION",
                                  "resourceId": "%s",
                                  "permission": "READ"
                                }
                                """.formatted(STATION_ID)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getsAccessGrant() throws Exception {
        when(accessGrants.get(new AccessGrantId(GRANT_ID))).thenReturn(GRANT);

        mockMvc.perform(get("/api/v1/access-grants/{id}", GRANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(GRANT_ID.toString()))
                .andExpect(jsonPath("$.permission").value("READ"));
    }

    @Test
    void listsAccessGrants() throws Exception {
        when(accessGrants.list()).thenReturn(List.of(GRANT));

        mockMvc.perform(get("/api/v1/access-grants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(GRANT_ID.toString()));
    }

    @Test
    void listsAccessGrantsForConnector() throws Exception {
        when(accessGrants.listForConnector(new ConnectorId(CONNECTOR_ID))).thenReturn(List.of(GRANT));

        mockMvc.perform(get("/api/v1/access-grants")
                        .param("connectorId", CONNECTOR_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].connectorId").value(CONNECTOR_ID.toString()));
    }
}
