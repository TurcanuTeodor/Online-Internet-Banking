package ro.app.client.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ro.app.client.audit.AuditService;
import ro.app.client.dto.ContactInfoDTO;
import ro.app.client.security.JwtPrincipal;
import ro.app.client.security.OwnershipChecker;
import ro.app.client.service.ClientService;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class ClientControllerTest {

    @Mock
    private ClientService clientService;
    @Mock
    private OwnershipChecker ownershipChecker;
    @Mock
    private AuditService auditService;
    @InjectMocks
    private ClientController clientController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        clientController = new ClientController(clientService, ownershipChecker, auditService);
    }

    @Test
    void updateContact_OwnershipCheckCalled() {
        ContactInfoDTO dto = mock(ContactInfoDTO.class);
        JwtPrincipal principal = mock(JwtPrincipal.class);
        when(clientService.updateClientContactInfo(123L, dto)).thenReturn(dto);

        ResponseEntity<ContactInfoDTO> response = clientController.updateContact(123L, dto, principal);

        verify(ownershipChecker).checkOwnership(principal, 123L);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // Adaugă teste similare pentru celelalte endpointuri cu ownership check
}
