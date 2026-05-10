package at.pegelhub.contact.api;

import at.pegelhub.contact.application.ContactService;
import at.pegelhub.shared.error.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static at.pegelhub.testsupport.ExampleData.CONTACT;
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

@WebMvcTest(HttpContactController.class)
class HttpContactControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ContactService contactService;

    @Test
    void saveContactReturnsDtoJson() throws Exception {
        when(contactService.createContact(any())).thenReturn(CONTACT);

        mockMvc.perform(post("/api/v1/contact")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "organization": "org1",
                                  "contactPerson": "Hans Maier",
                                  "contactStreet": "Blumenweg 22",
                                  "contactPlz": "1549",
                                  "location": "Wien",
                                  "contactCountry": "AT",
                                  "emergencyNumber": "123456789",
                                  "emergencyNumberTwo": "123456780",
                                  "emergencyMail": "emergency@mail.com",
                                  "serviceNumber": "123456789",
                                  "serviceNumberTwo": "123456780",
                                  "serviceMail": "service@mail.com",
                                  "administrationPhoneNumber": "123456789",
                                  "administrationPhoneNumberTwo": "123456780",
                                  "administrationMail": "service@mail.com",
                                  "contactNodes": "notes"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(CONTACT.getId().toString()))
                .andExpect(jsonPath("$.organization").value(CONTACT.getOrganization()));
    }

    @Test
    void getContactByIdReturnsDtoJson() throws Exception {
        when(contactService.getContactById(CONTACT.getId())).thenReturn(CONTACT);

        mockMvc.perform(get("/api/v1/contact/{uuid}", CONTACT.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(CONTACT.getId().toString()))
                .andExpect(jsonPath("$.organization").value(CONTACT.getOrganization()));
    }

    @Test
    void getAllContactsReturnsArray() throws Exception {
        when(contactService.getAllContacts()).thenReturn(List.of(CONTACT));

        mockMvc.perform(get("/api/v1/contact"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(CONTACT.getId().toString()));
    }

    @Test
    void deleteContactDelegatesToService() throws Exception {
        mockMvc.perform(delete("/api/v1/contact/{uuid}", CONTACT.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        verify(contactService).deleteContact(CONTACT.getId());
    }

    @Test
    void getContactByIdMapsNotFoundTo404() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new NotFoundException("contact missing")).when(contactService).getContactById(id);

        mockMvc.perform(get("/api/v1/contact/{uuid}", id))
                .andExpect(status().isNotFound())
                .andExpect(content().string("contact missing"));
    }
}
