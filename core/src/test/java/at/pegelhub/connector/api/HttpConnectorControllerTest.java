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
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
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
    void createNormalizesOmittedLegacyConnectorFields() throws Exception {
        when(connectorService.create(any())).thenReturn(CONNECTOR);

        mockMvc.perform(post("/api/v1/connectors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "connectorNumber": "connectorNR"
                                }
                                """))
                .andExpect(status().isOk());

        var command = forClass(at.pegelhub.connector.application.CreateConnectorCommand.class);
        verify(connectorService).create(command.capture());
        assertThat(command.getValue().typeDescription()).isEmpty();
        assertThat(command.getValue().softwareVersion()).isEmpty();
        assertThat(command.getValue().worksFromDataVersion()).isEmpty();
        assertThat(command.getValue().dataDefinition()).isEmpty();
        assertThat(command.getValue().notes()).isEmpty();
        assertThat(command.getValue().manufacturer()).isNotNull();
        assertThat(command.getValue().softwareManufacturer()).isNotNull();
        assertThat(command.getValue().technicallyResponsible()).isNotNull();
        assertThat(command.getValue().operationCompany()).isNotNull();
    }

    @Test
    void createWithBlankConnectorNumberReturnsValidationProblem() throws Exception {
        mockMvc.perform(post("/api/v1/connectors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "connectorNumber": " "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("connectorNumber"));
    }

    @Test
    void createWithInvalidNestedContactReturnsValidationProblem() throws Exception {
        mockMvc.perform(post("/api/v1/connectors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "connectorNumber": "connectorNR",
                                  "manufacturer": {
                                    "organization": "%s"
                                  }
                                }
                                """.formatted("x".repeat(151))))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("manufacturer.organization"));
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
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("connector missing"));
    }

    @Test
    void getByIdWithInvalidUuidReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/connectors/{uuid}", "not-a-uuid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteMapsNotFoundTo404() throws Exception {
        UUID id = UUID.randomUUID();
        ConnectorId connectorId = new ConnectorId(id);
        doThrow(new NotFoundException("connector missing")).when(connectorService).delete(connectorId);

        mockMvc.perform(delete("/api/v1/connectors/{uuid}", id))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("connector missing"));
    }
}
