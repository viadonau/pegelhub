package at.pegelhub.supplier.api;

import at.pegelhub.auth.application.AuthTokenIdHolder;
import at.pegelhub.auth.application.AuthorizationService;
import at.pegelhub.shared.error.NotFoundException;
import at.pegelhub.shared.error.UnauthorizedException;
import at.pegelhub.supplier.application.SupplierService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static at.pegelhub.testsupport.ExampleData.CONNECTOR;
import static at.pegelhub.testsupport.ExampleData.SUPPLIER;
import static at.pegelhub.testsupport.ExampleDtos.CREATE_SUPPLIER_DTO;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@WebMvcTest(HttpSupplierController.class)
class HttpSupplierControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    @MockitoBean
    private AuthorizationService authorizationService;

    @MockitoBean
    private SupplierService supplierService;

    @AfterEach
    void clearAuthHolder() {
        AuthTokenIdHolder.clear();
    }

    @Test
    void saveSupplierUsesAuthContextAndClearsHolderAfterSuccess() throws Exception {
        UUID tokenId = UUID.randomUUID();
        when(authorizationService.authorize("valid")).thenReturn(tokenId);
        doAnswer(invocation -> {
            assertEquals(tokenId, AuthTokenIdHolder.get());
            return SUPPLIER;
        }).when(supplierService).saveSupplier(any());

        mockMvc.perform(post("/api/v1/supplier")
                        .param("apiKey", "valid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(CREATE_SUPPLIER_DTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(requireNonNull(SUPPLIER.getId()).toString()));

        assertNull(AuthTokenIdHolder.get());
    }

    @Test
    void updateSupplierClearsHolderAfterException() throws Exception {
        UUID tokenId = UUID.randomUUID();
        when(authorizationService.authorize("valid")).thenReturn(tokenId);
        doAnswer(invocation -> {
            assertEquals(tokenId, AuthTokenIdHolder.get());
            throw new RuntimeException("update failed");
        }).when(supplierService).updateSupplier(any());

        mockMvc.perform(put("/api/v1/supplier")
                        .param("apiKey", "valid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(CREATE_SUPPLIER_DTO)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("update failed"));

        assertNull(AuthTokenIdHolder.get());
    }

    @Test
    void getSupplierByIdReturnsSupplierJson() throws Exception {
        when(supplierService.getSupplierById(SUPPLIER.getId())).thenReturn(SUPPLIER);

        mockMvc.perform(get("/api/v1/supplier/{uuid}", SUPPLIER.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(requireNonNull(SUPPLIER.getId()).toString()))
                .andExpect(jsonPath("$.stationNumber").value(SUPPLIER.getStationNumber()));
    }

    @Test
    void getAllSuppliersReturnsArray() throws Exception {
        when(supplierService.getAllSuppliers()).thenReturn(List.of(SUPPLIER));

        mockMvc.perform(get("/api/v1/supplier"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(requireNonNull(SUPPLIER.getId()).toString()));
    }

    @Test
    void deleteSupplierDelegatesToService() throws Exception {
        mockMvc.perform(delete("/api/v1/supplier/{uuid}", SUPPLIER.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        verify(supplierService).deleteSupplier(SUPPLIER.getId());
    }

    @Test
    void getConnectorIdReturnsUuid() throws Exception {
        when(supplierService.getConnectorID(SUPPLIER.getId())).thenReturn(CONNECTOR.getId());

        mockMvc.perform(get("/api/v1/supplier/connectorID/{uuid}", SUPPLIER.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string("\"" + CONNECTOR.getId() + "\""));
    }

    @Test
    void updateSupplierReturnsSupplierJson() throws Exception {
        when(authorizationService.authorize("valid")).thenReturn(UUID.randomUUID());
        when(supplierService.updateSupplier(any())).thenReturn(SUPPLIER);

        mockMvc.perform(put("/api/v1/supplier")
                        .param("apiKey", "valid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(CREATE_SUPPLIER_DTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(requireNonNull(SUPPLIER.getId()).toString()));
    }

    @Test
    void protectedSupplierEndpointsRejectUnauthorizedRequests() throws Exception {
        doThrow(new UnauthorizedException("unauthorized")).when(authorizationService).authorize(any());

        mockMvc.perform(post("/api/v1/supplier")
                        .param("apiKey", "invalid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(CREATE_SUPPLIER_DTO)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Unauthorized"));

        mockMvc.perform(put("/api/v1/supplier")
                        .param("apiKey", "invalid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(CREATE_SUPPLIER_DTO)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Unauthorized"));
    }

    @Test
    void getSupplierByIdMapsNotFoundTo404() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new NotFoundException("supplier missing")).when(supplierService).getSupplierById(id);

        mockMvc.perform(get("/api/v1/supplier/{uuid}", id))
                .andExpect(status().isNotFound())
                .andExpect(content().string("supplier missing"));
    }
}
