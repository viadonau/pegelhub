package at.pegelhub.connector.api;

import at.pegelhub.connector.application.ConnectorService;
import at.pegelhub.shared.error.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static at.pegelhub.testsupport.ExampleData.CONNECTOR;
import static java.util.Objects.requireNonNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HttpConnectorController.class)
class HttpConnectorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConnectorService connectorService;

    @Test
    void saveConnectorReturnsDtoJson() throws Exception {
        when(connectorService.createConnector(any())).thenReturn(CONNECTOR);

        mockMvc.perform(post("/api/v1/connector")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "connectorNumber": "connectorNR",
                                  "manufacturer": {
                                    "organization": "org1"
                                  },
                                  "typeDescription": "description",
                                  "softwareVersion": "1.0.0",
                                  "worksFromDataVersion": "1.0.0",
                                  "dataDefinition": "definition",
                                  "softwareManufacturer": {
                                    "organization": "org1"
                                  },
                                  "technicallyResponsible": {
                                    "organization": "org1"
                                  },
                                  "operationCompany": {
                                    "organization": "org1"
                                  },
                                  "notes": "notes",
                                  "apiToken": "11111111-1111-1111-1111-111111111111"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(requireNonNull(CONNECTOR.getId()).toString()))
                .andExpect(jsonPath("$.connectorNumber").value(CONNECTOR.getConnectorNumber()))
                .andExpect(jsonPath("$.typeDescription").value(CONNECTOR.getTypeDescription()));
    }

    @Test
    void getConnectorByIdReturnsDtoJson() throws Exception {
        when(connectorService.getConnectorById(CONNECTOR.getId())).thenReturn(CONNECTOR);

        mockMvc.perform(get("/api/v1/connector/{uuid}", CONNECTOR.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(requireNonNull(CONNECTOR.getId()).toString()))
                .andExpect(jsonPath("$.connectorNumber").value(CONNECTOR.getConnectorNumber()));
    }

    @Test
    void getAllConnectorsReturnsArray() throws Exception {
        when(connectorService.getAllConnectors()).thenReturn(List.of(CONNECTOR));

        mockMvc.perform(get("/api/v1/connector"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(requireNonNull(CONNECTOR.getId()).toString()));
    }

    @Test
    void deleteConnectorDelegatesToService() throws Exception {
        mockMvc.perform(delete("/api/v1/connector/{uuid}", CONNECTOR.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        verify(connectorService).deleteConnector(CONNECTOR.getId());
    }

    @Test
    void getConnectorByIdMapsNotFoundTo404() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new NotFoundException("connector missing")).when(connectorService).getConnectorById(id);

        mockMvc.perform(get("/api/v1/connector/{uuid}", id))
                .andExpect(status().isNotFound())
                .andExpect(content().string("connector missing"));
    }

    @Test
    void getConnectorByIdWithInvalidUuidReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/connector/{uuid}", "not-a-uuid"))
                .andExpect(status().isBadRequest());
    }
}
