package at.pegelhub.connector.api;

import at.pegelhub.connector.application.ConnectorService;
import at.pegelhub.connector.domain.ConnectorId;
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
    void createReturnsDtoJson() throws Exception {
        when(connectorService.create(any())).thenReturn(CONNECTOR);

        mockMvc.perform(post("/api/v1/connectors")
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
                                  "notes": "notes"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(CONNECTOR.id().value().toString()))
                .andExpect(jsonPath("$.connectorNumber").value(CONNECTOR.connectorNumber()))
                .andExpect(jsonPath("$.typeDescription").value(CONNECTOR.typeDescription()));
    }

    @Test
    void getByIdReturnsDtoJson() throws Exception {
        ConnectorId id = CONNECTOR.id();
        when(connectorService.get(id)).thenReturn(CONNECTOR);

        mockMvc.perform(get("/api/v1/connectors/{uuid}", id.value()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.value().toString()))
                .andExpect(jsonPath("$.connectorNumber").value(CONNECTOR.connectorNumber()));
    }

    @Test
    void listReturnsArray() throws Exception {
        when(connectorService.list()).thenReturn(List.of(CONNECTOR));

        mockMvc.perform(get("/api/v1/connectors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(CONNECTOR.id().value().toString()));
    }

    @Test
    void deleteDelegatesToService() throws Exception {
        ConnectorId id = CONNECTOR.id();
        mockMvc.perform(delete("/api/v1/connectors/{uuid}", id.value()))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        verify(connectorService).delete(id);
    }

    @Test
    void getByIdMapsNotFoundTo404() throws Exception {
        UUID id = UUID.randomUUID();
        ConnectorId cid = new ConnectorId(id);
        doThrow(new NotFoundException("connector missing")).when(connectorService).get(cid);

        mockMvc.perform(get("/api/v1/connectors/{uuid}", id))
                .andExpect(status().isNotFound())
                .andExpect(content().string("connector missing"));
    }

    @Test
    void getByIdWithInvalidUuidReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/connectors/{uuid}", "not-a-uuid"))
                .andExpect(status().isBadRequest());
    }
}
