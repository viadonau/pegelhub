package at.pegelhub.connector.api;

import at.pegelhub.connector.application.ConnectorService;
import at.pegelhub.connector.domain.ConnectorStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static at.pegelhub.testsupport.ExampleData.CONNECTOR;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HttpAdminConnectorController.class)
class HttpAdminConnectorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConnectorService connectorService;

    @Test
    void registerStoresKeycloakClientIdAndStatus() throws Exception {
        String keycloakClientId = "local-connector-example";
        when(connectorService.register(eq(keycloakClientId), eq(ConnectorStatus.ACTIVE), any()))
                .thenReturn(CONNECTOR);

        mockMvc.perform(post("/api/v1/admin/connectors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(keycloakClientId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(CONNECTOR.id().value().toString()))
                .andExpect(jsonPath("$.connectorNumber").value(CONNECTOR.connectorNumber()));

        verify(connectorService).register(eq(keycloakClientId), eq(ConnectorStatus.ACTIVE), any());
    }

    @Test
    void registerRequiresKeycloakClientId() throws Exception {
        mockMvc.perform(post("/api/v1/admin/connectors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("")))
                .andExpect(status().isBadRequest());
    }

    private static String registerJson(String keycloakClientId) {
        return """
                {
                  "keycloakClientId": "%s",
                  "connector": {
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
                }
                """.formatted(keycloakClientId);
    }
}
