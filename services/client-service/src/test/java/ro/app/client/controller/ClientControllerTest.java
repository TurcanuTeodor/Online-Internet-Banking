package ro.app.client.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ro.app.client.audit.AuditService;
import ro.app.client.dto.ContactInfoDTO;
import ro.app.client.security.JwtPrincipal;
import ro.app.client.security.OwnershipChecker;
import ro.app.client.service.ClientContactService;
import ro.app.client.service.ClientProfileService;
import ro.app.client.service.ClientViewProjectionService;

class ClientControllerTest {

    @Mock
    private ClientProfileService clientProfileService;
    @Mock
    private ClientContactService clientContactService;
    @Mock
    private ClientViewProjectionService clientViewProjectionService;
    @Mock
    private OwnershipChecker ownershipChecker;
    @Mock
    private AuditService auditService;
    @InjectMocks
    private ClientController clientController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        clientController = new ClientController(
                clientProfileService,
                clientContactService,
                clientViewProjectionService,
                ownershipChecker,
                auditService);
    }

    @Test
    void updateContact_OwnershipCheckCalled() {
        ContactInfoDTO dto = mock(ContactInfoDTO.class);
        JwtPrincipal principal = new JwtPrincipal("testUser", 123L, "USER", "testKey");
        when(clientContactService.updateClientContactInfo(eq(123L), any(ContactInfoDTO.class), anyString(), isNull()))
                .thenReturn(dto);

        ResponseEntity<ContactInfoDTO> response = clientController.updateContact(123L, dto, null, principal);

        verify(ownershipChecker).checkOwnership(principal, 123L);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // Adaugă teste similare pentru celelalte endpointuri cu ownership check
}
